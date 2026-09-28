from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.domain.entities.user import User
from app.domain.interfaces.user_repository import UserRepository
from app.infrastructure.persistence.models import UserModel


class SqlAlchemyUserRepository(UserRepository):
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def create(self, user: User) -> None:
        model = UserModel(
            id=user.id,
            name=user.name,
            email=user.email,
            password_hash=user.password_hash,
        )
        self._session.add(model)
        await self._session.commit()

    async def get_by_email(self, email: str) -> User | None:
        row = (
            await self._session.execute(select(UserModel).where(UserModel.email == email))
        ).scalar_one_or_none()
        return self._to_entity(row) if row else None

    async def get_by_id(self, user_id: UUID) -> User | None:
        row = (
            await self._session.execute(select(UserModel).where(UserModel.id == user_id))
        ).scalar_one_or_none()
        return self._to_entity(row) if row else None

    def _to_entity(self, row: UserModel) -> User:
        return User(
            id=row.id,
            name=row.name,
            email=row.email,
            password_hash=row.password_hash,
            created_at=row.created_at,
        )
