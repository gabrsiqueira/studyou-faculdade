from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.domain.entities.game_attempt import GameAttempt
from app.domain.interfaces.game_attempt_repository import GameAttemptRepository
from app.infrastructure.persistence.models import GameAttemptModel


class SqlAlchemyGameAttemptRepository(GameAttemptRepository):
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def save(self, attempt: GameAttempt) -> None:
        model = GameAttemptModel(
            id=attempt.id,
            user_id=attempt.user_id,
            game_type=attempt.game_type,
            file_title=attempt.file_title,
            score_label=attempt.score_label,
            xp_earned=attempt.xp_earned,
            best_score_label=attempt.best_score_label,
        )
        self._session.add(model)
        await self._session.commit()

    async def list_for_user(self, user_id: UUID) -> list[GameAttempt]:
        query = (
            select(GameAttemptModel)
            .where(GameAttemptModel.user_id == user_id)
            .order_by(GameAttemptModel.played_at.desc())
        )
        rows = (await self._session.execute(query)).scalars().all()
        return [
            GameAttempt(
                id=row.id,
                user_id=row.user_id,
                game_type=row.game_type,
                file_title=row.file_title,
                score_label=row.score_label,
                xp_earned=row.xp_earned,
                best_score_label=row.best_score_label,
                played_at=row.played_at,
            )
            for row in rows
        ]
