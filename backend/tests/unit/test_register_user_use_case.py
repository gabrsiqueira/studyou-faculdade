import pytest

from app.application.use_cases.register_user import RegisterUserInput, RegisterUserUseCase
from app.domain.exceptions import EmailAlreadyRegisteredError
from tests.unit.fakes import FakePasswordHasher, FakeTokenService, FakeUserRepository


def make_use_case(user_repository=None, password_hasher=None, token_service=None):
    return RegisterUserUseCase(
        user_repository=user_repository or FakeUserRepository(),
        password_hasher=password_hasher or FakePasswordHasher(),
        token_service=token_service or FakeTokenService(),
    )


async def test_registers_new_user_and_issues_token():
    user_repository = FakeUserRepository()
    token_service = FakeTokenService()
    use_case = make_use_case(user_repository=user_repository, token_service=token_service)

    result = await use_case.execute(
        RegisterUserInput(name="Fred", email="Fred@Example.com", password="segredo123")
    )

    assert result.user.name == "Fred"
    assert result.user.email == "fred@example.com"
    assert result.user.password_hash == "hashed:segredo123"
    assert result.token == f"token-for-{result.user.id}"
    assert len(user_repository.users) == 1
    assert token_service.issued_for == [result.user.id]


async def test_rejects_registration_with_already_used_email():
    user_repository = FakeUserRepository()
    use_case = make_use_case(user_repository=user_repository)
    await use_case.execute(RegisterUserInput(name="Fred", email="fred@example.com", password="segredo123"))

    with pytest.raises(EmailAlreadyRegisteredError):
        await use_case.execute(RegisterUserInput(name="Outro", email="fred@example.com", password="outraSenha"))

    assert len(user_repository.users) == 1
