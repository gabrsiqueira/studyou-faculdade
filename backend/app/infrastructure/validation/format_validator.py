from app.domain.exceptions import UnsupportedFileFormatError
from app.domain.interfaces.file_validator import FileFormatValidator


class ExtensionFileFormatValidator(FileFormatValidator):
    """Valida o formato pelo sufixo do nome do arquivo contra uma lista configurada."""

    def __init__(self, allowed_formats: set[str]) -> None:
        self._allowed_formats = allowed_formats

    def validate(self, filename: str, mime_type: str) -> None:
        extension = filename.rsplit(".", maxsplit=1)[-1].lower() if "." in filename else ""
        if extension not in self._allowed_formats:
            raise UnsupportedFileFormatError(filename, self._allowed_formats)
