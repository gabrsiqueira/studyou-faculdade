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
