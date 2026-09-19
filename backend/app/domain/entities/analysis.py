from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from uuid import UUID, uuid4

from app.domain.entities.games import GeneratedGames


class FileAnalysisStatus(str, Enum):
    COMPLETED = "completed"
    CONTEXT_LIMIT_EXCEEDED = "context_limit_exceeded"


@dataclass(frozen=True, slots=True)
class ContentAnalysis:
    """Texto produzido pela IA na analise do arquivo e os tokens gastos nessa chamada."""

    text: str
    total_tokens: int = 0


@dataclass(frozen=True, slots=True)
class FileAnalysisResult:
    """Resultado persistido quando a IA consegue analisar o conteudo do arquivo."""

    filename: str
    mime_type: str
    prompt_used: str
    content: str
    games: GeneratedGames
    # Soma dos tokens (entrada + saida) gastos em todas as chamadas a IA para este arquivo.
    total_tokens: int = 0
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
