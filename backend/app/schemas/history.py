from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, Field

from app.domain.entities.game_attempt import GameAttempt


class SaveGameAttemptRequest(BaseModel):
    file_id: UUID | None = None
    game_type: str
    file_title: str
    score_label: str
    xp_earned: int
    score_percent: int = Field(ge=0, le=100)
    best_score_label: str


class GameAttemptResponse(BaseModel):
    id: UUID
    file_id: UUID | None
    game_type: str
    file_title: str
    score_label: str
    xp_earned: int
    score_percent: int
    best_score_label: str
    played_at: datetime

    @classmethod
    def from_entity(cls, entity: GameAttempt) -> "GameAttemptResponse":
        return cls(
            id=entity.id,
            file_id=entity.file_id,
            game_type=entity.game_type,
            file_title=entity.file_title,
            score_label=entity.score_label,
            xp_earned=entity.xp_earned,
            score_percent=entity.score_percent,
            best_score_label=entity.best_score_label,
            played_at=entity.played_at,
        )
