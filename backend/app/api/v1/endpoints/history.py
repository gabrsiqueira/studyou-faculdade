from typing import Annotated
from uuid import uuid4

from fastapi import APIRouter, Depends, status

from app.api.v1.dependencies import get_current_user, get_game_attempt_repository
from app.domain.entities.game_attempt import GameAttempt
from app.domain.entities.user import User
from app.domain.interfaces.game_attempt_repository import GameAttemptRepository
from app.schemas.history import GameAttemptResponse, SaveGameAttemptRequest

router = APIRouter(prefix="/history", tags=["history"])


@router.post("/attempts", response_model=GameAttemptResponse, status_code=status.HTTP_201_CREATED)
async def save_attempt(
    request: SaveGameAttemptRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    repository: Annotated[GameAttemptRepository, Depends(get_game_attempt_repository)],
) -> GameAttemptResponse:
    attempt = GameAttempt(
        id=uuid4(),
        user_id=current_user.id,
        game_type=request.game_type,
        file_title=request.file_title,
        score_label=request.score_label,
        xp_earned=request.xp_earned,
        best_score_label=request.best_score_label,
    )
    await repository.save(attempt)
    return GameAttemptResponse.from_entity(attempt)


@router.get("/attempts", response_model=list[GameAttemptResponse])
async def list_attempts(
    current_user: Annotated[User, Depends(get_current_user)],
    repository: Annotated[GameAttemptRepository, Depends(get_game_attempt_repository)],
) -> list[GameAttemptResponse]:
    attempts = await repository.list_for_user(current_user.id)
    return [GameAttemptResponse.from_entity(attempt) for attempt in attempts]
