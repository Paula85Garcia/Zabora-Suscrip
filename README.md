# Zabora Subscription Service

Microservicio de suscripciones (planes gratuito / premium), pagos con **Mercado Pago Checkout Bricks** (tarjeta y PSE), webhooks y sincronización de rol con **auth-service** (Feign + Consul).

## Requisitos

- Java 17, Maven 3.9+
- MySQL 8
- **Docker** (para tests de integración con Testcontainers; si no hay Docker, esas pruebas se omiten)
- **Consul** (recomendado; si no, URL directa de Feign a auth — ver `.env.example`)
- **API Gateway** del monorepo enrutando a este servicio (en producción el front habla con el Gateway, no con el `:8004` directo)

## Configuración segura (importante)

1. **`application.yml`** del repo usa **solo placeholders** (`${VAR:default}`). No subas secretos a Git.
2. Copia **[`.env.example`](./.env.example)** a `.env` como referencia. Spring Boot **no lee `.env` por sí solo**: expórtalo, usa Docker `--env-file`, IntelliJ “Environment variables” o **`EnvironmentFile`** en systemd (ver [DEPLOY-PRODUCTION.md](./DEPLOY-PRODUCTION.md)).
3. Para overrides locales opcionales sin tocar el repo: crea **`application-local.yml`** en `src/main/resources/` (está en [`.gitignore`](./.gitignore)) y activa el perfil `local` si lo configuras en tu IDE (`spring.profiles.active=local`).

Variables clave:

| Variable | Uso |
|----------|-----|
| `MYSQL_*` | Conexión a `zabora_subscriptions` |
| `JWT_SECRET` | Mismo Base64 que **auth-service** y **Gateway** (`JWT_SECRET_KEY`) |
| `MP_*` | Mercado Pago (token, clave pública, webhook, URLs de retorno) |
| `CONSUL_*` | Descubrimiento; el servicio se registra como **`zabora-subscription-service`** |

Plantilla ampliada: [`application-example.yml`](./src/main/resources/application-example.yml).

## Integración con el API Gateway

El Gateway debe tener una ruta **`lb://zabora-subscription-service`** que coincida con `spring.application.name` + registro en Consul:

```yaml
# api-gateway application.yaml (referencia)
- id: suscription-service
  uri: lb://zabora-subscription-service
  predicates:
    - Path=/api/pagos/**,/api/suscripciones/**,/api/webhooks/mercadopago,/api/admin/**
```

Puntos críticos:

- **JWT**: `JWT_SECRET` (subscription) = `security.jwt.secret-key` (Gateway) = `jwt.secret` (auth). Si no coinciden, el usuario ve 401 o headers `X-User-Id` vacíos.
- **Webhooks Mercado Pago**: deben ser **públicos** en el Gateway (`/api/webhooks/**`). MP no envía `Authorization: Bearer`.
- **Catálogo de planes**: `GET /api/suscripciones/planes` suele ir público en el Gateway para el front.

Si el Gateway devuelve **503** hacia suscripciones, revisa en Consul que exista el servicio **`zabora-subscription-service`** y que la VM pueda alcanzar Consul y el puerto **8004**.

## Cómo enviar credenciales (equipo, cliente o VM)

- **No** las pongas en Git, en issues, ni en correo en claro a largo plazo.
- Preferible: **Azure Key Vault**, secretos del **pipeline** (GitHub Actions / Azure DevOps), o archivo en servidor vía SSH con **`chmod 600`**.
- Para compartir una vez: gestor de secretos (1Password, etc.) o enlace de un solo uso.
- Si alguna credencial llegó a un fichero versionado o a un chat, **rótala** (Mercado Pago, MySQL, Gmail app password, webhook secret, JWT).

## Ejecutar en local

```bash
cd suscription-service
# Define variables (ejemplo PowerShell):
# $env:MYSQL_PASSWORD="..."; $env:JWT_SECRET="..."; $env:MP_ACCESS_TOKEN="..."
mvn spring-boot:run
```

Puerto por defecto: **8004** (`PORT` / `server.port`).

## Docker

```bash
docker build -t zabora-subscription-service:latest .
docker run --rm -p 8004:8004 --env-file .env zabora-subscription-service:latest
```

## Despliegue en producción (Azure VM)

Guía detallada: **[DEPLOY-PRODUCTION.md](./DEPLOY-PRODUCTION.md)** (firewall, systemd, HTTPS, webhook, checklist).

## Pruebas (Rest Assured)

```bash
mvn test
```

Con Docker apagado, las IT que usan Testcontainers se saltan (`@Testcontainers(disabledWithoutDocker = true)`).

### Clases principales

| Clase | Descripción |
|--------|-------------|
| `FlujoCompletoSimuladoIT` | Flujo simulado extremo a extremo |
| `SuscripcionControladorIntegrationTest` | Suscripción y cancelación |
| `WebhookControladorIntegrationTest` | Webhook Mercado Pago |
| `BaseIntegrationTest` | Mock de `AuthClient` |

## API útil

- `GET /api/suscripciones/planes` — catálogo (público vía Gateway)
- `POST /api/suscripciones/suscribir` — requiere JWT / headers de usuario según Gateway
- `GET /api/suscripciones/estado`
- `POST /api/suscripciones/cancelar/{id}?inmediata=true|false`
- `GET /api/pagos/bricks/public-key`
- `POST /api/pagos/bricks/pay` / `POST /api/pagos/bricks/pay-pse`
- `GET /api/pagos/mis-pagos`
- `POST /api/webhooks/mercadopago`
- `GET /api/admin/suscripciones/dashboard` — admin

OpenAPI: `/swagger-ui.html` (si está habilitada).

## Datos de prueba

Con perfil `test`, `TestDataInitializer` inserta planes **gratuito** y **premium** si la tabla está vacía.

---

**Seguridad:** mantén `.env`, `application-local.yml` y cualquier archivo con tokens **fuera del repositorio** (ver `.gitignore`).
