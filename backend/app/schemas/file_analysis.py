from datetime import datetime
from uuid import UUID

from pydantic import BaseModel

from app.domain.entities.analysis import FileAnalysisResult


class FileAnalysisResponse(BaseModel):
    id: UUID
    filename: str
    mime_type: str
    content: str
    status: str
    created_at: datetime

    @classmethod
    def from_entity(cls, entity: FileAnalysisResult) -> "FileAnalysisResponse":
        return cls(
            id=entity.id,
            filename=entity.filename,
            mime_type=entity.mime_type,
            content=entity.content,
            status=entity.status.value,
            created_at=entity.created_at,
        )
