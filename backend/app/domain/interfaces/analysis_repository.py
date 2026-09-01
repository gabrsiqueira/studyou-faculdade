from abc import ABC, abstractmethod

from app.domain.entities.analysis import FileAnalysisError, FileAnalysisResult


class AnalysisRepository(ABC):
    """Porta de persistencia: 'Persiste dados do conteudo do arquivo' / 'Persiste erro referente ao arquivo'."""

    @abstractmethod
    async def save_result(self, result: FileAnalysisResult) -> None: ...

    @abstractmethod
    async def save_error(self, error: FileAnalysisError) -> None: ...
