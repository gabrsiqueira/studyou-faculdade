import io

from google import genai
from google.genai import types
from google.genai.errors import APIError
from pptx import Presentation
from docx import Document

from app.domain.exceptions import AIServiceError
from app.domain.interfaces.ai_content_analyzer import AIContentAnalyzer

_DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
_PPTX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation"


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


class GeminiContentAnalyzer(AIContentAnalyzer):
    """Implementacao de AIContentAnalyzer usando o Gemini (Google GenAI) como servico de terceiro."""

    def __init__(self, api_key: str, model: str, max_context_tokens: int) -> None:
        self._client = genai.Client(api_key=api_key)
        self._model = model
        self._max_context_tokens = max_context_tokens

    async def is_within_context_limit(self, filename: str, content: bytes, mime_type: str) -> bool:
        gemini_content, gemini_mime_type = _as_gemini_content(content, mime_type)
        try:
            response = await self._client.aio.models.count_tokens(
                model=self._model,
                contents=[types.Part.from_bytes(data=gemini_content, mime_type=gemini_mime_type)],
            )
        except APIError as error:
            raise AIServiceError(filename, str(error)) from error
        return (response.total_tokens or 0) <= self._max_context_tokens

    async def analyze(self, filename: str, content: bytes, mime_type: str, prompt: str) -> str:
        gemini_content, gemini_mime_type = _as_gemini_content(content, mime_type)
        try:
            response = await self._client.aio.models.generate_content(
                model=self._model,
                contents=[
                    types.Part.from_bytes(data=gemini_content, mime_type=gemini_mime_type),
                    prompt,
                ],
            )
        except APIError as error:
            raise AIServiceError(filename, str(error)) from error
        return response.text or ""
