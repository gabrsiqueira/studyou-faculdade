from abc import ABC, abstractmethod
from uuid import UUID


class TokenService(ABC):
    @abstractmethod
    def issue(self, user_id: UUID) -> str: ...

    @abstractmethod
    def verify(self, token: str) -> UUID:
        """Deve levantar UnauthorizedError se o token for invalido ou expirado."""
