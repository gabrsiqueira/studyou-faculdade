from uuid import UUID

from app.domain.entities.analysis import ContentAnalysis, FileAnalysisError, FileAnalysisResult
from app.domain.entities.games import RawGameContent
from app.domain.entities.user import User
from app.domain.exceptions import ContentExceedsContextLimitError, UnauthorizedError
from app.domain.interfaces.ai_content_analyzer import AIContentAnalyzer
from app.domain.interfaces.analysis_repository import AnalysisRepository
from app.domain.interfaces.game_content_generator import GameContentGenerator
from app.domain.interfaces.password_hasher import PasswordHasher
from app.domain.interfaces.token_service import TokenService
from app.domain.interfaces.user_repository import UserRepository


class FakeAIContentAnalyzer(AIContentAnalyzer):
    def __init__(
        self,
        within_limit: bool = True,
        analysis_output: str = "analise gerada",
        total_tokens: int = 0,
        response_time_ms: int = 0,
    ):
        self.within_limit = within_limit
        self.analysis_output = analysis_output
        self.total_tokens = total_tokens
        self.response_time_ms = response_time_ms
        self.analyze_calls: list[tuple[str, bytes, str, str]] = []

    async def analyze(self, filename: str, content: bytes, mime_type: str, prompt: str) -> ContentAnalysis:
        self.analyze_calls.append((filename, content, mime_type, prompt))
        if not self.within_limit:
            raise ContentExceedsContextLimitError(filename)
        return ContentAnalysis(
            text=self.analysis_output, total_tokens=self.total_tokens, response_time_ms=self.response_time_ms
        )


def make_raw_game_content(**overrides) -> RawGameContent:
    defaults = dict(
        quiz=[],
        flashcards=[],
        guess_word_answer="PALAVRA",
        crossword_words=["PALAVRA", "TERMO"],
        word_forming_letters=list("PALAVRA"),
        word_forming_valid_words=["PALAVRA", "PARA"],
    )
    defaults.update(overrides)
    return RawGameContent(**defaults)


class FakeGameContentGenerator(GameContentGenerator):
    def __init__(self, raw_game_content: RawGameContent | None = None):
        self.raw_game_content = raw_game_content or make_raw_game_content()
        self.generate_calls: list[tuple[str, str]] = []

    async def generate(self, filename: str, analysis_text: str) -> RawGameContent:
        self.generate_calls.append((filename, analysis_text))
        return self.raw_game_content


class FakeAnalysisRepository(AnalysisRepository):
    def __init__(self) -> None:
        self.saved_results: list[FileAnalysisResult] = []
        self.saved_errors: list[FileAnalysisError] = []

    async def save_result(self, result: FileAnalysisResult) -> None:
        self.saved_results.append(result)

    async def save_error(self, error: FileAnalysisError) -> None:
        self.saved_errors.append(error)

    async def list_results_for_user(self, user_id: UUID) -> list[FileAnalysisResult]:
        return [result for result in self.saved_results if result.user_id == user_id]


class FakeUserRepository(UserRepository):
    def __init__(self) -> None:
        self.users: list[User] = []

    async def create(self, user: User) -> None:
        self.users.append(user)

    async def get_by_email(self, email: str) -> User | None:
        return next((u for u in self.users if u.email == email), None)

    async def get_by_id(self, user_id: UUID) -> User | None:
        return next((u for u in self.users if u.id == user_id), None)


class FakePasswordHasher(PasswordHasher):
    """Does no real hashing -- just prefixes the password, enough to test behavior."""

    def hash(self, plain_password: str) -> str:
        return f"hashed:{plain_password}"

    def verify(self, plain_password: str, password_hash: str) -> bool:
        return password_hash == f"hashed:{plain_password}"


class FakeTokenService(TokenService):
    def __init__(self) -> None:
        self.issued_for: list[UUID] = []

    def issue(self, user_id: UUID) -> str:
        self.issued_for.append(user_id)
        return f"token-for-{user_id}"

    def verify(self, token: str) -> UUID:
        try:
            return UUID(token.removeprefix("token-for-"))
        except ValueError as error:
            raise UnauthorizedError() from error
