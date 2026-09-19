from abc import ABC, abstractmethod

from app.domain.entities.analysis import ContentAnalysis


class AIContentAnalyzer(ABC):
    """Porta para o servico de IA de terceiro (Gemini) usado para analisar o conteudo do arquivo."""

    @abstractmethod
    async def analyze(self, filename: str, content: bytes, mime_type: str, prompt: str) -> ContentAnalysis:
        """Etapa 'Analise do conteudo com base no prompt especificado na aplicacao'.

        Deve levantar ContentExceedsContextLimitError quando o conteudo exceder o limite
        de contexto do modelo (etapa 'Conteudo do arquivo esta no limite do contexto da IA?'),
        verificado reativamente a partir da resposta do proprio provedor de IA para evitar
        uma chamada extra apenas para contar tokens.
        """
