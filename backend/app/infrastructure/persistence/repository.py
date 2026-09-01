from sqlalchemy.ext.asyncio import AsyncSession

from app.domain.entities.analysis import FileAnalysisError, FileAnalysisResult
from app.domain.interfaces.analysis_repository import AnalysisRepository
from app.infrastructure.persistence.models import FileAnalysisErrorModel, FileAnalysisResultModel


class SqlAlchemyAnalysisRepository(AnalysisRepository):
    """Implementacao de AnalysisRepository usando SQLAlchemy (DB do diagrama de arquitetura)."""

    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def save_result(self, result: FileAnalysisResult) -> None:
        model = FileAnalysisResultModel(
            id=result.id,
            filename=result.filename,
            mime_type=result.mime_type,
            prompt_used=result.prompt_used,
            content=result.content,
            status=result.status.value,
        )
        self._session.add(model)
        await self._session.commit()

    async def save_error(self, error: FileAnalysisError) -> None:
        model = FileAnalysisErrorModel(
            id=error.id,
            filename=error.filename,
            mime_type=error.mime_type,
            reason=error.reason,
            status=error.status.value,
        )
        self._session.add(model)
        await self._session.commit()
