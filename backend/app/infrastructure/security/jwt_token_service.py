from datetime import datetime, timedelta, timezone
from uuid import UUID

import jwt

from app.domain.exceptions import UnauthorizedError
from app.domain.interfaces.token_service import TokenService

_ALGORITHM = "HS256"


class JwtTokenService(TokenService):
    def __init__(self, secret: str, expires_minutes: int) -> None:
        self._secret = secret
        self._expires_minutes = expires_minutes

    def issue(self, user_id: UUID) -> str:
        payload = {
            "sub": str(user_id),
            "exp": datetime.now(timezone.utc) + timedelta(minutes=self._expires_minutes),
        }
        return jwt.encode(payload, self._secret, algorithm=_ALGORITHM)

    def verify(self, token: str) -> UUID:
        try:
            payload = jwt.decode(token, self._secret, algorithms=[_ALGORITHM])
            return UUID(payload["sub"])
        except (jwt.PyJWTError, KeyError, ValueError) as error:
            raise UnauthorizedError() from error
