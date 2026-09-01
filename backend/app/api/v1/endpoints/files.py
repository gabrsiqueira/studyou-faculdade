import mimetypes
from typing import Annotated

from fastapi import APIRouter, Depends, UploadFile, status

from app.api.v1.dependencies import get_analyze_file_use_case
from app.application.use_cases.analyze_file import AnalyzeFileInput, AnalyzeFileUseCase
from app.schemas.error import ErrorResponse
from app.schemas.file_analysis import FileAnalysisResponse

router = APIRouter(prefix="/files", tags=["files"])


def _resolve_mime_type(file: UploadFile) -> str:
    if file.content_type and file.content_type != "application/octet-stream":
        return file.content_type
    guessed, _ = mimetypes.guess_type(file.filename or "")
    return guessed or "application/octet-stream"


@router.post(
    "/analyze",
    response_model=FileAnalysisResponse,
    status_code=status.HTTP_201_CREATED,
    responses={
        413: {"model": ErrorResponse, "description": "Arquivo excede o tamanho maximo permitido"},
        415: {"model": ErrorResponse, "description": "Formato de arquivo nao suportado"},
        422: {"model": ErrorResponse, "description": "Conteudo do arquivo excede o limite de contexto da IA"},
        502: {"model": ErrorResponse, "description": "Falha ao comunicar com o servico de IA"},
    },
)
async def analyze_file(
    use_case: Annotated[AnalyzeFileUseCase, Depends(get_analyze_file_use_case)],
    file: UploadFile,
) -> FileAnalysisResponse:
    content = await file.read()
    result = await use_case.execute(
        AnalyzeFileInput(
            filename=file.filename or "arquivo",
            mime_type=_resolve_mime_type(file),
            content=content,
        )
    )
    return FileAnalysisResponse.from_entity(result)
