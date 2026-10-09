from google.genai import types


def thinking_config_for(level: str) -> types.ThinkingConfig | None:
    """Converte o nivel configurado (ex.: "LOW") no ThinkingConfig do Gemini; vazio usa o
    padrao do modelo."""
    level = level.strip().upper()
    if not level:
        return None
    return types.ThinkingConfig(thinking_level=types.ThinkingLevel(level))
