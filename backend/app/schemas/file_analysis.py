from datetime import datetime
from uuid import UUID

from pydantic import BaseModel

from app.domain.entities.analysis import FileAnalysisResult
from app.schemas.games import GeneratedGamesResponse


class FileAnalysisResponse(BaseModel):
    id: UUID
    filename: str
    mime_type: str
    content: str
    games: GeneratedGamesResponse
    total_tokens: int
    ai_response_time_ms: int
    status: str
    created_at: datetime

    @classmethod
    def from_entity(cls, entity: FileAnalysisResult) -> "FileAnalysisResponse":
        return cls(
            id=entity.id,
            filename=entity.filename,
            mime_type=entity.mime_type,
            content=entity.content,
            games=GeneratedGamesResponse.from_entity(entity.games),
            total_tokens=entity.total_tokens,
            ai_response_time_ms=entity.ai_response_time_ms,
            status=entity.status.value,
            created_at=entity.created_at,
        )
