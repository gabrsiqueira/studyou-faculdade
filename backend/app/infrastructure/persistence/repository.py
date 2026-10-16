from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.domain.entities.analysis import FileAnalysisError, FileAnalysisResult, FileAnalysisStatus
from app.domain.interfaces.analysis_repository import AnalysisRepository
from app.infrastructure.persistence.models import FileAnalysisErrorModel, FileAnalysisResultModel
from app.schemas.games import GeneratedGamesResponse


class SqlAlchemyAnalysisRepository(AnalysisRepository):
    """Implementacao de AnalysisRepository usando SQLAlchemy (DB do diagrama de arquitetura)."""

    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def save_result(self, result: FileAnalysisResult) -> None:
        model = FileAnalysisResultModel(
            id=result.id,
            user_id=result.user_id,
            filename=result.filename,
            mime_type=result.mime_type,
            prompt_used=result.prompt_used,
            content="",
            games=GeneratedGamesResponse.from_entity(result.games).model_dump(mode="json"),
            status=result.status.value,
        )
        self._session.add(model)
        await self._session.commit()

    async def save_error(self, error: FileAnalysisError) -> None:
        model = FileAnalysisErrorModel(
            id=error.id,
            user_id=error.user_id,
            filename=error.filename,
            mime_type=error.mime_type,
            reason=error.reason,
            status=error.status.value,
        )
        self._session.add(model)
        await self._session.commit()

    async def list_results_for_user(self, user_id: UUID) -> list[FileAnalysisResult]:
        query = (
            select(FileAnalysisResultModel)
            .where(FileAnalysisResultModel.user_id == user_id)
            .order_by(FileAnalysisResultModel.created_at.desc())
        )
        rows = (await self._session.execute(query)).scalars().all()
        return [
            FileAnalysisResult(
                id=row.id,
                user_id=row.user_id,
                filename=row.filename,
                mime_type=row.mime_type,
                prompt_used=row.prompt_used,
                games=GeneratedGamesResponse.model_validate(row.games).to_entity(),
                status=FileAnalysisStatus(row.status),
                created_at=row.created_at,
            )
            for row in rows
        ]
