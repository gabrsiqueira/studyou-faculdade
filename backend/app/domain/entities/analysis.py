from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from uuid import UUID, uuid4


class FileAnalysisStatus(str, Enum):
    COMPLETED = "completed"
    CONTEXT_LIMIT_EXCEEDED = "context_limit_exceeded"


@dataclass(frozen=True, slots=True)
class FileAnalysisResult:
    """Resultado persistido quando a IA consegue analisar o conteudo do arquivo."""

    filename: str
    mime_type: str
    prompt_used: str
    content: str
    id: UUID = field(default_factory=uuid4)
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    status: FileAnalysisStatus = FileAnalysisStatus.COMPLETED


@dataclass(frozen=True, slots=True)
class FileAnalysisError:
    """Erro persistido referente ao arquivo (ex.: conteudo fora do limite de contexto da IA)."""

    filename: str
    mime_type: str
    reason: str
    id: UUID = field(default_factory=uuid4)
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    status: FileAnalysisStatus = FileAnalysisStatus.CONTEXT_LIMIT_EXCEEDED
