import logging
from dataclasses import dataclass
from uuid import UUID

from app.application.services.crossword_layout import CrosswordLayoutService
from app.application.services.game_uniqueness import enforce_unique_content
from app.domain.entities.analysis import FileAnalysisError, FileAnalysisResult
from app.domain.entities.games import GeneratedGames, GuessWordGame
from app.domain.exceptions import ContentExceedsContextLimitError
from app.domain.interfaces.analysis_repository import AnalysisRepository
from app.domain.interfaces.file_validator import FileFormatValidator, FileSizeValidator
from app.domain.interfaces.game_content_generator import GameContentGenerator

logger = logging.getLogger(__name__)


@dataclass
class AnalyzeFileInput:
    user_id: UUID
    filename: str
    mime_type: str
    content: bytes


class AnalyzeFileUseCase:
    """Orquestra o fluxo: validar formato -> validar tamanho -> enviar o arquivo a IA
    junto com o prompt de geracao (a IA rejeita se exceder o limite de contexto) ->
    validar as atividades geradas -> persistir resultado ou erro.

    Depende apenas de abstracoes (DIP), as implementacoes concretas sao
    injetadas pela camada de composicao (api/v1/dependencies.py).
    """

    def __init__(
        self,
        format_validator: FileFormatValidator,
        size_validator: FileSizeValidator,
        game_content_generator: GameContentGenerator,
        crossword_layout_service: CrosswordLayoutService,
        repository: AnalysisRepository,
    ) -> None:
        self._format_validator = format_validator
        self._size_validator = size_validator
        self._game_content_generator = game_content_generator
        self._crossword_layout_service = crossword_layout_service
        self._repository = repository

    async def execute(self, data: AnalyzeFileInput) -> FileAnalysisResult:
        logger.info(
            "Iniciando geracao das atividades do arquivo '%s' (%s, %d bytes)",
            data.filename,
            data.mime_type,
            len(data.content),
        )
        self._format_validator.validate(data.filename, data.mime_type)
        self._size_validator.validate(data.filename, data.content)

        try:
            generated = await self._game_content_generator.generate(data.filename, data.content, data.mime_type)
        except ContentExceedsContextLimitError:
            error = FileAnalysisError(
                user_id=data.user_id,
                filename=data.filename,
                mime_type=data.mime_type,
                reason="Conteúdo do arquivo excede o limite de contexto do modelo de IA.",
            )
            await self._repository.save_error(error)
            logger.info("Erro de limite de contexto persistido para '%s' (id=%s)", data.filename, error.id)
            raise
        logger.info(
            "Jogos gerados para '%s' em %dms (%d tokens)",
            data.filename,
            generated.response_time_ms,
            generated.total_tokens,
        )

        raw_games = enforce_unique_content(generated)
        games = GeneratedGames(
            quiz=raw_games.quiz,
            flashcards=raw_games.flashcards,
            guess_word=GuessWordGame(answer=raw_games.guess_word_answer),
            crossword=self._crossword_layout_service.build(raw_games.crossword_words, raw_games.crossword_clues),
        )

        result = FileAnalysisResult(
            user_id=data.user_id,
            filename=data.filename,
            mime_type=data.mime_type,
            prompt_used=raw_games.prompt_used,
            content=raw_games.raw_response,
            games=games,
            total_tokens=raw_games.total_tokens,
            ai_response_time_ms=raw_games.response_time_ms,
        )
        await self._repository.save_result(result)
        logger.info("Resultado persistido para '%s' (id=%s)", data.filename, result.id)
        return result
