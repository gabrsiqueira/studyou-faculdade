import logging
import secrets

from app.domain.entities.user import User
from app.domain.interfaces.password_hasher import PasswordHasher
from app.domain.interfaces.user_repository import UserRepository

logger = logging.getLogger(__name__)


class GetMasterUserUseCase:
    """Devolve o usuario usado pelo token mestre (MASTER_TOKEN, apenas para testes),
    criando-o na primeira vez que o token e usado.

    A senha desse usuario e aleatoria e descartada: ele so e acessivel pelo token mestre,
    nunca pelo login normal. Se o e-mail configurado ja pertencer a um usuario existente,
    o token mestre passa a agir como esse usuario.
    """

    def __init__(self, user_repository: UserRepository, password_hasher: PasswordHasher, email: str) -> None:
        self._user_repository = user_repository
        self._password_hasher = password_hasher
        self._email = email.strip().lower()

    async def execute(self) -> User:
        user = await self._user_repository.get_by_email(self._email)
        if user is not None:
            return user

        user = User(
            name="Usuario de testes (token mestre)",
            email=self._email,
            password_hash=self._password_hasher.hash(secrets.token_urlsafe(32)),
        )
        await self._user_repository.create(user)
        logger.warning("Usuario do token mestre criado: %s (id=%s)", user.email, user.id)
        return user
