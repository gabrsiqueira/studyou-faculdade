from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.error_handlers import register_error_handlers
from app.api.v1.router import api_router
from app.core.config import get_settings
from app.infrastructure.persistence.database import init_session_factory


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    init_session_factory(settings.database_url)
    yield


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(title=settings.app_name, lifespan=lifespan)

    register_error_handlers(app)
    app.include_router(api_router)

    return app


app = create_app()
