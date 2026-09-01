from abc import ABC, abstractmethod


class FileFormatValidator(ABC):
    """Valida se o formato do arquivo e suportado (etapa 'Suporta formato do arquivo?')."""

    @abstractmethod
    def validate(self, filename: str, mime_type: str) -> None:
        """Deve levantar UnsupportedFileFormatError quando o formato nao for suportado."""


class FileSizeValidator(ABC):
    """Valida se o tamanho do arquivo esta dentro do limite (etapa 'Tamanho do arquivo...')."""

    @abstractmethod
    def validate(self, filename: str, content: bytes) -> None:
        """Deve levantar FileTooLargeError quando o arquivo exceder o limite."""
