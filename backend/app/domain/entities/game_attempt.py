from dataclasses import dataclass, field
from datetime import datetime, timezone
from uuid import UUID, uuid4


@dataclass(frozen=True, slots=True)
class GameAttempt:
    user_id: UUID
    game_type: str
    file_title: str
    score_label: str
    xp_earned: int
    best_score_label: str
    id: UUID = field(default_factory=uuid4)
    played_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
