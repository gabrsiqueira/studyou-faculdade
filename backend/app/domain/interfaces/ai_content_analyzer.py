from abc import ABC, abstractmethod


class AIContentAnalyzer(ABC):
    """Porta para o servico de IA de terceiro (Gemini) usado para analisar o conteudo do arquivo."""

    @abstractmethod
    async def is_within_context_limit(self, filename: str, content: bytes, mime_type: str) -> bool:
        """Etapa 'Conteudo do arquivo esta no limite do contexto da IA?'."""

    @abstractmethod
    async def analyze(self, filename: str, content: bytes, mime_type: str, prompt: str) -> str:
        """Etapa 'Analise do conteudo com base no prompt especificado na aplicacao'."""
