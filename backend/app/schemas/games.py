from pydantic import BaseModel

from app.domain.entities.games import (
    CrosswordEntry,
    CrosswordGame,
    Flashcard,
    GeneratedGames,
    GuessWordGame,
    QuizQuestion,
)


class QuizQuestionResponse(BaseModel):
    prompt: str
    options: list[str]
    correct_option_index: int

    @classmethod
    def from_entity(cls, entity: QuizQuestion) -> "QuizQuestionResponse":
        return cls(prompt=entity.prompt, options=entity.options, correct_option_index=entity.correct_option_index)


class FlashcardResponse(BaseModel):
    question: str
    answer: str

    @classmethod
    def from_entity(cls, entity: Flashcard) -> "FlashcardResponse":
        return cls(question=entity.question, answer=entity.answer)


class GuessWordGameResponse(BaseModel):
    answer: str
    max_attempts: int

    @classmethod
    def from_entity(cls, entity: GuessWordGame) -> "GuessWordGameResponse":
        return cls(answer=entity.answer, max_attempts=entity.max_attempts)


class CrosswordEntryResponse(BaseModel):
    direction: str
    answer: str
    row: int
    col: int

    @classmethod
    def from_entity(cls, entity: CrosswordEntry) -> "CrosswordEntryResponse":
        return cls(direction=entity.direction, answer=entity.answer, row=entity.row, col=entity.col)


class CrosswordGameResponse(BaseModel):
    size: int
    letters: list[str]
    entries: list[CrosswordEntryResponse]
    bonus_words: list[str]

    @classmethod
    def from_entity(cls, entity: CrosswordGame) -> "CrosswordGameResponse":
        return cls(
            size=entity.size,
            letters=entity.letters,
            entries=[CrosswordEntryResponse.from_entity(item) for item in entity.entries],
            bonus_words=entity.bonus_words,
        )


class GeneratedGamesResponse(BaseModel):
    quiz: list[QuizQuestionResponse]
    flashcards: list[FlashcardResponse]
    guess_word: GuessWordGameResponse
    crossword: CrosswordGameResponse

    @classmethod
    def from_entity(cls, entity: GeneratedGames) -> "GeneratedGamesResponse":
        return cls(
            quiz=[QuizQuestionResponse.from_entity(item) for item in entity.quiz],
            flashcards=[FlashcardResponse.from_entity(item) for item in entity.flashcards],
            guess_word=GuessWordGameResponse.from_entity(entity.guess_word),
            crossword=CrosswordGameResponse.from_entity(entity.crossword),
        )

    def to_entity(self) -> GeneratedGames:
        return GeneratedGames(
            quiz=[QuizQuestion(prompt=q.prompt, options=q.options, correct_option_index=q.correct_option_index) for q in self.quiz],
            flashcards=[Flashcard(question=c.question, answer=c.answer) for c in self.flashcards],
            guess_word=GuessWordGame(answer=self.guess_word.answer, max_attempts=self.guess_word.max_attempts),
            crossword=CrosswordGame(
                size=self.crossword.size,
                letters=self.crossword.letters,
                entries=[
                    CrosswordEntry(direction=e.direction, answer=e.answer, row=e.row, col=e.col)
                    for e in self.crossword.entries
                ],
                bonus_words=self.crossword.bonus_words,
            ),
        )
