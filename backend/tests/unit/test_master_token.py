from uuid import uuid4

import pytest
from fastapi.security import HTTPAuthorizationCredentials
from pydantic import ValidationError

from app.api.v1.dependencies import get_current_user
from app.application.use_cases.get_master_user import GetMasterUserUseCase
from app.core.config import Settings
from app.domain.entities.user import User
from app.domain.exceptions import UnauthorizedError
from tests.unit.fakes import FakePasswordHasher, FakeTokenService, FakeUserRepository

MASTER_TOKEN = "m" * 40


def make_settings(**overrides) -> Settings:
    values = dict(
        database_url="postgresql+asyncpg://x:x@localhost/x",
        gemini_api_key="fake",
        jwt_secret="fake",
        master_token=MASTER_TOKEN,
    )
    values.update(overrides)
    return Settings(_env_file=None, **values)


def bearer(token: str) -> HTTPAuthorizationCredentials:
    return HTTPAuthorizationCredentials(scheme="Bearer", credentials=token)


async def authenticate(token: str, settings: Settings, user_repository: FakeUserRepository) -> User:
    return await get_current_user(
        credentials=bearer(token),
        token_service=FakeTokenService(),
        user_repository=user_repository,
        settings=settings,
        master_user_use_case=GetMasterUserUseCase(
            user_repository, FakePasswordHasher(), settings.master_token_user_email
        ),
    )


async def test_master_user_is_created_once_and_reused():
    user_repository = FakeUserRepository()
    use_case = GetMasterUserUseCase(user_repository, FakePasswordHasher(), "Master@StudYou.local")

    first = await use_case.execute()
    second = await use_case.execute()

    assert first.email == "master@studyou.local"
    assert first.id == second.id
    assert len(user_repository.users) == 1


async def test_master_token_authenticates_as_master_user():
    user_repository = FakeUserRepository()

    user = await authenticate(MASTER_TOKEN, make_settings(), user_repository)

    assert user.email == "master@studyou.local"
    assert user_repository.users == [user]


async def test_master_token_uses_existing_user_with_configured_email():
    user_repository = FakeUserRepository()
    existing = User(name="Fred", email="fred@example.com", password_hash="hashed:segredo123")
    await user_repository.create(existing)

    user = await authenticate(MASTER_TOKEN, make_settings(master_token_user_email="fred@example.com"), user_repository)

    assert user == existing


async def test_wrong_token_is_still_rejected_when_master_token_is_enabled():
    with pytest.raises(UnauthorizedError):
        await authenticate(f"token-for-{uuid4()}", make_settings(), FakeUserRepository())


async def test_master_token_value_is_not_accepted_when_disabled():
    with pytest.raises(UnauthorizedError):
        await authenticate(MASTER_TOKEN, make_settings(master_token=""), FakeUserRepository())


def test_master_token_is_refused_in_production():
    with pytest.raises(ValidationError, match="producao|production"):
        make_settings(environment="production")


def test_master_token_must_be_long_enough():
    with pytest.raises(ValidationError, match="32 caracteres"):
        make_settings(master_token="curto")


@pytest.mark.parametrize("blank", ["", "   "])
def test_blank_master_token_user_email_falls_back_to_default(blank):
    assert make_settings(master_token_user_email=blank).master_token_user_email == "master@studyou.local"
