from app.domain.entities.analysis import FileAnalysisError, FileAnalysisResult
from app.domain.interfaces.ai_content_analyzer import AIContentAnalyzer
from app.domain.interfaces.analysis_repository import AnalysisRepository


class FakeAIContentAnalyzer(AIContentAnalyzer):
    def __init__(self, within_limit: bool = True, analysis_output: str = "analise gerada"):
        self.within_limit = within_limit
        self.analysis_output = analysis_output
        self.analyze_calls: list[tuple[str, bytes, str, str]] = []

    async def is_within_context_limit(self, filename: str, content: bytes, mime_type: str) -> bool:
        return self.within_limit

    async def analyze(self, filename: str, content: bytes, mime_type: str, prompt: str) -> str:
        self.analyze_calls.append((filename, content, mime_type, prompt))
        return self.analysis_output


class FakeAnalysisRepository(AnalysisRepository):
    def __init__(self) -> None:
        self.saved_results: list[FileAnalysisResult] = []
        self.saved_errors: list[FileAnalysisError] = []

    async def save_result(self, result: FileAnalysisResult) -> None:
        self.saved_results.append(result)

    async def save_error(self, error: FileAnalysisError) -> None:
        self.saved_errors.append(error)
