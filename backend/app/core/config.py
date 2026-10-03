from functools import lru_cache

from pydantic import field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

_PRODUCTION_ENVIRONMENTS = {"production", "prod"}
_MASTER_TOKEN_MIN_LENGTH = 32
_DEFAULT_MASTER_TOKEN_USER_EMAIL = "master@studyou.local"


class Settings(BaseSettings):
    # hide_input_in_errors: erros de configuracao nao devem imprimir segredos (chaves, tokens) no log.
    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore", hide_input_in_errors=True
    )

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

    jwt_secret: str
    jwt_expires_minutes: int = 60 * 24 * 30

    # Token mestre, APENAS para testes: enviado como "Authorization: Bearer <token>",
    # autentica como o usuario master_token_user_email sem precisar de login.
    # Vazio = desativado. A aplicacao nao sobe se ele estiver definido em producao.
    master_token: str = ""
    master_token_user_email: str = _DEFAULT_MASTER_TOKEN_USER_EMAIL

    @field_validator("master_token_user_email", mode="before")
    @classmethod
    def _default_blank_master_token_user_email(cls, value: object) -> object:
        # "MASTER_TOKEN_USER_EMAIL=" vazio no .env usa o padrao, em vez de um usuario sem e-mail.
        if isinstance(value, str) and not value.strip():
            return _DEFAULT_MASTER_TOKEN_USER_EMAIL
        return value

    @model_validator(mode="after")
    def _validate_master_token(self) -> "Settings":
        if not self.master_token:
            return self
        if self.environment.strip().lower() in _PRODUCTION_ENVIRONMENTS:
            raise ValueError("MASTER_TOKEN nao pode ser usado com ENVIRONMENT=production.")
        if len(self.master_token) < _MASTER_TOKEN_MIN_LENGTH:
            raise ValueError(f"MASTER_TOKEN deve ter pelo menos {_MASTER_TOKEN_MIN_LENGTH} caracteres.")
        return self

    @property
    def allowed_file_formats_set(self) -> set[str]:
        return {fmt.strip().lower() for fmt in self.allowed_file_formats.split(",") if fmt.strip()}


@lru_cache
def get_settings() -> Settings:
    return Settings()
