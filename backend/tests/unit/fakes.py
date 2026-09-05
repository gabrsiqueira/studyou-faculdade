from app.domain.entities.analysis import FileAnalysisError, FileAnalysisResult
from app.domain.entities.games import RawGameContent
from app.domain.exceptions import ContentExceedsContextLimitError
from app.domain.interfaces.ai_content_analyzer import AIContentAnalyzer
from app.domain.interfaces.analysis_repository import AnalysisRepository
from app.domain.interfaces.game_content_generator import GameContentGenerator


class FakeAIContentAnalyzer(AIContentAnalyzer):
    def __init__(self, within_limit: bool = True, analysis_output: str = "analise gerada"):
        self.within_limit = within_limit
        self.analysis_output = analysis_output
        self.analyze_calls: list[tuple[str, bytes, str, str]] = []

    async def analyze(self, filename: str, content: bytes, mime_type: str, prompt: str) -> str:
        self.analyze_calls.append((filename, content, mime_type, prompt))
        if not self.within_limit:
            raise ContentExceedsContextLimitError(filename)
        return self.analysis_output


def make_raw_game_content(**overrides) -> RawGameContent:
    defaults = dict(
        quiz=[],
        flashcards=[],
        guess_word_answer="PALAVRA",
        crossword_words=["PALAVRA", "TERMO"],
        word_forming_letters=list("PALAVRA"),
        word_forming_valid_words=["PALAVRA", "PARA"],
    )
    defaults.update(overrides)
    return RawGameContent(**defaults)


class FakeGameContentGenerator(GameContentGenerator):
    def __init__(self, raw_game_content: RawGameContent | None = None):
        self.raw_game_content = raw_game_content or make_raw_game_content()
        self.generate_calls: list[tuple[str, str]] = []

    async def generate(self, filename: str, analysis_text: str) -> RawGameContent:
        self.generate_calls.append((filename, analysis_text))
        return self.raw_game_content


class FakeAnalysisRepository(AnalysisRepository):
    def __init__(self) -> None:
        self.saved_results: list[FileAnalysisResult] = []
        self.saved_errors: list[FileAnalysisError] = []

    async def save_result(self, result: FileAnalysisResult) -> None:
        self.saved_results.append(result)

    async def save_error(self, error: FileAnalysisError) -> None:
        self.saved_errors.append(error)
