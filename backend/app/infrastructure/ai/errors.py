from google.genai.errors import APIError

_FRIENDLY_REASONS = {
    429: "o limite de uso da IA foi atingido. Tente novamente mais tarde.",
    500: "o serviço de IA teve um erro interno. Tente novamente em alguns minutos.",
    503: "o serviço de IA está sobrecarregado no momento. Tente novamente em alguns minutos.",
}


def friendly_reason(error: APIError) -> str:
    """Mensagem curta, em portugues, para mostrar ao usuario no lugar do erro cru do Gemini."""
    return _FRIENDLY_REASONS.get(error.code, f"erro {error.code} do serviço de IA ({error.status}).")
