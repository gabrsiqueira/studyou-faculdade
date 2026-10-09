import io
import logging
import time

from google import genai
from google.genai import types
from google.genai.errors import APIError, ClientError
from pptx import Presentation
from docx import Document

from app.domain.entities.analysis import ContentAnalysis
from app.domain.exceptions import AIServiceError, ContentExceedsContextLimitError
from app.domain.interfaces.ai_content_analyzer import AIContentAnalyzer
from app.infrastructure.ai.thinking import thinking_config_for
from app.infrastructure.ai.errors import friendly_reason

logger = logging.getLogger(__name__)

_DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
_PPTX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation"

# Substrings observadas na mensagem de erro do Gemini quando a requisicao e rejeitada
# por exceder o limite de contexto do modelo (ao inves de outro tipo de erro 4xx).
_CONTEXT_LIMIT_ERROR_HINTS = ("token", "context length", "context window", "too long", "too large")


def _extract_docx_text(content: bytes) -> str:
    document = Document(io.BytesIO(content))
    return "\n".join(paragraph.text for paragraph in document.paragraphs)


def _extract_pptx_text(content: bytes) -> str:
    presentation = Presentation(io.BytesIO(content))
    lines = []
    for slide in presentation.slides:
        for shape in slide.shapes:
            if shape.has_text_frame:
                lines.append(shape.text_frame.text)
    return "\n".join(lines)


def _as_gemini_content(content: bytes, mime_type: str) -> tuple[bytes, str]:
    """Gemini understands PDFs, images and plain text directly, but not raw Office XML
    documents, so DOCX/PPTX are converted to plain text here before being sent."""
    if mime_type == _DOCX_MIME_TYPE:
        return _extract_docx_text(content).encode("utf-8"), "text/plain"
    if mime_type == _PPTX_MIME_TYPE:
        return _extract_pptx_text(content).encode("utf-8"), "text/plain"
    return content, mime_type


def _is_context_limit_error(error: APIError) -> bool:
    if not isinstance(error, ClientError):
        return False
    message = str(error).lower()
    return any(hint in message for hint in _CONTEXT_LIMIT_ERROR_HINTS)


class GeminiContentAnalyzer(AIContentAnalyzer):
    """Implementacao de AIContentAnalyzer usando o Gemini (Google GenAI) como servico de terceiro.

    Faz uma unica chamada (generate_content) por arquivo: o limite de contexto e checado
    reativamente a partir do erro que o proprio Gemini retorna quando o conteudo e grande
    demais, em vez de uma chamada extra (count_tokens) soh para essa verificacao.
    """

    def __init__(self, api_key: str, model: str, thinking_level: str = "") -> None:
        self._client = genai.Client(api_key=api_key)
        self._model = model
        self._thinking_config = thinking_config_for(thinking_level)

    async def analyze(self, filename: str, content: bytes, mime_type: str, prompt: str) -> ContentAnalysis:
        gemini_content, gemini_mime_type = _as_gemini_content(content, mime_type)
        logger.debug(
            "Enviando '%s' ao Gemini (modelo=%s, mime=%s, %d bytes)",
            filename,
            self._model,
            gemini_mime_type,
            len(gemini_content),
        )
        start = time.perf_counter()
        try:
            response = await self._client.aio.models.generate_content(
                model=self._model,
                contents=[
                    types.Part.from_bytes(data=gemini_content, mime_type=gemini_mime_type),
                    prompt,
                ],
                config=types.GenerateContentConfig(thinking_config=self._thinking_config),
            )
        except APIError as error:
            if _is_context_limit_error(error):
                raise ContentExceedsContextLimitError(filename) from error
            raise AIServiceError(filename, friendly_reason(error)) from error
        response_time_ms = round((time.perf_counter() - start) * 1000)

        usage = response.usage_metadata
        return ContentAnalysis(
            text=response.text or "",
            total_tokens=(usage.total_token_count or 0) if usage else 0,
            response_time_ms=response_time_ms,
        )
