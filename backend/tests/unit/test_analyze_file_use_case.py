from uuid import uuid4

import pytest

from app.application.services.crossword_layout import CrosswordLayoutService
from app.application.use_cases.analyze_file import AnalyzeFileInput, AnalyzeFileUseCase
from app.domain.exceptions import ContentExceedsContextLimitError, FileTooLargeError, UnsupportedFileFormatError
from app.infrastructure.validation.format_validator import ExtensionFileFormatValidator
from app.infrastructure.validation.size_validator import MaxSizeFileValidator
from tests.unit.fakes import (
    FakeAnalysisRepository,
    FakeGameContentGenerator,
    make_raw_game_content,
)

USER_ID = uuid4()


def make_use_case(
    game_content_generator=None,
    repository=None,
    allowed_formats=None,
    max_size=1_000,
):
    return AnalyzeFileUseCase(
        format_validator=ExtensionFileFormatValidator(allowed_formats or {"txt", "pdf"}),
        size_validator=MaxSizeFileValidator(max_size),
        game_content_generator=game_content_generator or FakeGameContentGenerator(),
        crossword_layout_service=CrosswordLayoutService(),
        repository=repository or FakeAnalysisRepository(),
    )


async def test_rejects_unsupported_format_before_calling_ai():
    game_content_generator = FakeGameContentGenerator()
    use_case = make_use_case(game_content_generator=game_content_generator)

    with pytest.raises(UnsupportedFileFormatError):
        await use_case.execute(
            AnalyzeFileInput(user_id=USER_ID, filename="video.mp4", mime_type="video/mp4", content=b"dados")
        )

    assert game_content_generator.generate_calls == []


async def test_rejects_file_too_large_before_calling_ai():
    game_content_generator = FakeGameContentGenerator()
    use_case = make_use_case(game_content_generator=game_content_generator, max_size=3)

    with pytest.raises(FileTooLargeError):
        await use_case.execute(
            AnalyzeFileInput(user_id=USER_ID, filename="notas.txt", mime_type="text/plain", content=b"dados")
        )

    assert game_content_generator.generate_calls == []


async def test_persists_error_and_raises_when_content_exceeds_context_limit():
    game_content_generator = FakeGameContentGenerator(within_limit=False)
    repository = FakeAnalysisRepository()
    use_case = make_use_case(game_content_generator=game_content_generator, repository=repository)

    with pytest.raises(ContentExceedsContextLimitError):
        await use_case.execute(
            AnalyzeFileInput(user_id=USER_ID, filename="notas.txt", mime_type="text/plain", content=b"dados")
        )

    assert len(repository.saved_errors) == 1
    assert repository.saved_errors[0].filename == "notas.txt"
    assert repository.saved_results == []


async def test_persists_result_with_generated_games_when_generation_succeeds():
    game_content_generator = FakeGameContentGenerator(
        make_raw_game_content(
            prompt_used="prompt de geracao",
            total_tokens=1500,
            response_time_ms=2400,
        )
    )
    repository = FakeAnalysisRepository()
    use_case = make_use_case(game_content_generator=game_content_generator, repository=repository)

    result = await use_case.execute(
        AnalyzeFileInput(user_id=USER_ID, filename="notas.txt", mime_type="text/plain", content=b"dados")
    )

    assert result.prompt_used == "prompt de geracao"
    assert result.total_tokens == 1500
    assert result.ai_response_time_ms == 2400
    assert len(repository.saved_results) == 1
    assert repository.saved_errors == []
    assert game_content_generator.generate_calls == [("notas.txt", b"dados", "text/plain")]

    assert result.games.guess_word.answer == "PALAVRA"
    assert {entry.answer for entry in result.games.crossword.entries} | set(result.games.crossword.bonus_words) == {
        "TERMO",
        "METRO",
    }
