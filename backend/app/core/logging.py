import logging
from logging.handlers import RotatingFileHandler
from pathlib import Path

# Logger raiz da aplicacao: todos os modulos usam logging.getLogger(__name__), que fica
# abaixo de "app" e herda os handlers configurados aqui. Os loggers do uvicorn continuam
# com a configuracao propria deles (console).
APP_LOGGER_NAME = "app"

_LOG_FORMAT = "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s"


def setup_logging(level: str, log_file: str, max_bytes: int, backup_count: int) -> None:
    """Configura o log da aplicacao: sempre no console e, se `log_file` for informado,
    tambem em um arquivo separado com rotacao por tamanho.

    Idempotente: pode ser chamada mais de uma vez (ex.: reload do uvicorn) sem duplicar handlers.
    """
    formatter = logging.Formatter(_LOG_FORMAT)

    app_logger = logging.getLogger(APP_LOGGER_NAME)
    app_logger.setLevel(level.upper())
    app_logger.propagate = False

    for handler in list(app_logger.handlers):
        app_logger.removeHandler(handler)
        handler.close()

    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    app_logger.addHandler(console_handler)

    if log_file:
        log_path = Path(log_file)
        log_path.parent.mkdir(parents=True, exist_ok=True)
        file_handler = RotatingFileHandler(
            log_path, maxBytes=max_bytes, backupCount=backup_count, encoding="utf-8"
        )
        file_handler.setFormatter(formatter)
        app_logger.addHandler(file_handler)
