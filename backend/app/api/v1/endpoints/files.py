import mimetypes
from typing import Annotated

from fastapi import APIRouter, Depends, UploadFile, status

from app.api.v1.dependencies import get_analysis_repository, get_analyze_file_use_case, get_current_user
from app.application.use_cases.analyze_file import AnalyzeFileInput, AnalyzeFileUseCase
from app.domain.entities.user import User
from app.domain.interfaces.analysis_repository import AnalysisRepository
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
        401: {"model": ErrorResponse, "description": "Nao autenticado"},
        413: {"model": ErrorResponse, "description": "Arquivo excede o tamanho maximo permitido"},
        415: {"model": ErrorResponse, "description": "Formato de arquivo nao suportado"},
        422: {"model": ErrorResponse, "description": "Conteudo do arquivo excede o limite de contexto da IA"},
        502: {"model": ErrorResponse, "description": "Falha ao comunicar com o servico de IA"},
    },
)
async def analyze_file(
    current_user: Annotated[User, Depends(get_current_user)],
    use_case: Annotated[AnalyzeFileUseCase, Depends(get_analyze_file_use_case)],
    file: UploadFile,
) -> FileAnalysisResponse:
    content = await file.read()
    result = await use_case.execute(
        AnalyzeFileInput(
            user_id=current_user.id,
            filename=file.filename or "arquivo",
            mime_type=_resolve_mime_type(file),
            content=content,
        )
    )
    return FileAnalysisResponse.from_entity(result)


@router.get("", response_model=list[FileAnalysisResponse])
async def list_files(
    current_user: Annotated[User, Depends(get_current_user)],
    repository: Annotated[AnalysisRepository, Depends(get_analysis_repository)],
) -> list[FileAnalysisResponse]:
    results = await repository.list_results_for_user(current_user.id)
    return [FileAnalysisResponse.from_entity(result) for result in results]
