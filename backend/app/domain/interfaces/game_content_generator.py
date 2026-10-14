from abc import ABC, abstractmethod

from app.domain.entities.games import RawGameContent


class GameContentGenerator(ABC):
    """Porta para gerar o conteudo bruto dos jogos (perguntas, palavras-chave) a partir
    do arquivo de estudo enviado pelo usuario. O posicionamento geometrico das palavras
    cruzadas nao e responsabilidade desta porta -- fica a cargo de CrosswordLayoutService."""

    @abstractmethod
    async def generate(self, filename: str, content: bytes, mime_type: str) -> RawGameContent:
        """Envia o arquivo ao servico de IA junto com o prompt de geracao, em uma unica
        requisicao, e devolve as atividades geradas.

        Deve levantar ContentExceedsContextLimitError quando o conteudo exceder o limite
        de contexto do modelo (verificado reativamente a partir da resposta do proprio
        provedor de IA, sem uma chamada extra apenas para contar tokens) e AIServiceError
        se o servico de IA falhar.
        """
