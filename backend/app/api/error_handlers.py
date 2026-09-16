import logging

from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse

from app.domain.exceptions import (
    AIServiceError,
    ContentExceedsContextLimitError,
    FileTooLargeError,
    UnsupportedFileFormatError,
)

logger = logging.getLogger(__name__)


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(UnsupportedFileFormatError)
    async def handle_unsupported_format(_: Request, exc: UnsupportedFileFormatError) -> JSONResponse:
        logger.warning("Formato nao suportado: %s", exc)
        return JSONResponse(status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE, content={"detail": str(exc)})

    @app.exception_handler(FileTooLargeError)
    async def handle_file_too_large(_: Request, exc: FileTooLargeError) -> JSONResponse:
        logger.warning("Arquivo grande demais: %s", exc)
        return JSONResponse(status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, content={"detail": str(exc)})

    @app.exception_handler(ContentExceedsContextLimitError)
    async def handle_context_limit_exceeded(
        _: Request, exc: ContentExceedsContextLimitError
    ) -> JSONResponse:
        logger.warning("Limite de contexto da IA excedido: %s", exc)
        return JSONResponse(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, content={"detail": str(exc)})

    @app.exception_handler(AIServiceError)
    async def handle_ai_service_error(_: Request, exc: AIServiceError) -> JSONResponse:
        # exc_info inclui a excecao original do Gemini (encadeada via "from error") no log.
        logger.error("Falha no servico de IA: %s", exc, exc_info=exc)
        return JSONResponse(status_code=status.HTTP_502_BAD_GATEWAY, content={"detail": str(exc)})
