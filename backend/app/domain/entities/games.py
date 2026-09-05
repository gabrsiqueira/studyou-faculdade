from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class QuizQuestion:
    prompt: str
    options: list[str]
    correct_option_index: int


@dataclass(frozen=True, slots=True)
class Flashcard:
    question: str
    answer: str


@dataclass(frozen=True, slots=True)
class GuessWordGame:
    answer: str
    max_attempts: int = 6


@dataclass(frozen=True, slots=True)
class CrosswordEntry:
    direction: str  # "ACROSS" ou "DOWN"
    answer: str
    row: int
    col: int


@dataclass(frozen=True, slots=True)
class CrosswordGame:
    size: int
    letters: list[str]
    entries: list[CrosswordEntry]
    bonus_words: list[str]


@dataclass(frozen=True, slots=True)
class WordFormingGame:
    letters: list[str]
    valid_words: list[str]


@dataclass(frozen=True, slots=True)
class GeneratedGames:
    """Todos os jogos gerados a partir do conteudo analisado de um arquivo."""

    quiz: list[QuizQuestion]
    flashcards: list[Flashcard]
    guess_word: GuessWordGame
    crossword: CrosswordGame
    word_forming: WordFormingGame


@dataclass(frozen=True, slots=True)
class RawGameContent:
    """Conteudo de jogos gerado pela IA, antes do posicionamento geometrico do
    cruzamento (que e feito deterministicamente por CrosswordLayoutService)."""

    quiz: list[QuizQuestion]
    flashcards: list[Flashcard]
    guess_word_answer: str
    crossword_words: list[str]
    word_forming_letters: list[str]
    word_forming_valid_words: list[str]
