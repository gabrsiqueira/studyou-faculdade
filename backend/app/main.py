import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.error_handlers import register_error_handlers
from app.api.middleware import register_request_logging
from app.api.v1.router import api_router
from app.core.config import get_settings
from app.core.logging import setup_logging
from app.infrastructure.persistence.database import init_session_factory

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    init_session_factory(settings.database_url)
    yield


def create_app() -> FastAPI:
    settings = get_settings()
    setup_logging(
        level=settings.log_level,
        log_file=settings.log_file,
        max_bytes=settings.log_max_bytes,
        backup_count=settings.log_backup_count,
    )
    if settings.master_token:
        logger.warning(
            "MASTER_TOKEN ativo: requisicoes com esse token autenticam como '%s' sem login. "
            "Use apenas em testes.",
            settings.master_token_user_email,
        )

    app = FastAPI(title=settings.app_name, lifespan=lifespan)

    register_error_handlers(app)
    register_request_logging(app)
    app.include_router(api_router)

    return app


app = create_app()
