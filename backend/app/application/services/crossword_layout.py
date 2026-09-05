import unicodedata
from collections import Counter

from app.domain.entities.games import CrosswordEntry, CrosswordGame

_MIN_WORD_LENGTH = 3
_MAX_WORD_LENGTH = 12
_DIRECTIONS = ("ACROSS", "DOWN")

_Cell = tuple[int, int]
_Placement = tuple[str, str, int, int]  # direction, word, row, col


def normalize_word(word: str) -> str:
    """Caixa alta, sem acentos, mantendo apenas letras A-Z."""
    decomposed = unicodedata.normalize("NFKD", word)
    ascii_only = "".join(ch for ch in decomposed if not unicodedata.combining(ch))
    return "".join(ch for ch in ascii_only.upper() if ch.isalpha())


class CrosswordLayoutService:
    """Posiciona uma lista de palavras numa grade de cruzamento sem dicas de texto
    (estilo Word of Wonders): cada palavra que se encaixa, cruzando alguma letra ja
    posicionada, entra na grade; as que nao encaixam viram palavras bonus. Tambem
    calcula o conjunto de letras necessario para formar todas as palavras na roda.
    """

    def build(self, candidate_words: list[str]) -> CrosswordGame:
        words = self._prepare_words(candidate_words)
        if not words:
            return CrosswordGame(size=0, letters=[], entries=[], bonus_words=[])

        placements, bonus_words = self._place_words(words)
        entries, size = self._normalize_placements(placements)
        letters = self._letter_pool(entries, bonus_words)
        return CrosswordGame(size=size, letters=letters, entries=entries, bonus_words=bonus_words)

    def _prepare_words(self, candidate_words: list[str]) -> list[str]:
        seen: set[str] = set()
        prepared: list[str] = []
        for raw in candidate_words:
            word = normalize_word(raw)
            if _MIN_WORD_LENGTH <= len(word) <= _MAX_WORD_LENGTH and word not in seen:
                seen.add(word)
                prepared.append(word)
        prepared.sort(key=len, reverse=True)
        return prepared

    def _place_words(self, words: list[str]) -> tuple[list[_Placement], list[str]]:
        grid: dict[_Cell, str] = {}
        placements: list[_Placement] = []
        bonus_words: list[str] = []

        first = words[0]
        self._write(grid, first, 0, 0, "ACROSS")
        placements.append(("ACROSS", first, 0, 0))

        for word in words[1:]:
            placement = self._find_placement(grid, word)
            if placement is None:
                bonus_words.append(word)
                continue
            direction, row, col = placement
            self._write(grid, word, row, col, direction)
            placements.append((direction, word, row, col))

        return placements, bonus_words

    def _find_placement(self, grid: dict[_Cell, str], word: str) -> tuple[str, int, int] | None:
        for i, letter in enumerate(word):
            for (r, c), existing_letter in list(grid.items()):
                if existing_letter != letter:
                    continue
                for direction in _DIRECTIONS:
                    row = r - i if direction == "DOWN" else r
                    col = c - i if direction == "ACROSS" else c
                    if self._fits(grid, word, row, col, direction):
                        return direction, row, col
        return None

    def _fits(self, grid: dict[_Cell, str], word: str, row: int, col: int, direction: str) -> bool:
        crosses_existing = False
        for i, letter in enumerate(word):
            r, c = (row, col + i) if direction == "ACROSS" else (row + i, col)
            existing = grid.get((r, c))
            if existing is not None:
                if existing != letter:
                    return False
                crosses_existing = True
            else:
                side_a, side_b = ((r - 1, c), (r + 1, c)) if direction == "ACROSS" else ((r, c - 1), (r, c + 1))
                if grid.get(side_a) is not None or grid.get(side_b) is not None:
                    return False

        before = (row, col - 1) if direction == "ACROSS" else (row - 1, col)
        after = (row, col + len(word)) if direction == "ACROSS" else (row + len(word), col)
        if grid.get(before) is not None or grid.get(after) is not None:
            return False

        return crosses_existing

    def _write(self, grid: dict[_Cell, str], word: str, row: int, col: int, direction: str) -> None:
        for i, letter in enumerate(word):
            r, c = (row, col + i) if direction == "ACROSS" else (row + i, col)
            grid[(r, c)] = letter

    def _normalize_placements(self, placements: list[_Placement]) -> tuple[list[CrosswordEntry], int]:
        all_cells: list[_Cell] = []
        for direction, word, row, col in placements:
            for i in range(len(word)):
                all_cells.append((row, col + i) if direction == "ACROSS" else (row + i, col))

        min_row = min(r for r, _ in all_cells)
        min_col = min(c for _, c in all_cells)
        max_row = max(r for r, _ in all_cells)
        max_col = max(c for _, c in all_cells)
        size = max(max_row - min_row, max_col - min_col) + 1

        entries = [
            CrosswordEntry(direction=direction, answer=word, row=row - min_row, col=col - min_col)
            for direction, word, row, col in placements
        ]
        return entries, size

    def _letter_pool(self, entries: list[CrosswordEntry], bonus_words: list[str]) -> list[str]:
        counts: Counter[str] = Counter()
        for word in [entry.answer for entry in entries] + bonus_words:
            for letter, count in Counter(word).items():
                counts[letter] = max(counts[letter], count)
        return list(counts.elements())
