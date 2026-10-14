import io
import logging
import time

from docx import Document
from google import genai
from google.genai import types
from google.genai.errors import APIError, ClientError
from pptx import Presentation
from pydantic import BaseModel, Field

from app.application.services.crossword_layout import normalize_word
from app.domain.entities.games import Flashcard, QuizQuestion, RawGameContent
from app.domain.exceptions import AIServiceError, ContentExceedsContextLimitError
from app.domain.interfaces.game_content_generator import GameContentGenerator
from app.infrastructure.ai.errors import friendly_reason
from app.infrastructure.ai.thinking import thinking_config_for

logger = logging.getLogger(__name__)

_DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
_PPTX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation"

# Substrings observadas na mensagem de erro do Gemini quando a requisicao e rejeitada
# por exceder o limite de contexto do modelo (ao inves de outro tipo de erro 4xx).
_CONTEXT_LIMIT_ERROR_HINTS = ("token", "context length", "context window", "too long", "too large")

GENERATION_PROMPT = """\
Com base no material de estudo enviado em anexo, gere o conteudo bruto para 4 mini-jogos \
educativos em portugues do Brasil. Responda SOMENTE no formato JSON pedido.

Regras gerais (MUITO IMPORTANTE):
- Use SOMENTE os termos, conceitos e informacoes presentes no proprio material enviado. \
Nao acrescente fatos, exemplos ou palavras que nao estejam no material.
- Nenhum conteudo pode se repetir, nem dentro de um jogo nem ENTRE jogos diferentes. \
Cada pergunta, cartao e palavra deve cobrir um fato ou termo que nao aparece em nenhum \
outro item, de nenhum dos jogos (nao reformule a mesma pergunta com outras palavras).
- Antes de responder, divida os fatos do material entre os jogos: os fatos usados no quiz \
NAO podem ser usados nos flashcards, e as palavras usadas em um jogo de palavras NAO podem \
ser usadas em outro.

Regras por campo:
- quiz: exatamente 5 perguntas de multipla escolha, cada uma sobre um fato diferente, com \
exatamente 4 opcoes e o indice (0 a 3) da opcao correta.
- flashcards: de 6 a 8 cartoes de pergunta/resposta curtos, sobre fatos que NAO foram \
usados em nenhuma pergunta do quiz e distintos entre si.
- guess_word_answer: UMA unica palavra-chave central do conteudo (sem espacos, sem \
acentos, 4 a 10 letras, maiusculas) para um jogo de adivinhacao estilo Termo. Ela NAO \
pode aparecer em crossword.
- crossword: de 6 a 10 itens para preencher palavras cruzadas, cada um com:
  - word: uma palavra-chave do conteudo (uma palavra so, sem espacos, sem acentos, 3 a 10 \
letras), todas diferentes entre si e da palavra do Termo;
  - clue: uma dica curta (ate 12 palavras) que ajude o jogador a descobrir essa palavra a \
partir do conteudo estudado. A dica NUNCA pode conter a propria palavra nem derivados dela.
"""


class _QuizQuestionSchema(BaseModel):
    prompt: str
    options: list[str] = Field(min_length=4, max_length=4)
    correct_option_index: int


class _FlashcardSchema(BaseModel):
    question: str
    answer: str


class _CrosswordWordSchema(BaseModel):
    word: str
    clue: str


class _GameContentSchema(BaseModel):
    quiz: list[_QuizQuestionSchema]
    flashcards: list[_FlashcardSchema]
    guess_word_answer: str
    crossword: list[_CrosswordWordSchema]


def _extract_docx_text(content: bytes) -> str:
    document = Document(io.BytesIO(content))
    return "\n".join(paragraph.text for paragraph in document.paragraphs)


def _extract_pptx_text(content: bytes) -> str:
    presentation = Presentation(io.BytesIO(content))
    lines = []
    for slide in presentation.slides:
        for shape in slide.shapes:
            if shape.has_text_frame:
                lines.append(shape.text_frame.text)
    return "\n".join(lines)


def _as_gemini_content(content: bytes, mime_type: str) -> tuple[bytes, str]:
    """O Gemini entende PDF, imagens e texto puro diretamente, mas nao os arquivos do
    Office (XML compactado), entao DOCX/PPTX sao convertidos para texto puro antes do envio."""
    if mime_type == _DOCX_MIME_TYPE:
        return _extract_docx_text(content).encode("utf-8"), "text/plain"
    if mime_type == _PPTX_MIME_TYPE:
        return _extract_pptx_text(content).encode("utf-8"), "text/plain"
    return content, mime_type


def _is_context_limit_error(error: APIError) -> bool:
    if not isinstance(error, ClientError):
        return False
    message = str(error).lower()
    return any(hint in message for hint in _CONTEXT_LIMIT_ERROR_HINTS)


def _clues_by_word(items: list[_CrosswordWordSchema]) -> dict[str, str]:
    """Dica de cada palavra; descarta dicas vazias ou que entregam a propria resposta."""
    clues: dict[str, str] = {}
    for item in items:
        word, clue = normalize_word(item.word), " ".join(item.clue.split())
        gives_answer_away = any(normalize_word(token).startswith(word) for token in clue.split())
        if word and clue and not gives_answer_away:
            clues.setdefault(word, clue)
    return clues


class GeminiGameContentGenerator(GameContentGenerator):
    """Implementacao de GameContentGenerator usando o Gemini, com saida estruturada
    (JSON validado por schema) para evitar parsing fragil de texto livre.

    Faz uma unica chamada (generate_content) por arquivo: o material enviado vai junto
    com o prompt de geracao e o modelo devolve diretamente o JSON das atividades. O limite
    de contexto e checado reativamente a partir do erro que o proprio Gemini retorna quando
    o conteudo e grande demais, em vez de uma chamada extra (count_tokens) so para isso.
    """

    def __init__(self, api_key: str, model: str, thinking_level: str = "") -> None:
        self._client = genai.Client(api_key=api_key)
        self._model = model
        self._thinking_config = thinking_config_for(thinking_level)

    async def generate(self, filename: str, content: bytes, mime_type: str) -> RawGameContent:
        gemini_content, gemini_mime_type = _as_gemini_content(content, mime_type)
        logger.debug(
            "Enviando '%s' ao Gemini (modelo=%s, mime=%s, %d bytes)",
            filename,
            self._model,
            gemini_mime_type,
            len(gemini_content),
        )
        start = time.perf_counter()
        try:
            response = await self._client.aio.models.generate_content(
                model=self._model,
                contents=[
                    types.Part.from_bytes(data=gemini_content, mime_type=gemini_mime_type),
                    GENERATION_PROMPT,
                ],
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    response_schema=_GameContentSchema,
                    thinking_config=self._thinking_config,
                ),
            )
        except APIError as error:
            if _is_context_limit_error(error):
                raise ContentExceedsContextLimitError(filename) from error
            raise AIServiceError(filename, friendly_reason(error)) from error
        response_time_ms = round((time.perf_counter() - start) * 1000)

        parsed = response.parsed
        if not isinstance(parsed, _GameContentSchema):
            logger.debug("Resposta invalida do Gemini para os jogos de '%s': %r", filename, response.text)
            raise AIServiceError(filename, "A IA nao retornou um JSON valido para os jogos.")

        usage = response.usage_metadata
        total_tokens = (usage.total_token_count or 0) if usage else 0
        return self._to_raw_game_content(parsed, response.text or "", total_tokens, response_time_ms)

    def _to_raw_game_content(
        self, schema: _GameContentSchema, raw_response: str, total_tokens: int, response_time_ms: int
    ) -> RawGameContent:
        """Converte e descarta itens malformados. A unicidade entre jogos fica em
        enforce_unique_content (use case)."""
        quiz = [
            QuizQuestion(
                prompt=q.prompt.strip(),
                options=[option.strip() for option in q.options],
                correct_option_index=min(max(q.correct_option_index, 0), len(q.options) - 1),
            )
            for q in schema.quiz
            if q.prompt.strip() and len(q.options) == 4
        ]
        flashcards = [
            Flashcard(question=card.question.strip(), answer=card.answer.strip())
            for card in schema.flashcards
            if card.question.strip() and card.answer.strip()
        ]

        return RawGameContent(
            quiz=quiz,
            flashcards=flashcards,
            guess_word_answer=normalize_word(schema.guess_word_answer),
            crossword_words=[word for item in schema.crossword if (word := normalize_word(item.word))],
            crossword_clues=_clues_by_word(schema.crossword),
            prompt_used=GENERATION_PROMPT,
            raw_response=raw_response,
            total_tokens=total_tokens,
            response_time_ms=response_time_ms,
        )
