from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from uuid import UUID, uuid4

from app.domain.entities.games import GeneratedGames


class FileAnalysisStatus(str, Enum):
    COMPLETED = "completed"
    CONTEXT_LIMIT_EXCEEDED = "context_limit_exceeded"


@dataclass(frozen=True, slots=True)
class FileAnalysisResult:
    """Resultado persistido quando a IA consegue gerar as atividades a partir do arquivo."""

    user_id: UUID
    filename: str
    mime_type: str
    # Prompt enviado a IA junto com o arquivo.
    prompt_used: str
    # JSON bruto devolvido pela IA com as atividades.
    content: str
    games: GeneratedGames
    # Tokens (entrada + saida) gastos na requisicao a IA para este arquivo.
    total_tokens: int = 0
    # Tempo de resposta (ms) da requisicao a IA para este arquivo, medido no backend.
    ai_response_time_ms: int = 0
    id: UUID = field(default_factory=uuid4)
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    status: FileAnalysisStatus = FileAnalysisStatus.COMPLETED


@dataclass(frozen=True, slots=True)
class FileAnalysisError:
    """Erro persistido referente ao arquivo (ex.: conteudo fora do limite de contexto da IA)."""

    user_id: UUID
    filename: str
    mime_type: str
    reason: str
    id: UUID = field(default_factory=uuid4)
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    status: FileAnalysisStatus = FileAnalysisStatus.CONTEXT_LIMIT_EXCEEDED
