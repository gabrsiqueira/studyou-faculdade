from app.infrastructure.ai.gemini_game_generator import _clues_by_word, _CrosswordWordSchema


def test_drops_clues_that_contain_the_answer():
    clues = _clues_by_word(
        [
            _CrosswordWordSchema(word="Música", clue="Arte de combinar sons"),
            _CrosswordWordSchema(word="POESIA", clue="Texto poético; a poesia em versos"),
            _CrosswordWordSchema(word="ARTE", clue="Expressão criativa das partes da cultura"),
            _CrosswordWordSchema(word="DANCA", clue="   "),
        ]
    )

    assert clues == {"MUSICA": "Arte de combinar sons", "ARTE": "Expressão criativa das partes da cultura"}
