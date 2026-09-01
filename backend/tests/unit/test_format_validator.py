import pytest

from app.domain.exceptions import UnsupportedFileFormatError
from app.infrastructure.validation.format_validator import ExtensionFileFormatValidator


def test_accepts_allowed_extension():
    validator = ExtensionFileFormatValidator({"pdf", "txt"})
    validator.validate("notas.pdf", "application/pdf")


def test_rejects_disallowed_extension():
    validator = ExtensionFileFormatValidator({"pdf", "txt"})
    with pytest.raises(UnsupportedFileFormatError):
        validator.validate("video.mp4", "video/mp4")


def test_rejects_missing_extension():
    validator = ExtensionFileFormatValidator({"pdf"})
    with pytest.raises(UnsupportedFileFormatError):
        validator.validate("arquivo-sem-extensao", "application/octet-stream")


def test_validation_is_case_insensitive():
    validator = ExtensionFileFormatValidator({"pdf"})
    validator.validate("notas.PDF", "application/pdf")
