import logging
import time

from fastapi import FastAPI, Request, Response

logger = logging.getLogger(__name__)


def register_request_logging(app: FastAPI) -> None:
    """Loga cada requisicao (metodo, rota, status e duracao) e qualquer excecao nao tratada."""

    @app.middleware("http")
    async def log_requests(request: Request, call_next) -> Response:
        start = time.perf_counter()
        try:
            response = await call_next(request)
        except Exception:
            duration_ms = (time.perf_counter() - start) * 1000
            logger.exception(
                "%s %s -> 500 (excecao nao tratada) em %.0fms",
                request.method,
                request.url.path,
                duration_ms,
            )
            raise

        duration_ms = (time.perf_counter() - start) * 1000
        level = logging.WARNING if response.status_code >= 400 else logging.INFO
        logger.log(
            level,
            "%s %s -> %d em %.0fms",
            request.method,
            request.url.path,
            response.status_code,
            duration_ms,
        )
        return response
