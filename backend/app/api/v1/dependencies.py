from functools import lru_cache
from typing import Annotated

from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.application.use_cases.analyze_file import AnalyzeFileUseCase
from app.core.config import Settings, get_settings
from app.domain.interfaces.ai_content_analyzer import AIContentAnalyzer
from app.infrastructure.ai.gemini_analyzer import GeminiContentAnalyzer
from app.infrastructure.persistence.database import get_db_session
from app.infrastructure.persistence.repository import SqlAlchemyAnalysisRepository
from app.infrastructure.validation.format_validator import ExtensionFileFormatValidator
from app.infrastructure.validation.size_validator import MaxSizeFileValidator


@lru_cache
def get_ai_analyzer() -> AIContentAnalyzer:
    settings = get_settings()
    return GeminiContentAnalyzer(
        api_key=settings.gemini_api_key,
        model=settings.gemini_model,
        max_context_tokens=settings.gemini_max_context_tokens,
    )


def get_analyze_file_use_case(
    session: Annotated[AsyncSession, Depends(get_db_session)],
    settings: Annotated[Settings, Depends(get_settings)],
    ai_analyzer: Annotated[AIContentAnalyzer, Depends(get_ai_analyzer)],
) -> AnalyzeFileUseCase:
    return AnalyzeFileUseCase(
        format_validator=ExtensionFileFormatValidator(settings.allowed_file_formats_set),
        size_validator=MaxSizeFileValidator(settings.max_file_size_bytes),
        ai_analyzer=ai_analyzer,
        repository=SqlAlchemyAnalysisRepository(session),
        analysis_prompt=settings.gemini_analysis_prompt,
    )
