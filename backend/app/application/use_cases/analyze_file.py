from dataclasses import dataclass

from app.domain.entities.analysis import FileAnalysisError, FileAnalysisResult
from app.domain.exceptions import ContentExceedsContextLimitError
from app.domain.interfaces.ai_content_analyzer import AIContentAnalyzer
from app.domain.interfaces.analysis_repository import AnalysisRepository
from app.domain.interfaces.file_validator import FileFormatValidator, FileSizeValidator


@dataclass
class AnalyzeFileInput:
    filename: str
    mime_type: str
    content: bytes


class AnalyzeFileUseCase:
    """Orquestra o fluxo: validar formato -> validar tamanho -> checar limite de
    contexto da IA -> analisar conteudo -> persistir resultado ou erro.

    Depende apenas de abstracoes (DIP), as implementacoes concretas sao
    injetadas pela camada de composicao (api/v1/dependencies.py).
    """

    def __init__(
        self,
        format_validator: FileFormatValidator,
        size_validator: FileSizeValidator,
        ai_analyzer: AIContentAnalyzer,
        repository: AnalysisRepository,
        analysis_prompt: str,
    ) -> None:
        self._format_validator = format_validator
        self._size_validator = size_validator
        self._ai_analyzer = ai_analyzer
        self._repository = repository
        self._analysis_prompt = analysis_prompt

    async def execute(self, data: AnalyzeFileInput) -> FileAnalysisResult:
        self._format_validator.validate(data.filename, data.mime_type)
        self._size_validator.validate(data.filename, data.content)

        within_context_limit = await self._ai_analyzer.is_within_context_limit(
            data.filename, data.content, data.mime_type
        )
        if not within_context_limit:
            error = FileAnalysisError(
                filename=data.filename,
                mime_type=data.mime_type,
                reason="Conteudo do arquivo excede o limite de contexto do modelo de IA.",
            )
            await self._repository.save_error(error)
            raise ContentExceedsContextLimitError(data.filename)

        analyzed_content = await self._ai_analyzer.analyze(
            data.filename, data.content, data.mime_type, self._analysis_prompt
        )

        result = FileAnalysisResult(
            filename=data.filename,
            mime_type=data.mime_type,
            prompt_used=self._analysis_prompt,
            content=analyzed_content,
        )
        await self._repository.save_result(result)
        return result
