import re
import unicodedata

from app.application.services.crossword_layout import normalize_word
from app.domain.entities.games import Flashcard, QuizQuestion, RawGameContent

# Similaridade (Jaccard sobre palavras de conteudo) a partir da qual dois textos sao
# considerados a mesma pergunta reformulada.
_SIMILARITY_THRESHOLD = 0.5

_GUESS_WORD_MIN_LENGTH = 4
_GUESS_WORD_MAX_LENGTH = 10

_STOPWORDS = {
    "qual", "quais", "como", "onde", "quando", "quem", "porque", "para", "pela", "pelo",
    "pelas", "pelos", "sobre", "entre", "sendo", "esta", "este", "essa", "esse", "isso",
    "isto", "aquele", "aquela", "uma", "umas", "uns", "dos", "das", "nos", "nas", "com",
    "sem", "que", "sao", "ser", "foi", "tem", "mais", "menos", "muito", "principal",
}


def _content_tokens(text: str) -> set[str]:
    decomposed = unicodedata.normalize("NFKD", text.lower())
    ascii_text = "".join(ch for ch in decomposed if not unicodedata.combining(ch))
    return {token for token in re.split(r"[^a-z0-9]+", ascii_text) if len(token) >= 3 and token not in _STOPWORDS}


def _is_similar(a: str, b: str) -> bool:
    tokens_a, tokens_b = _content_tokens(a), _content_tokens(b)
    if not tokens_a or not tokens_b:
        return a.strip().lower() == b.strip().lower()
    return len(tokens_a & tokens_b) / len(tokens_a | tokens_b) >= _SIMILARITY_THRESHOLD


def _unique_quiz(quiz: list[QuizQuestion]) -> list[QuizQuestion]:
    kept: list[QuizQuestion] = []
    for question in quiz:
        if not any(_is_similar(question.prompt, other.prompt) for other in kept):
            kept.append(question)
    return kept


def _unique_flashcards(flashcards: list[Flashcard], quiz: list[QuizQuestion]) -> list[Flashcard]:
    quiz_texts = [
        (question.prompt, f"{question.prompt} {question.options[question.correct_option_index]}")
        for question in quiz
    ]
    kept: list[Flashcard] = []
    for card in flashcards:
        card_text = f"{card.question} {card.answer}"
        repeats_quiz = any(
            _is_similar(card.question, prompt) or _is_similar(card_text, prompt_with_answer)
            for prompt, prompt_with_answer in quiz_texts
        )
        repeats_card = any(_is_similar(card.question, other.question) for other in kept)
        if not repeats_quiz and not repeats_card:
            kept.append(card)
    return kept


def _unique_words(words: list[str], excluded: set[str]) -> list[str]:
    kept: list[str] = []
    for word in words:
        normalized = normalize_word(word)
        if normalized and normalized not in excluded and normalized not in kept:
            kept.append(normalized)
    return kept


def _is_valid_guess_word(word: str) -> bool:
    """Palavra do guess_word: uma palavra so, de 4 a 10 letras (como pedido no prompt)."""
    return word.isalpha() and _GUESS_WORD_MIN_LENGTH <= len(word) <= _GUESS_WORD_MAX_LENGTH


def enforce_unique_content(raw: RawGameContent) -> RawGameContent:
    """Garante que nenhum jogo repita o conteudo de outro: os flashcards nao repetem
    perguntas do quiz e as palavras cruzadas nao repetem a palavra do Termo. Itens
    repetidos sao descartados (a IA ja
    e instruida a nao repetir; isto e a garantia caso ela repita mesmo assim)."""
    quiz = _unique_quiz(raw.quiz)
    flashcards = _unique_flashcards(raw.flashcards, quiz)

    guess_word = normalize_word(raw.guess_word_answer)
    crossword_words = _unique_words(raw.crossword_words, excluded={guess_word})
    if not _is_valid_guess_word(guess_word):
        # Sem palavra valida para o guess_word: usa a palavra mais longa do crossword.
        guess_word = max(crossword_words, key=len) if crossword_words else ""
        if guess_word:
            crossword_words.remove(guess_word)

    return RawGameContent(
        quiz=quiz,
        flashcards=flashcards,
        guess_word_answer=guess_word,
        crossword_words=crossword_words,
        prompt_used=raw.prompt_used,
        total_tokens=raw.total_tokens,
        response_time_ms=raw.response_time_ms,
    )
