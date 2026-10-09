import logging
from dataclasses import dataclass
from uuid import UUID

from app.application.services.crossword_layout import CrosswordLayoutService
from app.application.services.game_uniqueness import enforce_unique_content
from app.domain.entities.analysis import FileAnalysisError, FileAnalysisResult
from app.domain.entities.games import GeneratedGames, GuessWordGame
from app.domain.exceptions import ContentExceedsContextLimitError
from app.domain.interfaces.ai_content_analyzer import AIContentAnalyzer
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
    """Orquestra o fluxo: validar formato -> validar tamanho -> analisar conteudo (a IA
    rejeita se exceder o limite de contexto) -> gerar os jogos a partir da analise ->
    persistir resultado ou erro.

    Depende apenas de abstracoes (DIP), as implementacoes concretas sao
    injetadas pela camada de composicao (api/v1/dependencies.py).
    """

    def __init__(
        self,
        format_validator: FileFormatValidator,
        size_validator: FileSizeValidator,
        ai_analyzer: AIContentAnalyzer,
        game_content_generator: GameContentGenerator,
        crossword_layout_service: CrosswordLayoutService,
        repository: AnalysisRepository,
        analysis_prompt: str,
    ) -> None:
        self._format_validator = format_validator
        self._size_validator = size_validator
        self._ai_analyzer = ai_analyzer
        self._game_content_generator = game_content_generator
        self._crossword_layout_service = crossword_layout_service
        self._repository = repository
        self._analysis_prompt = analysis_prompt

    async def execute(self, data: AnalyzeFileInput) -> FileAnalysisResult:
        logger.info(
            "Iniciando analise do arquivo '%s' (%s, %d bytes)",
            data.filename,
            data.mime_type,
            len(data.content),
        )
        self._format_validator.validate(data.filename, data.mime_type)
        self._size_validator.validate(data.filename, data.content)

        try:
            analysis = await self._ai_analyzer.analyze(
                data.filename, data.content, data.mime_type, self._analysis_prompt
            )
            logger.info(
                "Analise da IA concluida para '%s' em %dms (%d tokens)",
                data.filename,
                analysis.response_time_ms,
                analysis.total_tokens,
            )

            raw_games = enforce_unique_content(
                await self._game_content_generator.generate(data.filename, analysis.text)
            )
            logger.info(
                "Jogos gerados para '%s' em %dms (%d tokens)",
                data.filename,
                raw_games.response_time_ms,
                raw_games.total_tokens,
            )
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
            prompt_used=self._analysis_prompt,
            content=analysis.text,
            games=games,
            total_tokens=analysis.total_tokens + raw_games.total_tokens,
            ai_response_time_ms=analysis.response_time_ms + raw_games.response_time_ms,
        )
        await self._repository.save_result(result)
        logger.info("Resultado da analise persistido para '%s' (id=%s)", data.filename, result.id)
        return result
