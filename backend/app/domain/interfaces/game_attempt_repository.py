from abc import ABC, abstractmethod
from uuid import UUID

from app.domain.entities.game_attempt import GameAttempt


class GameAttemptRepository(ABC):
    @abstractmethod
    async def save(self, attempt: GameAttempt) -> None: ...

    @abstractmethod
    async def list_for_user(self, user_id: UUID) -> list[GameAttempt]: ...
