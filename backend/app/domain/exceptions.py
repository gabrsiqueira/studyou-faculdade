class FileAnalysisDomainError(Exception):
    """Classe base para erros de negocio do fluxo de analise de arquivos."""


class UnsupportedFileFormatError(FileAnalysisDomainError):
    """Levantado quando o formato do arquivo nao e suportado pela aplicacao."""

    def __init__(self, filename: str, allowed_formats: set[str]):
        self.filename = filename
        self.allowed_formats = allowed_formats
        super().__init__(
            f"Formato do arquivo '{filename}' nao suportado. "
            f"Formatos aceitos: {', '.join(sorted(allowed_formats))}."
        )


class FileTooLargeError(FileAnalysisDomainError):
    """Levantado quando o arquivo excede o tamanho maximo permitido."""

    def __init__(self, filename: str, size_bytes: int, max_size_bytes: int):
        self.filename = filename
        self.size_bytes = size_bytes
        self.max_size_bytes = max_size_bytes
        super().__init__(
            f"Arquivo '{filename}' ({size_bytes} bytes) excede o limite de {max_size_bytes} bytes."
        )


class ContentExceedsContextLimitError(FileAnalysisDomainError):
    """Levantado quando o conteudo do arquivo excede o limite de contexto do modelo de IA."""

    def __init__(self, filename: str):
        self.filename = filename
        super().__init__(
            f"Conteudo do arquivo '{filename}' excede o limite de contexto do modelo de IA."
        )


class AIServiceError(FileAnalysisDomainError):
    """Levantado quando o servico de IA de terceiro (Gemini) falha ou retorna um erro."""

    def __init__(self, filename: str, reason: str):
        self.filename = filename
        self.reason = reason
        super().__init__(
            f"Falha ao analisar o arquivo '{filename}' com o servico de IA: {reason}"
        )


class AuthDomainError(Exception):
    """Classe base para erros de negocio do fluxo de autenticacao."""


class EmailAlreadyRegisteredError(AuthDomainError):
    """Levantado ao tentar cadastrar um e-mail que ja possui conta."""

    def __init__(self, email: str):
        self.email = email
        super().__init__(f"Ja existe uma conta com o e-mail '{email}'.")


class InvalidCredentialsError(AuthDomainError):
    """Levantado quando o e-mail ou a senha informados no login estao incorretos."""

    def __init__(self) -> None:
        super().__init__("E-mail ou senha invalidos.")


class UnauthorizedError(AuthDomainError):
    """Levantado quando a requisicao nao tem um token de autenticacao valido."""

    def __init__(self) -> None:
        super().__init__("Nao autenticado. Faca login novamente.")
