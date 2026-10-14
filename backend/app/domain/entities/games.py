from dataclasses import dataclass, field


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
    # Dica mostrada ao jogador para descobrir a palavra (vazia em jogos gerados antes das dicas).
    clue: str = ""


@dataclass(frozen=True, slots=True)
class CrosswordGame:
    size: int
    letters: list[str]
    entries: list[CrosswordEntry]
    bonus_words: list[str]


@dataclass(frozen=True, slots=True)
class GeneratedGames:
    """Todos os jogos gerados a partir do conteudo de um arquivo."""

    quiz: list[QuizQuestion]
    flashcards: list[Flashcard]
    guess_word: GuessWordGame
    crossword: CrosswordGame


@dataclass(frozen=True, slots=True)
class RawGameContent:
    """Conteudo de jogos gerado pela IA, antes do posicionamento geometrico das
    palavras cruzadas (que e feito deterministicamente por CrosswordLayoutService)."""

    quiz: list[QuizQuestion]
    flashcards: list[Flashcard]
    guess_word_answer: str
    crossword_words: list[str]
    # Dica de cada palavra das palavras cruzadas, indexada pela palavra normalizada.
    crossword_clues: dict[str, str] = field(default_factory=dict)
    # Prompt enviado a IA junto com o arquivo e o JSON bruto devolvido por ela.
    prompt_used: str = ""
    raw_response: str = ""
    # Tokens (entrada + saida) gastos na chamada a IA que gerou este conteudo.
    total_tokens: int = 0
    # Tempo de resposta (ms) da chamada a IA que gerou este conteudo.
    response_time_ms: int = 0
