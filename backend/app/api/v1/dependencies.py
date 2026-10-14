import secrets
from functools import lru_cache
from typing import Annotated
from uuid import UUID

from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from app.application.services.crossword_layout import CrosswordLayoutService
from app.application.use_cases.analyze_file import AnalyzeFileUseCase
from app.application.use_cases.get_master_user import GetMasterUserUseCase
from app.application.use_cases.login_user import LoginUserUseCase
from app.application.use_cases.register_user import RegisterUserUseCase
from app.core.config import Settings, get_settings
from app.domain.entities.user import User
from app.domain.exceptions import UnauthorizedError
from app.domain.interfaces.analysis_repository import AnalysisRepository
from app.domain.interfaces.game_attempt_repository import GameAttemptRepository
from app.domain.interfaces.game_content_generator import GameContentGenerator
from app.domain.interfaces.password_hasher import PasswordHasher
from app.domain.interfaces.token_service import TokenService
from app.domain.interfaces.user_repository import UserRepository
from app.infrastructure.ai.gemini_game_generator import GeminiGameContentGenerator
from app.infrastructure.persistence.database import get_db_session
from app.infrastructure.persistence.game_attempt_repository import SqlAlchemyGameAttemptRepository
from app.infrastructure.persistence.repository import SqlAlchemyAnalysisRepository
from app.infrastructure.persistence.user_repository import SqlAlchemyUserRepository
from app.infrastructure.security.bcrypt_password_hasher import BcryptPasswordHasher
from app.infrastructure.security.jwt_token_service import JwtTokenService
from app.infrastructure.validation.format_validator import ExtensionFileFormatValidator
from app.infrastructure.validation.size_validator import MaxSizeFileValidator

_bearer_scheme = HTTPBearer(auto_error=False)


@lru_cache
def get_game_content_generator() -> GameContentGenerator:
    settings = get_settings()
    return GeminiGameContentGenerator(
        api_key=settings.gemini_api_key,
        model=settings.gemini_model,
        thinking_level=settings.gemini_thinking_level,
    )


@lru_cache
def get_crossword_layout_service() -> CrosswordLayoutService:
    return CrosswordLayoutService()


@lru_cache
def get_password_hasher() -> PasswordHasher:
    return BcryptPasswordHasher()


@lru_cache
def get_token_service() -> TokenService:
    settings = get_settings()
    return JwtTokenService(secret=settings.jwt_secret, expires_minutes=settings.jwt_expires_minutes)


def get_user_repository(session: Annotated[AsyncSession, Depends(get_db_session)]) -> UserRepository:
    return SqlAlchemyUserRepository(session)


def get_game_attempt_repository(
    session: Annotated[AsyncSession, Depends(get_db_session)],
) -> GameAttemptRepository:
    return SqlAlchemyGameAttemptRepository(session)


def get_analysis_repository(session: Annotated[AsyncSession, Depends(get_db_session)]) -> AnalysisRepository:
    return SqlAlchemyAnalysisRepository(session)


def get_register_user_use_case(
    user_repository: Annotated[UserRepository, Depends(get_user_repository)],
    password_hasher: Annotated[PasswordHasher, Depends(get_password_hasher)],
    token_service: Annotated[TokenService, Depends(get_token_service)],
) -> RegisterUserUseCase:
    return RegisterUserUseCase(user_repository, password_hasher, token_service)


def get_login_user_use_case(
    user_repository: Annotated[UserRepository, Depends(get_user_repository)],
    password_hasher: Annotated[PasswordHasher, Depends(get_password_hasher)],
    token_service: Annotated[TokenService, Depends(get_token_service)],
) -> LoginUserUseCase:
    return LoginUserUseCase(user_repository, password_hasher, token_service)


def get_master_user_use_case(
    user_repository: Annotated[UserRepository, Depends(get_user_repository)],
    password_hasher: Annotated[PasswordHasher, Depends(get_password_hasher)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> GetMasterUserUseCase:
    return GetMasterUserUseCase(user_repository, password_hasher, settings.master_token_user_email)


def _is_master_token(token: str, settings: Settings) -> bool:
    # compare_digest evita vazar o token por diferenca de tempo na comparacao.
    return bool(settings.master_token) and secrets.compare_digest(
        token.encode("utf-8"), settings.master_token.encode("utf-8")
    )


async def get_current_user(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(_bearer_scheme)],
    token_service: Annotated[TokenService, Depends(get_token_service)],
    user_repository: Annotated[UserRepository, Depends(get_user_repository)],
    settings: Annotated[Settings, Depends(get_settings)],
    master_user_use_case: Annotated[GetMasterUserUseCase, Depends(get_master_user_use_case)],
) -> User:
    if credentials is None:
        raise UnauthorizedError()
    if _is_master_token(credentials.credentials, settings):
        return await master_user_use_case.execute()
    user_id: UUID = token_service.verify(credentials.credentials)
    user = await user_repository.get_by_id(user_id)
    if user is None:
        raise UnauthorizedError()
    return user


def get_analyze_file_use_case(
    session: Annotated[AsyncSession, Depends(get_db_session)],
    settings: Annotated[Settings, Depends(get_settings)],
    game_content_generator: Annotated[GameContentGenerator, Depends(get_game_content_generator)],
    crossword_layout_service: Annotated[CrosswordLayoutService, Depends(get_crossword_layout_service)],
) -> AnalyzeFileUseCase:
    return AnalyzeFileUseCase(
        format_validator=ExtensionFileFormatValidator(settings.allowed_file_formats_set),
        size_validator=MaxSizeFileValidator(settings.max_file_size_bytes),
        game_content_generator=game_content_generator,
        crossword_layout_service=crossword_layout_service,
        repository=SqlAlchemyAnalysisRepository(session),
    )
