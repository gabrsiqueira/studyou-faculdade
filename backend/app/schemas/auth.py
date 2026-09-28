from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, field_validator

from app.application.use_cases.register_user import AuthResult


def _validate_email(value: str) -> str:
    value = value.strip()
    if "@" not in value or value.startswith("@") or value.endswith("@"):
        raise ValueError("E-mail invalido.")
    return value


class RegisterRequest(BaseModel):
    name: str
    email: str
    password: str

    _validate_email = field_validator("email")(_validate_email)


class LoginRequest(BaseModel):
    email: str
    password: str

    _validate_email = field_validator("email")(_validate_email)


class UserResponse(BaseModel):
    id: UUID
    name: str
    email: str
    created_at: datetime


class AuthResponse(BaseModel):
    token: str
    user: UserResponse

    @classmethod
    def from_result(cls, result: AuthResult) -> "AuthResponse":
        return cls(
            token=result.token,
            user=UserResponse(
                id=result.user.id,
                name=result.user.name,
                email=result.user.email,
                created_at=result.user.created_at,
            ),
        )
