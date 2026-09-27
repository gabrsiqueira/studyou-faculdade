from dataclasses import dataclass

from app.domain.entities.user import User
from app.domain.exceptions import EmailAlreadyRegisteredError
from app.domain.interfaces.password_hasher import PasswordHasher
from app.domain.interfaces.token_service import TokenService
from app.domain.interfaces.user_repository import UserRepository


@dataclass
class RegisterUserInput:
    name: str
    email: str
    password: str


@dataclass
class AuthResult:
    user: User
    token: str


class RegisterUserUseCase:
    def __init__(
        self,
        user_repository: UserRepository,
        password_hasher: PasswordHasher,
        token_service: TokenService,
    ) -> None:
        self._user_repository = user_repository
        self._password_hasher = password_hasher
        self._token_service = token_service

    async def execute(self, data: RegisterUserInput) -> AuthResult:
        email = data.email.strip().lower()
        if await self._user_repository.get_by_email(email) is not None:
            raise EmailAlreadyRegisteredError(email)

        user = User(
            name=data.name.strip(),
            email=email,
            password_hash=self._password_hasher.hash(data.password),
        )
        await self._user_repository.create(user)
        token = self._token_service.issue(user.id)
        return AuthResult(user=user, token=token)
