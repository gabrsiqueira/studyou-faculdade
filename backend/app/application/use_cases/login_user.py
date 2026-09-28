from dataclasses import dataclass

from app.application.use_cases.register_user import AuthResult
from app.domain.exceptions import InvalidCredentialsError
from app.domain.interfaces.password_hasher import PasswordHasher
from app.domain.interfaces.token_service import TokenService
from app.domain.interfaces.user_repository import UserRepository


@dataclass
class LoginUserInput:
    email: str
    password: str


class LoginUserUseCase:
    def __init__(
        self,
        user_repository: UserRepository,
        password_hasher: PasswordHasher,
        token_service: TokenService,
    ) -> None:
        self._user_repository = user_repository
        self._password_hasher = password_hasher
        self._token_service = token_service

    async def execute(self, data: LoginUserInput) -> AuthResult:
        email = data.email.strip().lower()
        user = await self._user_repository.get_by_email(email)
        if user is None or not self._password_hasher.verify(data.password, user.password_hash):
            raise InvalidCredentialsError()

        token = self._token_service.issue(user.id)
        return AuthResult(user=user, token=token)
