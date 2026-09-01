from app.domain.exceptions import FileTooLargeError
from app.domain.interfaces.file_validator import FileSizeValidator


class MaxSizeFileValidator(FileSizeValidator):
    """Valida se o tamanho do arquivo esta dentro do limite configurado em bytes."""

    def __init__(self, max_size_bytes: int) -> None:
        self._max_size_bytes = max_size_bytes

    def validate(self, filename: str, content: bytes) -> None:
        size_bytes = len(content)
        if size_bytes > self._max_size_bytes:
            raise FileTooLargeError(filename, size_bytes, self._max_size_bytes)
