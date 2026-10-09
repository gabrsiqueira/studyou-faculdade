import logging
import time

from google import genai
from google.genai import types
from google.genai.errors import APIError
from pydantic import BaseModel, Field

from app.application.services.crossword_layout import normalize_word
from app.domain.entities.games import Flashcard, QuizQuestion, RawGameContent
from app.domain.exceptions import AIServiceError
from app.domain.interfaces.game_content_generator import GameContentGenerator
from app.infrastructure.ai.errors import friendly_reason
from app.infrastructure.ai.thinking import thinking_config_for

logger = logging.getLogger(__name__)

_GENERATION_PROMPT_TEMPLATE = """\
Com base no material de estudo analisado abaixo, gere o conteudo bruto para 4 mini-jogos \
educativos em portugues do Brasil. Responda SOMENTE no formato JSON pedido.

Material analisado:
---
{analysis_text}
---

Regras gerais (MUITO IMPORTANTE):
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
    (JSON validado por schema) para evitar parsing fragil de texto livre."""

    def __init__(self, api_key: str, model: str, thinking_level: str = "") -> None:
        self._client = genai.Client(api_key=api_key)
        self._model = model
        self._thinking_config = thinking_config_for(thinking_level)

    async def generate(self, filename: str, analysis_text: str) -> RawGameContent:
        prompt = _GENERATION_PROMPT_TEMPLATE.format(analysis_text=analysis_text)
        start = time.perf_counter()
        try:
            response = await self._client.aio.models.generate_content(
                model=self._model,
                contents=prompt,
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    response_schema=_GameContentSchema,
                    thinking_config=self._thinking_config,
                ),
            )
        except APIError as error:
            raise AIServiceError(filename, friendly_reason(error)) from error
        response_time_ms = round((time.perf_counter() - start) * 1000)

        parsed = response.parsed
        if not isinstance(parsed, _GameContentSchema):
            logger.debug("Resposta invalida do Gemini para os jogos de '%s': %r", filename, response.text)
            raise AIServiceError(filename, "A IA nao retornou um JSON valido para os jogos.")

        usage = response.usage_metadata
        total_tokens = (usage.total_token_count or 0) if usage else 0
        return self._to_raw_game_content(parsed, total_tokens, response_time_ms)

    def _to_raw_game_content(
        self, schema: _GameContentSchema, total_tokens: int, response_time_ms: int
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
            total_tokens=total_tokens,
            response_time_ms=response_time_ms,
        )
