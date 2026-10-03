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
- `201` — análise concluída, retorna o conteúdo analisado e os jogos. O
  campo `total_tokens` traz o total de tokens (entrada + saída) gastos no
  Gemini, somando a chamada de análise e a de geração dos jogos, e o campo
  `ai_response_time_ms` traz o tempo de resposta do Gemini (em ms) somado
  dessas duas chamadas.
- `413` — arquivo excede `MAX_FILE_SIZE_BYTES`.
- `415` — extensão do arquivo fora de `ALLOWED_FILE_FORMATS`.
- `422` — conteúdo do arquivo excede o limite de contexto do modelo Gemini.

## Logs

A aplicação usa o `logging` padrão do Python (configurado em
`app/core/logging.py`). Os logs vão para o console e também para um **arquivo
separado**, `logs/studyou.log` por padrão, com rotação por tamanho (5 MB, 5
arquivos de backup). No Docker Compose a pasta `logs/` é montada no host.

O que é logado:
- Cada requisição: método, rota, status e duração (`app.api.middleware`).
- Início e conclusão de cada análise, tempo das chamadas ao Gemini e o id do
  resultado/erro persistido (`app.application.use_cases.analyze_file`).
- Erros de negócio (413/415/422) como `WARNING` e falhas do Gemini (502) e
  exceções não tratadas como `ERROR`, com stack trace.

Configuração via `LOG_LEVEL`, `LOG_FILE` (vazio = só console),
`LOG_MAX_BYTES` e `LOG_BACKUP_COUNT`. Com `LOG_LEVEL=DEBUG` também são
logados os detalhes do que é enviado ao Gemini.

## Token mestre (apenas para testes)

Para testar os endpoints autenticados sem fazer login, defina `MASTER_TOKEN`
no `.env` (mínimo de 32 caracteres) e envie-o como token Bearer:

```bash
curl -H "Authorization: Bearer $MASTER_TOKEN" http://localhost:8000/api/v1/files
```

No Swagger (`/docs`), basta colar o token no botão **Authorize**.

- O token autentica como o usuário `MASTER_TOKEN_USER_EMAIL` (padrão
  `master@studyou.local`). Se ele não existir, é criado no primeiro uso com
  uma senha aleatória, ou seja, só é acessível pelo token mestre. Se o e-mail
  for de um usuário já cadastrado, o token age como esse usuário.
- Tokens JWT normais continuam funcionando.
- Vazio = desativado (padrão). A API **não sobe** se `MASTER_TOKEN` estiver
  definido com `ENVIRONMENT=production`, e registra um aviso no log ao subir
  com ele ativo.

## Variáveis de ambiente

Veja [.env.example](.env.example).
