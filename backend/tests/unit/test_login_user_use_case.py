import pytest

from app.application.use_cases.login_user import LoginUserInput, LoginUserUseCase
from app.application.use_cases.register_user import RegisterUserInput, RegisterUserUseCase
from app.domain.exceptions import InvalidCredentialsError
from tests.unit.fakes import FakePasswordHasher, FakeTokenService, FakeUserRepository


async def _register(user_repository, password_hasher, token_service, email="fred@example.com", password="segredo123"):
    register = RegisterUserUseCase(user_repository, password_hasher, token_service)
    await register.execute(RegisterUserInput(name="Fred", email=email, password=password))


async def test_logs_in_with_correct_credentials():
    user_repository = FakeUserRepository()
    password_hasher = FakePasswordHasher()
    token_service = FakeTokenService()
    await _register(user_repository, password_hasher, token_service)

    use_case = LoginUserUseCase(user_repository, password_hasher, token_service)
    result = await use_case.execute(LoginUserInput(email="fred@example.com", password="segredo123"))

    assert result.user.email == "fred@example.com"
    assert result.token == f"token-for-{result.user.id}"


async def test_rejects_login_with_wrong_password():
    user_repository = FakeUserRepository()
    password_hasher = FakePasswordHasher()
    token_service = FakeTokenService()
    await _register(user_repository, password_hasher, token_service)

    use_case = LoginUserUseCase(user_repository, password_hasher, token_service)
    with pytest.raises(InvalidCredentialsError):
        await use_case.execute(LoginUserInput(email="fred@example.com", password="senhaErrada"))


async def test_rejects_login_with_unknown_email():
    user_repository = FakeUserRepository()
    use_case = LoginUserUseCase(user_repository, FakePasswordHasher(), FakeTokenService())

    with pytest.raises(InvalidCredentialsError):
        await use_case.execute(LoginUserInput(email="ninguem@example.com", password="qualquer"))
