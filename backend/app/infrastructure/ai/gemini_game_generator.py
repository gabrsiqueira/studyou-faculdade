import logging
import time
from collections import Counter

from google import genai
from google.genai import types
from google.genai.errors import APIError
from pydantic import BaseModel, Field

from app.application.services.crossword_layout import normalize_word
from app.domain.entities.games import Flashcard, QuizQuestion, RawGameContent
from app.domain.exceptions import AIServiceError
from app.domain.interfaces.game_content_generator import GameContentGenerator

logger = logging.getLogger(__name__)

_GENERATION_PROMPT_TEMPLATE = """\
Com base no material de estudo analisado abaixo, gere o conteudo bruto para 5 mini-jogos \
educativos em portugues do Brasil. Responda SOMENTE no formato JSON pedido.

Material analisado:
---
{analysis_text}
---

Regras gerais:
- Cada pergunta, cartao ou palavra deve ser DIFERENTE das demais e cobrir um aspecto \
distinto do conteudo (nao repita a mesma pergunta/cartao reformulado(a) com outras \
palavras, nem reuse o mesmo fato para itens diferentes).

Regras por campo:
- quiz: exatamente 5 perguntas de multipla escolha DISTINTAS sobre o conteudo, cada uma \
cobrindo um topico ou fato diferente, cada uma com exatamente 4 opcoes e o indice (0 a 3) \
da opcao correta.
- flashcards: de 6 a 8 cartoes de pergunta/resposta curtos e DISTINTOS entre si para revisao.
- guess_word_answer: UMA unica palavra-chave central do conteudo (sem espacos, sem \
acentos, 4 a 10 letras, maiusculas) para um jogo de adivinhacao estilo Termo.
- crossword_words: de 6 a 10 palavras-chave do conteudo (uma palavra cada, sem espacos, \
sem acentos, 3 a 10 letras) para preencher uma cruzadinha.
- word_forming_letters: um conjunto de 7 a 10 letras (maiusculas, sem acentos, pode \
repetir letras) a partir das quais seja possivel formar varias palavras relacionadas ao \
conteudo.
- word_forming_valid_words: de 5 a 10 palavras (sem acentos, 3+ letras) que podem ser \
formadas usando SOMENTE as letras de word_forming_letters, respeitando a quantidade de \
cada letra disponivel.
"""


class _QuizQuestionSchema(BaseModel):
    prompt: str
    options: list[str] = Field(min_length=4, max_length=4)
    correct_option_index: int


class _FlashcardSchema(BaseModel):
    question: str
    answer: str


class _GameContentSchema(BaseModel):
    quiz: list[_QuizQuestionSchema]
    flashcards: list[_FlashcardSchema]
    guess_word_answer: str
    crossword_words: list[str]
    word_forming_letters: list[str]
    word_forming_valid_words: list[str]


def _can_form_word(word: str, letter_pool: Counter[str]) -> bool:
    word_counts = Counter(word)
    return all(letter_pool.get(letter, 0) >= count for letter, count in word_counts.items())


class GeminiGameContentGenerator(GameContentGenerator):
    """Implementacao de GameContentGenerator usando o Gemini, com saida estruturada
    (JSON validado por schema) para evitar parsing fragil de texto livre."""

    def __init__(self, api_key: str, model: str) -> None:
        self._client = genai.Client(api_key=api_key)
        self._model = model

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
                ),
            )
        except APIError as error:
            raise AIServiceError(filename, str(error)) from error
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
        quiz = []
        seen_prompts: set[str] = set()
        for q in schema.quiz:
            prompt_text = q.prompt.strip()
            dedup_key = prompt_text.lower()
            if not prompt_text or len(q.options) != 4 or dedup_key in seen_prompts:
                continue
            seen_prompts.add(dedup_key)
            quiz.append(
                QuizQuestion(
                    prompt=prompt_text,
                    options=[option.strip() for option in q.options],
                    correct_option_index=min(max(q.correct_option_index, 0), len(q.options) - 1),
                )
            )

        flashcards = []
        seen_questions: set[str] = set()
        for card in schema.flashcards:
            question_text = card.question.strip()
            answer_text = card.answer.strip()
            dedup_key = question_text.lower()
            if not question_text or not answer_text or dedup_key in seen_questions:
                continue
            seen_questions.add(dedup_key)
            flashcards.append(Flashcard(question=question_text, answer=answer_text))

        guess_word_answer = normalize_word(schema.guess_word_answer)
        crossword_words = [normalize_word(word) for word in schema.crossword_words]
        crossword_words = [word for word in crossword_words if word]

        if not guess_word_answer and crossword_words:
            guess_word_answer = max(crossword_words, key=len)

        raw_letters = [letter for raw in schema.word_forming_letters if (letter := normalize_word(raw))]
        raw_letter_pool = Counter("".join(raw_letters))

        word_forming_valid_words = [
            word
            for raw_word in schema.word_forming_valid_words
            if (word := normalize_word(raw_word)) and len(word) >= 3 and _can_form_word(word, raw_letter_pool)
        ]

        # Deriva o conjunto final de letras a partir das proprias palavras validadas
        # (igual ao cruzamento), garantindo por construcao que toda palavra valida
        # realmente pode ser formada a partir das letras devolvidas -- independente de
        # a lista "crua" de letras da IA estar perfeita ou nao.
        if word_forming_valid_words:
            derived_pool: Counter[str] = Counter()
            for word in word_forming_valid_words:
                for letter, count in Counter(word).items():
                    derived_pool[letter] = max(derived_pool[letter], count)
            word_forming_letters = list(derived_pool.elements())
        else:
            word_forming_letters = list(raw_letter_pool.elements())

        return RawGameContent(
            quiz=quiz,
            flashcards=flashcards,
            guess_word_answer=guess_word_answer,
            crossword_words=crossword_words,
            word_forming_letters=word_forming_letters,
            word_forming_valid_words=word_forming_valid_words,
            total_tokens=total_tokens,
            response_time_ms=response_time_ms,
        )
