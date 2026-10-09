from abc import ABC, abstractmethod

from app.domain.entities.games import RawGameContent


class GameContentGenerator(ABC):
    """Porta para gerar o conteudo bruto dos jogos (perguntas, palavras-chave) a partir
    do texto ja analisado de um arquivo. O posicionamento geometrico das palavras cruzadas nao
    e responsabilidade desta porta -- fica a cargo de CrosswordLayoutService."""

    @abstractmethod
    async def generate(self, filename: str, analysis_text: str) -> RawGameContent:
        """Deve levantar AIServiceError se o servico de IA falhar."""
