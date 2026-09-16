from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "StudYou Backend"
    environment: str = "development"

    database_url: str

    gemini_api_key: str
    gemini_model: str = "gemini-3.8-flash"
    gemini_analysis_prompt: str = (
        "Voce e um assistente de estudos. Resuma o conteudo do arquivo enviado e "
        "extraia os principais topicos, conceitos e possiveis perguntas de revisao."
    )

    max_file_size_bytes: int = 10 * 1024 * 1024
    allowed_file_formats: str = "pdf,txt,docx,pptx,png,jpg,jpeg"

    log_level: str = "INFO"
    # Arquivo de log separado (com rotacao). Vazio = loga apenas no console.
    log_file: str = "logs/studyou.log"
    log_max_bytes: int = 5 * 1024 * 1024
    log_backup_count: int = 5

    @property
    def allowed_file_formats_set(self) -> set[str]:
        return {fmt.strip().lower() for fmt in self.allowed_file_formats.split(",") if fmt.strip()}


@lru_cache
def get_settings() -> Settings:
    return Settings()
