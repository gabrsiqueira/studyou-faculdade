from collections import Counter

from app.application.services.crossword_layout import CrosswordLayoutService, normalize_word


def test_normalize_word_strips_accents_and_keeps_only_letters():
    assert normalize_word("fotossíntese") == "FOTOSSINTESE"
    assert normalize_word("átrio-3") == "ATRIO"
    assert normalize_word("  Ação  ") == "ACAO"


def test_build_with_no_valid_words_returns_empty_game():
    game = CrosswordLayoutService().build(["a", "bc", ""])

    assert game.size == 0
    assert game.entries == []
    assert game.bonus_words == []
    assert game.letters == []


def test_build_places_single_word():
    game = CrosswordLayoutService().build(["SOL"])

    assert game.size == 3
    assert len(game.entries) == 1
    assert game.entries[0].answer == "SOL"
    assert game.bonus_words == []


def test_build_places_two_intersecting_words_without_conflict():
    game = CrosswordLayoutService().build(["TERRA", "MARTE"])

    placed_answers = {entry.answer for entry in game.entries}
    assert placed_answers == {"TERRA", "MARTE"}
    assert game.bonus_words == []

    # nenhuma celula deve ter duas letras diferentes atribuidas a ela
    cells: dict[tuple[int, int], str] = {}
    for entry in game.entries:
        for i, letter in enumerate(entry.answer):
            cell = (entry.row, entry.col + i) if entry.direction == "ACROSS" else (entry.row + i, entry.col)
            assert cells.get(cell, letter) == letter
            cells[cell] = letter

    # as duas palavras compartilham pelo menos uma celula (se cruzam de fato)
    assert game.size < len("TERRA") + len("MARTE")


def test_build_sends_non_intersecting_word_to_bonus():
    game = CrosswordLayoutService().build(["ABCDE", "FGHIJ"])

    placed_answers = {entry.answer for entry in game.entries}
    assert placed_answers == {"ABCDE"}
    assert game.bonus_words == ["FGHIJ"]


def test_prepare_words_dedupes_case_insensitively_and_filters_by_length():
    game = CrosswordLayoutService().build(["sol", "SOL", "ab", "um-termo-bem-comprido-demais"])

    assert [entry.answer for entry in game.entries] == ["SOL"]


def test_letter_pool_uses_max_count_per_letter_across_words():
    game = CrosswordLayoutService().build(["TERRA", "MARTE"])

    expected = Counter()
    for word in ["TERRA", "MARTE"]:
        for letter, count in Counter(word).items():
            expected[letter] = max(expected[letter], count)

    assert Counter(game.letters) == expected
