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