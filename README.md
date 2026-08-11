# Nura BE

## Stack

| Category | Version             |
|----------|---------------------|
| Java | 21                  |
| Spring Boot | 4.1.0               |
| Gradle | 9.5.1                |
| MySQL | 8.4                 |
| Docker | Latest              |
| Spring Data JPA | Spring Boot Managed |
| Spring Validation | Spring Boot Managed |
| Lombok | Spring Boot Managed |
| SpringDoc OpenAPI | 3.0.3               |


## Run

```bash
docker compose -f docker-compose.local.yml up -d
```

```bash
./gradlew bootRun
```

## Swagger

http://localhost:8080/swagger-ui.html

## Checkin API (Sync)

- Status: `GET /api/v1/checkin/status?date=2026-08-10`
- Create: `POST /api/v1/checkin` (multipart/form-data)

### Required Env

```bash
export AI_FASTAPI_BASE_URL="http://localhost:8000"
export OPENAI_API_KEY="<your-openai-key>"
export OPENAI_MODEL="gpt-4o-mini"
```

`OPENAI_API_KEY` is optional in local development. If empty, fallback comment is used.

### Curl - Checkin Status

```bash
curl -X GET "http://localhost:8080/api/v1/checkin/status?date=2026-08-10" \
  -H "Authorization: Bearer <access-token>"
```

### Curl - Create Checkin (without photo)

```bash
curl -X POST "http://localhost:8080/api/v1/checkin" \
  -H "Authorization: Bearer <access-token>" \
  -F "date=2026-08-10" \
  -F "fatigue=4" \
  -F "tightness=HIGH" \
  -F "redness=MODERATE"
```

### Curl - Create Checkin (with photo)

```bash
curl -X POST "http://localhost:8080/api/v1/checkin" \
  -H "Authorization: Bearer <access-token>" \
  -F "date=2026-08-10" \
  -F "fatigue=4" \
  -F "tightness=HIGH" \
  -F "redness=MODERATE" \
  -F "photo=@./sample-face.jpg"
```
