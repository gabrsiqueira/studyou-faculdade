# StudYou Backend

Backend em **Python + FastAPI** responsável por receber arquivos de estudo
enviados pelo app mobile, validar formato/tamanho, enviar o conteúdo para o
**Gemini** (serviço de IA de terceiro) e persistir o resultado da análise (ou
o erro) no banco de dados.

## Fluxo implementado (ver diagrama de arquitetura)

1. Mobile envia o arquivo via API (`POST /api/v1/files/analyze`).
2. Backend valida o **formato** do arquivo → `415` se não suportado.
3. Backend valida o **tamanho** do arquivo → `413` se exceder o limite.
4. Backend envia o conteúdo para o modelo de IA (Gemini) e verifica se está
   dentro do **limite de contexto**:
   - Não está no limite → persiste o erro no banco e retorna `422`.
   - Está no limite → a IA analisa o conteúdo com base no **prompt
     especificado na aplicação**, o resultado é persistido no banco e
     retornado ao mobile com `201`.

## Arquitetura

Camadas no estilo Clean Architecture, aplicando SOLID (em especial
Dependency Inversion: as camadas internas dependem apenas de abstrações):

```
app/
  domain/            # Entidades, exceções de negócio e interfaces (ports) — sem dependência de framework
    entities/
    interfaces/
    exceptions.py
  application/        # Casos de uso — orquestram o fluxo usando as abstrações do domínio
    use_cases/
  infrastructure/      # Implementações concretas (adapters) das interfaces do domínio
    ai/                # GeminiContentAnalyzer (implementa AIContentAnalyzer)
    validation/        # Validadores de formato/tamanho
    persistence/        # SQLAlchemy: models, engine/session, repository
  api/                 # Camada de apresentação (FastAPI): routers, DI, exception handlers
    v1/
      endpoints/
      dependencies.py  # Composição: injeta as implementações concretas nos casos de uso
    error_handlers.py  # Mapeia exceções de domínio para status HTTP (413/415/422)
  schemas/             # DTOs Pydantic de request/response
  core/
    config.py          # Settings (pydantic-settings)
  main.py              # Monta a aplicação FastAPI
```

Cada classe tem uma única responsabilidade (SRP): validação de formato,
validação de tamanho, análise via IA e persistência são componentes
independentes, substituíveis sem alterar o caso de uso (`AnalyzeFileUseCase`),
que depende apenas das interfaces definidas em `domain/interfaces`.

## Rodando localmente

### Pré-requisitos
- Python 3.12+
- Docker (para o Postgres, opcional se já tiver um Postgres local)
- Uma `GEMINI_API_KEY` (https://aistudio.google.com/apikey)

### Setup

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
cp .env.example .env
# edite .env e preencha GEMINI_API_KEY
```

### Subindo o banco de dados

```bash
docker compose up -d db
```

### Rodando as migrations (Alembic)

```bash
alembic upgrade head
```

### Subindo a API

```bash
uvicorn app.main:app --reload
```

A API sobe em `http://localhost:8000`. Docs interativas em
`http://localhost:8000/docs`.

### Rodando com Docker Compose (API + banco)

```bash
docker compose up --build
```

### Testes

```bash
pytest
```

## Endpoint principal

`POST /api/v1/files/analyze` — multipart/form-data com o campo `file`.

Respostas:
- `201` — análise concluída, retorna o conteúdo analisado.
- `413` — arquivo excede `MAX_FILE_SIZE_BYTES`.
- `415` — extensão do arquivo fora de `ALLOWED_FILE_FORMATS`.
- `422` — conteúdo do arquivo excede o limite de contexto do modelo Gemini.

## Variáveis de ambiente

Veja [.env.example](.env.example).
