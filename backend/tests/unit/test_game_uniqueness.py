from app.application.services.game_uniqueness import enforce_unique_content
from app.domain.entities.games import Flashcard, QuizQuestion
from tests.unit.fakes import make_raw_game_content


def _quiz(prompt: str, correct: str) -> QuizQuestion:
    return QuizQuestion(prompt=prompt, options=[correct, "B", "C", "D"], correct_option_index=0)


def test_drops_flashcard_that_repeats_a_quiz_question():
    raw = make_raw_game_content(
        quiz=[_quiz("Quantas camaras tem o coracao humano?", "Quatro")],
        flashcards=[
            Flashcard(question="Quantas câmaras possui o coração humano?", answer="Quatro"),
            Flashcard(question="O que impede o refluxo do sangue?", answer="As valvulas cardiacas"),
        ],
    )

    result = enforce_unique_content(raw)

    assert [card.question for card in result.flashcards] == ["O que impede o refluxo do sangue?"]


def test_drops_reworded_duplicate_quiz_questions_and_flashcards():
    raw = make_raw_game_content(
        quiz=[
            _quiz("Qual e a funcao do ventriculo esquerdo?", "Bombear sangue ao corpo"),
            _quiz("Qual a funcao do ventriculo esquerdo do coracao?", "Bombear sangue ao corpo"),
        ],
        flashcards=[
            Flashcard(question="Onde ficam os atrios?", answer="Na parte superior"),
            Flashcard(question="Onde ficam localizados os atrios?", answer="Em cima"),
        ],
    )

    result = enforce_unique_content(raw)

    assert len(result.quiz) == 1
    assert len(result.flashcards) == 1


def test_word_games_never_share_words():
    raw = make_raw_game_content(
        guess_word_answer="coração",
        crossword_words=["CORACAO", "SANGUE", "Sangue", "VALVULA"],
    )

    result = enforce_unique_content(raw)

    assert result.guess_word_answer == "CORACAO"
    assert result.crossword_words == ["SANGUE", "VALVULA"]


def test_keeps_clues_only_for_words_that_stay_in_the_crossword():
    raw = make_raw_game_content(
        guess_word_answer="CORACAO",
        crossword_words=["CORACAO", "SANGUE"],
        crossword_clues={"CORACAO": "Orgao que bombeia", "SANGUE": "Liquido vermelho do corpo"},
    )

    result = enforce_unique_content(raw)

    assert result.crossword_clues == {"SANGUE": "Liquido vermelho do corpo"}


def test_falls_back_to_longest_crossword_word_when_guess_word_is_invalid():
    raw = make_raw_game_content(
        guess_word_answer="SOL",
        crossword_words=["MAR", "FOTOSSINTESE", "CLOROFILA"],
        crossword_clues={"FOTOSSINTESE": "Processo vegetal", "CLOROFILA": "Pigmento verde"},
    )

    result = enforce_unique_content(raw)

    assert result.guess_word_answer == "FOTOSSINTESE"
    assert result.crossword_words == ["MAR", "CLOROFILA"]
    assert result.crossword_clues == {"CLOROFILA": "Pigmento verde"}
