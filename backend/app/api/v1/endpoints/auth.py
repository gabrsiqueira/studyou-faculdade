from typing import Annotated

from fastapi import APIRouter, Depends, status

from app.api.v1.dependencies import get_login_user_use_case, get_register_user_use_case
from app.application.use_cases.login_user import LoginUserInput, LoginUserUseCase
from app.application.use_cases.register_user import RegisterUserInput, RegisterUserUseCase
from app.schemas.auth import AuthResponse, LoginRequest, RegisterRequest

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
async def register(
    request: RegisterRequest,
    use_case: Annotated[RegisterUserUseCase, Depends(get_register_user_use_case)],
) -> AuthResponse:
    result = await use_case.execute(RegisterUserInput(name=request.name, email=request.email, password=request.password))
    return AuthResponse.from_result(result)


@router.post("/login", response_model=AuthResponse)
async def login(
    request: LoginRequest,
    use_case: Annotated[LoginUserUseCase, Depends(get_login_user_use_case)],
) -> AuthResponse:
    result = await use_case.execute(LoginUserInput(email=request.email, password=request.password))
    return AuthResponse.from_result(result)
