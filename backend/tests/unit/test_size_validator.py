import pytest

from app.domain.exceptions import FileTooLargeError
from app.infrastructure.validation.size_validator import MaxSizeFileValidator


def test_accepts_file_within_limit():
    validator = MaxSizeFileValidator(max_size_bytes=10)
    validator.validate("notas.txt", b"0123456789")


def test_rejects_file_above_limit():
    validator = MaxSizeFileValidator(max_size_bytes=5)
    with pytest.raises(FileTooLargeError):
        validator.validate("notas.txt", b"0123456789")
