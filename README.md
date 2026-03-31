# Zabora Subscription Service

Microservicio de suscripciones (planes gratuito / premium), pagos con **Mercado Pago Checkout Bricks** (tarjeta y PSE), webhooks y sincronización de rol con **auth-service** (Feign + Consul).

## Requisitos

- Java 17, Maven 3.9+
- MySQL 8 (ejecución local o contenedor)
- **Docker** (necesario para ejecutar las pruebas de integración; si no está disponible, esas clases se **omiten** automáticamente)
- Consul (opcional en local; ver `application-example.yml` para URL fija de Feign → auth)

## Configuración

1. Copia variables desde [`.env.example`](./.env.example) a un archivo `.env` (no lo subas al repo).
2. Usa [`src/main/resources/application-example.yml`](./src/main/resources/application-example.yml) como plantilla de propiedades; los secretos deben ir por entorno (`MYSQL_PASSWORD`, `MP_ACCESS_TOKEN`, `JWT_SECRET`, etc.).
3. Crea la base `zabora_subscriptions` en MySQL si corres el servicio sin Docker.

## Ejecutar

```bash
cd suscription-service
mvn spring-boot:run
```

Puerto por defecto: **8004** (`server.port` / `SERVER_PORT`).

## Docker

```bash
docker build -t zabora-subscription-service:latest .
docker run --rm -p 8004:8004 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/zabora_subscriptions?sslMode=DISABLED \
  -e SPRING_DATASOURCE_USERNAME=... \
  -e SPRING_DATASOURCE_PASSWORD=... \
  zabora-subscription-service:latest
```

Ajusta variables según tu entorno (Mercado Pago, JWT, Consul, correo).

## Pruebas (Rest Assured)

Las pruebas de integración arrancan Spring Boot con **puerto aleatorio** y **MySQL en Testcontainers** (Rest Assured). Con Docker apagado o no accesible, JUnit las **salta** gracias a `@Testcontainers(disabledWithoutDocker = true)` en `BaseIntegrationTest`.

```bash
mvn test
```

### Clases principales

| Clase | Descripción |
|--------|-------------|
| [`FlujoCompletoSimuladoIT`](./src/test/java/com/zabora/subscription/integration/controller/FlujoCompletoSimuladoIT.java) | Flujo simulado: planes → suscripción → estado → premium pendiente → public key → mis pagos → cancelación → admin dashboard → webhook. |
| [`SuscripcionControladorIntegrationTest`](./src/test/java/com/zabora/subscription/integration/controller/SuscripcionControladorIntegrationTest.java) | CRUD de suscripción y cancelación. |
| [`WebhookControladorIntegrationTest`](./src/test/java/com/zabora/subscription/integration/controller/WebhookControladorIntegrationTest.java) | GET/POST webhook Mercado Pago. |
| [`BaseIntegrationTest`](./src/test/java/com/zabora/subscription/integration/BaseIntegrationTest.java) | Rest Assured + **mock de `AuthClient`** (no hace llamadas reales a auth al cancelar / premium). |

**Nota:** No se llama a la API real de Mercado Pago en los tests; el token del `application-test.yml` es ficticio.

### Ejecutar solo el flujo completo

```bash
mvn test -Dtest=FlujoCompletoSimuladoIT
```

Reportes Surefire: `target/surefire-reports/`.

## API útil

- `GET /api/suscripciones/planes` — catálogo (público)
- `POST /api/suscripciones/suscribir` — requiere headers `X-User-Id`, `X-User-Email`, `X-User-Role` (o JWT según gateway)
- `GET /api/suscripciones/estado`
- `POST /api/suscripciones/cancelar/{id}?inmediata=true|false`
- `GET /api/pagos/bricks/public-key`
- `POST /api/pagos/bricks/pay` / `POST /api/pagos/bricks/pay-pse`
- `GET /api/pagos/mis-pagos`
- `POST /api/webhooks/mercadopago`
- `GET /api/admin/suscripciones/dashboard` — uso admin

Documentación OpenAPI: `/swagger-ui.html` (si está habilitada).

## Datos de prueba

Con perfil `test`, [`TestDataInitializer`](./src/test/java/com/zabora/subscription/integration/TestDataInitializer.java) inserta planes **gratuito** y **premium** si la tabla está vacía.

---

**Seguridad:** no commitees `application.yml` con contraseñas ni tokens de producción; usa `.env` y `application-example.yml` como referencia.
