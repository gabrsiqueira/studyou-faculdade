import pytest

from app.application.use_cases.analyze_file import AnalyzeFileInput, AnalyzeFileUseCase
from app.domain.exceptions import ContentExceedsContextLimitError, FileTooLargeError, UnsupportedFileFormatError
from app.infrastructure.validation.format_validator import ExtensionFileFormatValidator
from app.infrastructure.validation.size_validator import MaxSizeFileValidator
from tests.unit.fakes import FakeAIContentAnalyzer, FakeAnalysisRepository


def make_use_case(ai_analyzer=None, repository=None, allowed_formats=None, max_size=1_000):
    return AnalyzeFileUseCase(
        format_validator=ExtensionFileFormatValidator(allowed_formats or {"txt", "pdf"}),
        size_validator=MaxSizeFileValidator(max_size),
        ai_analyzer=ai_analyzer or FakeAIContentAnalyzer(),
        repository=repository or FakeAnalysisRepository(),
        analysis_prompt="resuma o conteudo",
    )


async def test_rejects_unsupported_format_before_calling_ai():
    ai_analyzer = FakeAIContentAnalyzer()
    use_case = make_use_case(ai_analyzer=ai_analyzer)

    with pytest.raises(UnsupportedFileFormatError):
        await use_case.execute(AnalyzeFileInput(filename="video.mp4", mime_type="video/mp4", content=b"dados"))

    assert ai_analyzer.analyze_calls == []


async def test_rejects_file_too_large_before_calling_ai():
    ai_analyzer = FakeAIContentAnalyzer()
    use_case = make_use_case(ai_analyzer=ai_analyzer, max_size=3)

    with pytest.raises(FileTooLargeError):
        await use_case.execute(AnalyzeFileInput(filename="notas.txt", mime_type="text/plain", content=b"dados"))

    assert ai_analyzer.analyze_calls == []


async def test_persists_error_and_raises_when_content_exceeds_context_limit():
    ai_analyzer = FakeAIContentAnalyzer(within_limit=False)
    repository = FakeAnalysisRepository()
    use_case = make_use_case(ai_analyzer=ai_analyzer, repository=repository)

    with pytest.raises(ContentExceedsContextLimitError):
        await use_case.execute(AnalyzeFileInput(filename="notas.txt", mime_type="text/plain", content=b"dados"))

    assert len(repository.saved_errors) == 1
    assert repository.saved_errors[0].filename == "notas.txt"
    assert repository.saved_results == []


async def test_persists_result_when_analysis_succeeds():
    ai_analyzer = FakeAIContentAnalyzer(within_limit=True, analysis_output="resumo do conteudo")
    repository = FakeAnalysisRepository()
    use_case = make_use_case(ai_analyzer=ai_analyzer, repository=repository)

    result = await use_case.execute(
        AnalyzeFileInput(filename="notas.txt", mime_type="text/plain", content=b"dados")
    )

    assert result.content == "resumo do conteudo"
    assert len(repository.saved_results) == 1
    assert repository.saved_errors == []
    assert ai_analyzer.analyze_calls == [("notas.txt", b"dados", "text/plain", "resuma o conteudo")]
