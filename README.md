# Zabora Subscription Service

Servicio de suscripciones para la plataforma Zabora con integración de MercadoPago y PSE.

## 🚀 Características

- ✅ Gestión de planes de suscripción (Básico, Premium, Empresarial)
- ✅ Integración con MercadoPago (Tarjetas, PSE, Efectivo)
- ✅ Webhooks para notificaciones de pago
- ✅ Sistema de notificaciones al Auth Service
- ✅ Gestión de pagos y reembolsos
- ✅ Email service para confirmaciones
- ✅ API REST completa con documentación

## 📋 Prerrequisitos

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js 16+ (para frontend)

## 🛠️ Instalación

1. **Clonar el repositorio**
```bash
git clone <repository-url>
cd Zabora-Suscrip
```

2. **Configurar base de datos**
```sql
CREATE DATABASE zabora_subscriptions;
CREATE USER 'zabora'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON zabora_subscriptions.* TO 'zabora'@'localhost';
FLUSH PRIVILEGES;
```

3. **Configurar variables de entorno**
```bash
# Copiar archivo de configuración
cp application-example.yml application.yml

# Editar configuración
nano application.yml
```

4. **Compilar y ejecutar**
```bash
mvn clean install
mvn spring-boot:run
```

## 🧪 Ejecutar Tests

### Tests Unitarios
```bash
# Ejecutar todos los tests
mvn test

# Ejecutar tests específicos
mvn test -Dtest=SuscripcionServicioTest
mvn test -Dtest=SuscripcionControllerTest
```

### Tests de Integración
```bash
# Requiere Docker para bases de datos
mvn verify -Pintegration-test
```

### Reportes de Tests
Los reportes se generan en:
```
target/surefire-reports/
target/failsafe-reports/
```

## 📊 Endpoints Principales

### Gestión de Suscripciones
- `GET /api/suscripciones/planes` - Listar planes disponibles
- `POST /api/suscripciones/crear` - Crear nueva suscripción
- `GET /api/suscripciones/estado` - Obtener estado actual
- `POST /api/suscripciones/cancelar` - Cancelar suscripción

### Gestión de Pagos
- `POST /api/pagos/crear` - Crear pago
- `GET /api/pagos/{id}` - Obtener detalles de pago
- `POST /api/pagos/pse` - Pago con PSE
- `POST /api/pagos/reembolsar` - Solicitar reembolso

### Webhooks
- `POST /api/webhooks/mercadopago` - Recepción de notificaciones MercadoPago
- `GET /api/webhooks/mercadopago` - Verificación de webhook

## 💳 Integración de Pagos

### MercadoPago
```java
// Crear preferencia de pago
PagoRequest request = PagoRequest.builder()
    .planId("premium")
    .usuarioId(123)
    .metodoPago("tarjeta")
    .build();

ResponseEntity<PagoResponse> response = pagoController.crearPago(request);
```

### PSE
```java
// Pago con PSE
PseRequest request = PseRequest.builder()
    .planId("premium")
    .usuarioId(123)
    .banco("1022")
    .tipoPersona("natural")
    .build();

ResponseEntity<PagoResponse> response = pagoController.crearPagoPse(request);
```

## 🔧 Configuración

### Variables de Entorno
```bash
# MercadoPago
MERCADOPAGO_ACCESS_TOKEN=APP_USR-5585734670493828-03200
MERCADOPAGO_PUBLIC_KEY=APP_USR-ec81463d-913f-4b19-9d79-f21b4e0c615e
MERCADOPAGO_ENVIRONMENT=production

# Base de Datos
DB_HOST=localhost
DB_PORT=3306
DB_NAME=zabora_subscriptions
DB_USER=root
DB_PASSWORD=root

# Auth Service
AUTH_SERVICE_URL=http://localhost:8000

# Email
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password
```

## 🐳 Docker

### Construir Imagen
```bash
docker build -t zabora-subscription .
```

### Ejecutar con Docker Compose
```bash
docker-compose up -d
```

### Variables Docker
```bash
# Para producción
docker run -e SPRING_PROFILES_ACTIVE=prod \
           -e DB_HOST=mysql \
           -e MERCADOPAGO_ACCESS_TOKEN=your-token \
           zabora-subscription
```

## 📝 Logs

Los logs se configuran en `application.yml`:
- Consola: Formato legible con colores
- Archivo: `logs/subscription-service.log`
- Niveles: DEBUG (dev), INFO (prod)

## 🔄 Flujo de Pago

1. **Usuario selecciona plan** → `POST /api/suscripciones/crear`
2. **Sistema crea preferencia** → MercadoPago/PSE
3. **Usuario completa pago** → Redirección a proveedor
4. **Webhook recibe confirmación** → `POST /api/webhooks/mercadopago`
5. **Sistema actualiza suscripción** → Estado ACTIVO
6. **Notificación a Auth Service** → Actualización de rol
7. **Email confirmación** → Usuario notificado

## 🛡️ Seguridad

- Validación de JWT en endpoints protegidos
- Rate limiting configurable
- Sanitización de inputs
- HTTPS obligatorio en producción
- Verificación de webhooks

## 📈 Monitoreo

### Health Checks
- `GET /actuator/health` - Estado del servicio
- `GET /actuator/info` - Información del servicio
- `GET /actuator/metrics` - Métricas de rendimiento

### Métricas Clave
- Tiempo de respuesta de pagos
- Tasa de éxito de suscripciones
- Errores de MercadoPago
- Latencia de base de datos

## 🐛 Troubleshooting

### Problemas Comunes

#### 1. Error de conexión a MercadoPago
```bash
# Verificar token
curl -H "Authorization: Bearer $MERCADOPAGO_ACCESS_TOKEN" \
     https://api.mercadopago.com/v1/users/me
```

#### 2. Webhook no recibe notificaciones
```bash
# Verificar ngrok
ngrok http 8004

# Probar webhook
curl -X POST http://localhost:8004/api/webhooks/mercadopago \
     -H "Content-Type: application/json" \
     -d '{"type":"payment","data":{"id":"123"}}'
```

#### 3. Error de base de datos
```bash
# Verificar conexión
mysql -h localhost -u root -p zabora_subscriptions

# Revisar logs
tail -f logs/subscription-service.log
```

## 📞 Soporte

- **Documentación API**: `http://localhost:8004/swagger-ui.html`
- **Logs**: `logs/subscription-service.log`
- **Health**: `http://localhost:8004/actuator/health`

## 🤝 Contribuir

1. Fork del proyecto
2. Crear feature branch: `git checkout -b feature/nueva-funcionalidad`
3. Commit changes: `git commit -am 'Agregar nueva funcionalidad'`
4. Push branch: `git push origin feature/nueva-funcionalidad`
5. Pull Request

## 📄 Licencia

MIT License - Ver archivo LICENSE para detalles
4. [Requisitos Previos](#requisitos-previos)
5. [Instalación y Configuración](#instalación-y-configuración)
6. [Instalación de Ngrok](#instalación-de-ngrok)
7. [Estructura de la Base de Datos](#estructura-de-la-base-de-datos)
8. [Puntos finales de la API](#puntos-finales-de-la-api)
   - [Suscripciones](#suscripciones)
   - [Pagos](#pagos)
   - [Planes](#planes)
   - [Webhooks](#webhooks)
9. [Integración con MercadoPago](#integración-con-mercadopago)
10. [Ejemplos de uso](#ejemplos-de-uso)
11. [Pruebas](#pruebas)
12. [Solución de Problemas](#solución-de-problemas)

---

## ARQUITECTURA DEL SISTEMA DE SUSCRIPCIONES

### Visión General

El sistema de suscripciones de Zabora está diseñado como un microservicio independiente que se comunica con el Auth Service y el API Gateway.

```
┌─────────────────────────────────────────────────────────────────┐
│                        INTERNET                                  │
│                            ↓                                     │
│                    NGROK TUNNEL (Testing)                        │
│         https://abc123.ngrok-free.app                            │
│                            ↓                                     │
└─────────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    LOCALHOST (Tu PC)                             │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  API GATEWAY (Puerto 9000)                                │  │
│  │  Ruta: /api/suscripciones/** → Subscription Service      │  │
│  │  Ruta: /api/pagos/**        → Subscription Service       │  │
│  │  Ruta: /api/webhooks/**     → Subscription Service       │  │
│  │  Ruta: /api/upgrade/**      → Auth Service               │  │
│  └────────┬─────────────────────────────────────────────────┘  │
│           │                                                     │
│           ↓                                                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  SUBSCRIPTION SERVICE (Puerto 8004)                       │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │ Controladores:                                     │  │  │
│  │  │ - SuscripcionController                            │  │  │
│  │  │ - PagoController                                    │  │  │
│  │  │ - PlanController                                    │  │  │
│  │  │ - MercadoPagoWebhookController                      │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                       │                                    │  │
│  │                       ↓                                    │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │ Servicios:                                         │  │  │
│  │  │ - SuscripcionServicioReal                          │  │  │
│  │  │ - MercadoPagoServicio                              │  │  │
│  │  │ - EmailServicio                                    │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                       │                                    │  │
│  │                       ↓                                    │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │ Repositorios:                                      │  │  │
│  │  │ - PlanSuscripcionRepository                        │  │  │
│  │  │ - SuscripcionUsuarioRepository                      │  │  │
│  │  │ - PagoRepository                                    │  │  │
│  │  │ - LogSuscripcionRepository                          │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                       │                                    │  │
│  │                       ↓                                    │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │ Clientes Feign:                                    │  │  │
│  │  │ - AuthClient → http://localhost:8000               │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                       │                                         │
│                       ↓                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  AUTH SERVICE (Puerto 8000)                               │  │
│  │  Endpoint: /api/upgrade/premium/{userId}                 │  │
│  │  Función: Actualizar rol de usuario a PREMIUM            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                       │                                         │
│                       ↓                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  BASES DE DATOS (Puerto 3306)                             │  │
│  │  - zabora_subscriptions (suscripciones, pagos, planes)   │  │
│  │  - zabora_auth (usuarios, roles)                         │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  FRONTEND - ANGULAR (Puerto 4200)                         │  │
│  │  Páginas: /suscripciones/planes, /mi-suscripcion         │  │
│  │  Servicios: pago.service.ts, suscripcion.service.ts      │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## PILA TECNOLÓGICA

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 17+ | Lenguaje de programación |
| Spring Boot | 3.x | Framework principal |
| Spring Data JPA | 3.x | Persistencia de datos |
| Hibernate | 6.x | ORM |
| MySQL | 8.0+ | Base de datos |
| MercadoPago SDK | 2.x | Integración con pasarela de pagos |
| Lombok | - | Reducción de código repetitivo |
| Feign Client | - | Comunicación entre microservicios |
| Swagger/OpenAPI | 3.0 | Documentación de la API |

---

## CARACTERÍSTICAS PRINCIPALES

### Gestión de Planes
- Plan GRATUITO (acceso básico)
- Plan PREMIUM (acceso completo)
- Configuración flexible de precios
- Activación/desactivación de planes

### Gestión de Suscripciones
- Creación de suscripciones para usuarios
- Estados: PENDIENTE_PAGO, ACTIVA, CANCELADA, EXPIRADA
- Control de períodos de facturación (mensual)
- Cancelación al final del período o inmediata
- Renovación automática

### Integración con MercadoPago
- Creación de preferencias de pago
- Procesamiento de webhooks
- Verificación de estado de pagos
- Soporte para múltiples métodos de pago

### Comunicación con Auth Service
- Feign Client para comunicación síncrona
- Actualización automática de roles de usuario
- Manejo de errores y reintentos

### Notificaciones por Email
- Confirmación de pago exitoso
- Activación de suscripción
- Cancelación de suscripción

### Logging y Auditoría
- Logs detallados de todas las operaciones
- Trazabilidad de cambios en suscripciones
- Registro de webhooks recibidos

---

## REQUISITOS PREVIOS

Antes de comenzar, asegúrate de tener instalado:

- Java JDK 17+ (Descargar)
- MySQL 8.0+ (Descargar)
- Maven 3.6+ (incluido en la mayoría de IDE)
- Git (Descargar)
- Postman o cURL (opcional, para probar)

---

## INSTALACIÓN Y CONFIGURACIÓN

### 1. Clonar el Repositorio

```bash
git clone https://github.com/Zabora/Zabora-Suscrip.git
cd Zabora-Suscrip
```

### 2. Configurar la Base de Datos

#### 2.1. Crear la Base de Datos

```sql
CREATE DATABASE IF NOT EXISTS zabora_subscriptions 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

#### 2.2. Datos Predefinidos Incluidos

```sql
USE zabora_subscriptions;

-- Planes predefinidos
INSERT INTO planes_suscripcion (nombre, descripcion, precio, moneda, activo) VALUES
('gratuito', 'Plan gratuito con funcionalidades básicas', 0, 'COP', true),
('premium', 'Plan premium con todas las funcionalidades', 29900, 'COP', true);
```

### 3. Configurar Credenciales de la Aplicación

Edita el archivo `src/main/resources/application.yml`:

```yaml
server:
  port: 8004

spring:
  application:
    name: zabora-subscription-service
  
  datasource:
    url: jdbc:mysql://localhost:3306/zabora_subscriptions?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: TU_USUARIO_MYSQL    # Cambiar esto
    password: TU_CONTRASEÑA_MYSQL # Cambiar esto
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true

# ========================================
# MERCADOPAGO CONFIGURATION
# ========================================
mercadopago:
  # Credenciales de PRUEBA (Testing)
  access-token: TEST-6541386271619-022410-5b4025c5ab0255f05846d51533694260-3223648585
  public-key: TEST-01677c07-6b61-4718-9d81-78123be19879
  environment: test
  
  # URLs de redirección (Frontend Angular)
  success-url: http://localhost:4200/home?payment=success
  failure-url: http://localhost:4200/home?payment=failure
  pending-url: http://localhost:4200/home?payment=pending
  
  webhook:
    secret: c4c69a70dcb99ebf7b530bfd4cd8080e49feed021d9bc56e5bd27d119b59eec5
    # IMPORTANTE: ACTUALIZAR CON LA URL DE NGROK
    notification-url: https://TU_SUBDOMINIO_NGROK.ngrok-free.app/api/webhooks/mercadopago

# Email configuration
email:
  enabled: true
  from: zaborapayment@gmail.com

# Feign Client para Auth Service
feign:
  client:
    config:
      auth-service:
        url: http://localhost:8000
        connectTimeout: 10000
        readTimeout: 30000

# Logging
logging:
  level:
    com.zabora.subscription: DEBUG
    com.mercadopago: DEBUG
    feign: DEBUG
```

### 4. Compilar y Ejecutar

```bash
# Compilar el proyecto
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run
```

La aplicación estará disponible en: http://localhost:8004

### 5. Verificar la Instalación

```bash
# Verificar health check
curl http://localhost:8004/actuator/health

# Verificar planes disponibles
curl http://localhost:8004/api/suscripciones/planes
```

Respuesta esperada:
```json
[
  {
    "id": 1,
    "nombre": "gratuito",
    "descripcion": "Plan gratuito con funcionalidades básicas",
    "precio": 0.0,
    "moneda": "COP",
    "activo": true
  },
  {
    "id": 2,
    "nombre": "premium",
    "descripcion": "Plan premium con todas las funcionalidades",
    "precio": 29900.0,
    "moneda": "COP",
    "activo": true
  }
]
```

---

## INSTALACIÓN DE NGROK

### ¿Qué es Ngrok?

Ngrok es una herramienta que crea un túnel seguro desde internet a tu servidor local. Es necesaria para que MercadoPago pueda enviar notificaciones (webhooks) a tu entorno de desarrollo local.

### Opción 1: Instalador de Windows

#### Paso 1: Descargar Ngrok

1. Ve a: https://ngrok.com/download
2. Haz clic en "Download for Windows"
3. Descarga el archivo `ngrok.zip`

#### Paso 2: Extraer el archivo

1. Descomprime `ngrok.zip`
2. Mueve `ngrok.exe` a una carpeta permanente, por ejemplo:
   ```
   C:\Tools\ngrok\ngrok.exe
   ```

#### Paso 3: Agregar Ngrok al PATH (opcional pero recomendado)

1. Haz clic derecho en "Este equipo" → Propiedades
2. Haz clic en "Configuración avanzada del sistema"
3. Haz clic en "Variables de entorno"
4. En "Variables del sistema", selecciona `Path` → Haz clic en "Editar"
5. Haz clic en "Nuevo" → Agrega: `C:\Tools\ngrok`
6. Haz clic en "Aceptar" en todas las ventanas

#### Paso 4: Verificar instalación

Abre una terminal y ejecuta:
```cmd
ngrok version
```

Deberías ver algo como:
```
ngrok version 3.x.x
```

### Opción 2: Con Chocolatey (si lo tienes instalado)

```powershell
choco install ngrok
```

### Configuración de Ngrok

#### Paso 1: Crear cuenta en Ngrok

1. Ve a: https://dashboard.ngrok.com/signup
2. Regístrate (puedes usar tu cuenta de Google/GitHub)
3. Confirma tu email

#### Paso 2: Obtener tu Authtoken

1. Inicia sesión en: https://dashboard.ngrok.com
2. Ve a la sección "Your Authtoken"
3. Copia el token (se ve así: `2aB3cD4eF5gH6iJ7kL8mN9oP0qR1sT2uV3wX4yZ`)

#### Paso 3: Configurar Authtoken en Ngrok

Abre una terminal y ejecuta:

```cmd
ngrok config add-authtoken TU_TOKEN_AQUI
```

Ejemplo:
```cmd
ngrok config add-authtoken 2aB3cD4eF5gH6iJ7kL8mN9oP0qR1sT2uV3wX4yZ
```

Deberías ver:
```
Authtoken saved to configuration file: C:\Users\TuUsuario\.ngrok2\ngrok.yml
```

#### Paso 4: Iniciar Ngrok

Ngrok debe apuntar a tu API Suscription-Service (puerto 8004):

```cmd
ngrok http 8004
```

IMPORTANTE: Debes dejar esta ventana ABIERTA mientras pruebes. Si la cierras, el túnel se cierra.

#### Paso 5: Verificar que funciona

Deberías ver algo como esto en la consola:

```
ngrok

Session Status                online
Account                       tu_email@gmail.com (Plan: Free)
Version                       3.5.0
Region                        United States (us)
Latency                       45ms
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://abc123def456.ngrok-free.app -> http://localhost:9000

Connections                   ttl     opn     rt1     rt5     p50     p90
                              0       0       0.00    0.00    0.00    0.00
```

Copia la URL de Forwarding: `https://abc123def456.ngrok-free.app`

#### Paso 6: Actualizar la configuración del Subscription Service

1. Abre `src/main/resources/application.yml`
2. Actualiza la URL de notificación con la URL de Ngrok:

```yaml
mercadopago:
  webhook:
    notification-url: https://abc123def456.ngrok-free.app/api/webhooks/mercadopago
```

3. Reinicia el Subscription Service (Ctrl+C y luego `mvn spring-boot:run`)

#### Paso 7: Acceder a la interfaz web de Ngrok

Puedes ver todas las peticiones que pasan por Ngrok en:

```
http://127.0.0.1:4040
```

Esto es muy útil para depurar si los webhooks están llegando correctamente.

### Nota importante sobre Ngrok Free

En el plan gratuito de Ngrok, cada vez que reinicias el túnel, la URL cambia. Debes actualizar la `notification-url` en `application.yml` y en la configuración del webhook en MercadoPago cada vez que esto ocurra.

Para evitar esto, considera el plan de pago de Ngrok que ofrece URLs fijas.

---

## ESTRUCTURA DE LA BASE DE DATOS

### Diagrama de Relaciones

```
┌─────────────────┐       ┌──────────────────────┐       ┌─────────────────┐
│   users         │       │ suscripciones_usuarios│       │ planes_suscripcion│
│ (zabora_auth)   │       │   (zabora_subscriptions)    │   │ (zabora_subscriptions)│
├─────────────────┤       ├──────────────────────┤       ├─────────────────┤
│ id INT PK       │───────│ usuario_id INT       │       │ id INT PK       │
│ nombre          │       │ id VARCHAR PK        │───────│ nombre          │
│ email           │       │ plan_id INT FK       │       │ descripcion     │
│ tipo_usuario    │       │ estado VARCHAR       │       │ precio DECIMAL  │
│ password        │       │ inicio_periodo_actual│       │ moneda VARCHAR  │
│ fecha_inicio_premium│    │ fin_periodo_actual  │       │ activo BOOLEAN  │
└─────────────────┘       │ cancelar_al_final    │       └─────────────────┘
                          │ fecha_creacion       │
                          └──────────┬───────────┘
                                     │
                                     │
                          ┌──────────▼───────────┐       ┌─────────────────┐
                          │      pagos           │       │ logs_suscripciones│
                          │ (zabora_subscriptions)│       │ (zabora_subscriptions)│
                          ├──────────────────────┤       ├─────────────────┤
                          │ id VARCHAR PK        │       │ id BIGINT PK    │
                          │ suscripcion_id VARCHAR FK│────│ suscripcion_id  │
                          │ usuario_id INT       │       │ accion          │
                          │ monto DECIMAL         │       │ estado_anterior │
                          │ moneda VARCHAR        │       │ estado_nuevo    │
                          │ metodo_pago VARCHAR   │       │ usuario_id      │
                          │ estado VARCHAR        │       │ detalles       │
                          │ id_intento_pago VARCHAR│      │ fecha          │
                          │ fecha_pago DATETIME   │       └─────────────────┘
                          │ fecha_creacion DATETIME│
                          └──────────────────────┘
```

### Tablas Principales

#### planes_suscripcion - Planes disponibles

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | INT | ID único del plan |
| nombre | VARCHAR(50) | Nombre del plan (gratuito, premium) |
| descripcion | TEXT | Descripción detallada |
| precio | DECIMAL(10,2) | Precio del plan |
| moneda | VARCHAR(3) | Moneda (COP, USD, etc.) |
| activo | BOOLEAN | Si el plan está disponible |

Datos predefinidos:

| ID | nombre | precio | moneda |
|----|--------|--------|--------|
| 1 | gratuito | 0.00 | COP |
| 2 | premium | 29900.00 | COP |

#### suscripciones_usuarios - Suscripciones de usuarios

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | VARCHAR(100) | ID único de la suscripción (formato: sub_ + UUID) |
| usuario_id | INT | ID del usuario (de auth service) |
| plan_id | INT | FK al plan seleccionado |
| estado | VARCHAR(50) | PENDIENTE_PAGO, ACTIVA, CANCELADA, EXPIRADA |
| inicio_periodo_actual | DATETIME | Inicio del período actual de facturación |
| fin_periodo_actual | DATETIME | Fin del período actual |
| cancelar_al_final_periodo | BOOLEAN | Si se cancelará al final del período |
| fecha_creacion | DATETIME | Fecha de creación |

#### pagos - Historial de pagos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | VARCHAR(100) | ID único del pago (formato: pago_ + UUID) |
| suscripcion_id | VARCHAR(100) | FK a la suscripción |
| usuario_id | INT | ID del usuario |
| monto | DECIMAL(10,2) | Monto pagado |
| moneda | VARCHAR(3) | Moneda del pago |
| metodo_pago | VARCHAR(50) | Método usado (tarjeta, etc.) |
| estado | VARCHAR(50) | PENDIENTE, COMPLETADO, FALLIDO, REEMBOLSADO |
| id_intento_pago | VARCHAR(255) | ID del pago en MercadoPago |
| fecha_pago | DATETIME | Fecha en que se completó el pago |
| fecha_creacion | DATETIME | Fecha de creación |

#### logs_suscripciones - Auditoría

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT | ID único del log |
| suscripcion_id | VARCHAR(100) | ID de la suscripción |
| accion | VARCHAR(100) | Acción realizada |
| estado_anterior | VARCHAR(50) | Estado antes del cambio |
| estado_nuevo | VARCHAR(50) | Estado después del cambio |
| usuario_id | INT | ID del usuario |
| detalles | TEXT | Detalles adicionales |
| fecha | DATETIME | Fecha del log |

---

## ENDPOINTS DE LA API

### URL Base

```
http://localhost:8004
```

A través del API Gateway:

```
http://localhost:9000/api/suscripciones
http://localhost:9000/api/pagos
http://localhost:9000/api/webhooks
```

---

## PLANES

### Obtener Todos los Planes

```
GET /api/suscripciones/planes
```

Respuesta Exitosa:
```json
[
  {
    "id": 1,
    "nombre": "gratuito",
    "descripcion": "Plan gratuito con funcionalidades básicas",
    "precio": 0.0,
    "moneda": "COP",
    "activo": true
  },
  {
    "id": 2,
    "nombre": "premium",
    "descripcion": "Plan premium con todas las funcionalidades",
    "precio": 29900.0,
    "moneda": "COP",
    "activo": true
  }
]
```

### Obtener Plan por ID

```
GET /api/suscripciones/planes/{id}
```

Ejemplo:
```
GET /api/suscripciones/planes/2
```

Respuesta:
```json
{
  "id": 2,
  "nombre": "premium",
  "descripcion": "Plan premium con todas las funcionalidades",
  "precio": 29900.0,
  "moneda": "COP",
  "activo": true
}
```

---

## SUSCRIPCIONES

### Crear Suscripción

```
POST /api/suscripciones/suscribir
Content-Type: application/json
Headers: X-User-Id: {userId}
```

Cuerpo de la solicitud:
```json
{
  "planId": 2,
  "metodoPago": "MERCADOPAGO"
}
```

Respuesta Exitosa (Plan Premium - requiere pago):
```json
{
  "requierePago": true,
  "idSuscripcion": "sub_abc123def456",
  "mensaje": "Suscripción creada. Complete el pago para activarla."
}
```

Respuesta Exitosa (Plan Gratuito - no requiere pago):
```json
{
  "requierePago": false,
  "idSuscripcion": "sub_xyz789uvw123",
  "mensaje": "Suscripción gratuita activada exitosamente"
}
```

### Obtener Estado de Suscripción del Usuario

```
GET /api/suscripciones/estado
Headers: X-User-Id: {userId}
```

Respuesta Exitosa:
```json
{
  "id": "sub_abc123def456",
  "plan": "premium",
  "estado": "ACTIVA",
  "inicioPeriodoActual": "2026-03-17T15:30:00",
  "finPeriodoActual": "2026-04-17T15:30:00",
  "diasRestantes": 30,
  "horasRestantes": 720,
  "cancelarAlFinalPeriodo": false
}
```

### Obtener Historial de Suscripciones del Usuario

```
GET /api/suscripciones/historial
Headers: X-User-Id: {userId}
```

Respuesta Exitosa:
```json
[
  {
    "id": "sub_abc123def456",
    "plan": "premium",
    "estado": "ACTIVA",
    "fechaInicio": "2026-03-17T15:30:00",
    "fechaFin": "2026-04-17T15:30:00"
  },
  {
    "id": "sub_xyz789uvw123",
    "plan": "gratuito",
    "estado": "CANCELADA",
    "fechaInicio": "2026-02-17T10:15:00",
    "fechaFin": "2026-02-17T10:15:00"
  }
]
```

### Cancelar Suscripción

```
POST /api/suscripciones/cancelar/{suscripcionId}
Content-Type: application/json
Headers: X-User-Id: {userId}
```

Cuerpo de la solicitud:
```json
{
  "tipoCancelacion": "FIN_PERIODO"  // o "INMEDIATA"
}
```

Respuesta Exitosa (Cancelación al final del período):
```json
{
  "mensaje": "La suscripción se cancelará al final del período actual (2026-04-17)",
  "fechaCancelacion": "2026-04-17T15:30:00",
  "cancelacionInmediata": false
}
```

Respuesta Exitosa (Cancelación inmediata):
```json
{
  "mensaje": "Suscripción cancelada inmediatamente",
  "fechaCancelacion": "2026-03-17T15:45:00",
  "cancelacionInmediata": true
}
```

---

## PAGOS

### Crear Preferencia de Pago

```
POST /api/pagos/crear-preferencia
Content-Type: application/json
Headers: X-User-Id: {userId}, X-User-Email: {email}
```

Cuerpo de la solicitud:
```json
{
  "suscripcionId": "sub_abc123def456",
  "planId": 2
}
```

Respuesta Exitosa:
```json
{
  "id": "123456789-abcdef",
  "initPoint": "https://www.mercadopago.com.co/checkout/v1/redirect?pref_id=123456789-abcdef",
  "sandboxInitPoint": "https://sandbox.mercadopago.com.co/checkout/v1/redirect?pref_id=123456789-abcdef"
}
```

### Obtener Historial de Pagos del Usuario

```
GET /api/pagos/historial
Headers: X-User-Id: {userId}
```

Respuesta Exitosa:
```json
[
  {
    "id": "pago_abc123def456",
    "monto": 29900.0,
    "moneda": "COP",
    "estado": "COMPLETADO",
    "metodoPago": "credit_card",
    "fechaPago": "2026-03-17T15:35:00",
    "idIntentoPago": "1234567890"
  }
]
```

---

## WEBHOOKS

### Webhook de MercadoPago

```
POST /api/webhooks/mercadopago
Content-Type: application/json
```

Payload recibido de MercadoPago:
```json
{
  "action": "payment.updated",
  "api_version": "v1",
  "data": {
    "id": "1234567890"
  },
  "date_created": "2026-03-17T15:35:00Z",
  "id": "1234567890",
  "live_mode": false,
  "type": "payment",
  "user_id": "3233058352"
}
```

Respuesta Exitosa:
```
200 OK
```

Procesamiento interno:
1. Recibe webhook con payment ID
2. Consulta estado del pago en MercadoPago
3. Si estado = "approved", actualiza pago y activa suscripción
4. Llama a Auth Service para actualizar rol a PREMIUM
5. Registra logs de la operación

---

## INTEGRACIÓN CON MERCADOPAGO

### Credenciales de PRUEBA (Testing)

| Campo | Valor |
|-------|-------|
| Public Key (TEST) | `TEST-01677c07-6b61-4718-9d81-78123be19879` |
| Access Token (TEST) | `TEST-6541386271619-022410-5b4025c5ab0255f05846d51533694260-3223648585` |
| Webhook Secret | `c4c69a70dcb99ebf7b530bfd4cd8080e49feed021d9bc56e5bd27d119b59eec5` |

### Credenciales de PRODUCCIÓN

| Campo | Valor |
|-------|-------|
| Public Key (PROD) | `APP_USR-46460ebc-eb77-4630-a699-42155e7b3df4` |
| Access Token (PROD) | `APP_USR-8754802509768642-022816-29928cef3f32a3600cd6098f87947575-3233058352` |
| Webhook Secret | `c4c69a70dcb99ebf7b530bfd4cd8080e49feed021d9bc56e5bd27d119b59eec5` |

### Configuración de Webhook en MercadoPago

#### Paso 1: Acceder al Panel de MercadoPago

1. Ve a: https://www.mercadopago.com.co/developers/panel
2. Inicia sesión con tu cuenta de vendedor

#### Paso 2: Ir a Webhooks

1. En el menú lateral, haz clic en "Webhooks"
2. O ve directamente a: https://www.mercadopago.com.co/developers/panel/webhooks

#### Paso 3: Crear un Webhook para Payments

1. Haz clic en "Crear webhook" o "+ Agregar"
2. Tipo: Payments (Pagos)
3. Configura la URL:
   ```
   https://TU_SUBDOMINIO_NGROK.ngrok-free.app/api/webhooks/mercadopago
   ```
4. Eventos a escuchar:
   - payment.created
   - payment.updated
5. Haz clic en "Guardar"

### Cuentas de Prueba de MercadoPago

#### Comprador (Buyer):
- User ID: 3228610948
- Password: ODfKavGULP

#### Vendedor (Seller):
- User ID: 3233058352
- Password: P97mgkJIPd

#### Tarjeta de Prueba:
- Número: 5031 7557 3453 0604
- CVV: 123
- Fecha de expiración: 11/25
- Nombre: APRO

Más tarjetas de prueba: https://www.mercadopago.com.co/developers/es/docs/checkout-pro/additional-content/test-cards

---

## FLUJO COMPLETO DE PAGO

### Paso a Paso

```
1. USUARIO (Frontend)
   ↓
   POST /api/suscripciones/suscribir
   ↓
2. API GATEWAY (9000)
   ↓ (valida JWT)
   ↓ (añade headers: X-User-Id, X-User-Email)
   ↓
3. SUBSCRIPTION SERVICE (8004)
   ↓ Crea suscripción con estado PENDIENTE_PAGO
   ↓ Retorna: { requierePago: true, idSuscripcion: "sub_..." }
   ↓
4. USUARIO (Frontend)
   ↓
   POST /api/pagos/crear-preferencia
   ↓
5. API GATEWAY → SUBSCRIPTION SERVICE
   ↓ Crea preferencia en MercadoPago
   ↓ Retorna: { initPoint: "https://mercadopago.com/checkout/..." }
   ↓
6. USUARIO (Frontend)
   ↓ Redirige a: initPoint (MercadoPago)
   ↓
7. USUARIO completa el pago en MercadoPago
   ↓
8. MERCADOPAGO
   ↓ Redirige a: http://localhost:4200/home?payment=success
   ↓ Envía webhook a: https://abc123.ngrok-free.app/api/webhooks/mercadopago
   ↓
9. NGROK TUNNEL
   ↓ Redirige a: http://localhost:9000/api/webhooks/mercadopago
   ↓
10. API GATEWAY (9000)
    ↓ (permite paso sin JWT porque es ruta pública)
    ↓
11. SUBSCRIPTION SERVICE (8004)
    ↓ Recibe webhook
    ↓ Consulta pago en MercadoPago API
    ↓ Si status = "approved":
    ↓   - Actualiza pago: PENDIENTE → COMPLETADO
    ↓   - Actualiza suscripción: PENDIENTE_PAGO → ACTIVA
    ↓   - Llama a Auth Service:
    ↓
12. AUTH SERVICE (8000)
    ↓ POST /api/upgrade/premium/{userId}
    ↓ Actualiza: tipo_usuario = 2 (PREMIUM)
    ↓ Actualiza: fecha_inicio_premium = NOW()
    ↓
13. USUARIO (Frontend)
    ↓ Consulta estado: GET /api/suscripciones/estado
    ↓ Muestra: Plan PREMIUM, estado ACTIVA
```

---

## EJEMPLOS DE USO

### Ejemplo 1: Usuario se suscribe a Premium

#### 1. Verificar planes disponibles

```bash
curl http://localhost:9000/api/suscripciones/planes
```

#### 2. Crear suscripción

```bash
curl -X POST http://localhost:9000/api/suscripciones/suscribir \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "planId": 2,
    "metodoPago": "MERCADOPAGO"
  }'
```

Respuesta:
```json
{
  "requierePago": true,
  "idSuscripcion": "sub_abc123def456",
  "mensaje": "Suscripción creada. Complete el pago para activarla."
}
```

#### 3. Crear preferencia de pago

```bash
curl -X POST http://localhost:9000/api/pagos/crear-preferencia \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "X-User-Email: usuario@example.com" \
  -d '{
    "suscripcionId": "sub_abc123def456",
    "planId": 2
  }'
```

Respuesta:
```json
{
  "id": "123456789-abcdef",
  "initPoint": "https://www.mercadopago.com.co/checkout/v1/redirect?pref_id=123456789-abcdef"
}
```

#### 4. Verificar estado después del pago

```bash
curl -H "X-User-Id: 1" http://localhost:9000/api/suscripciones/estado
```

Respuesta después del webhook:
```json
{
  "id": "sub_abc123def456",
  "plan": "premium",
  "estado": "ACTIVA",
  "inicioPeriodoActual": "2026-03-17T15:30:00",
  "finPeriodoActual": "2026-04-17T15:30:00",
  "diasRestantes": 30,
  "horasRestantes": 720,
  "cancelarAlFinalPeriodo": false
}
```

### Ejemplo 2: Usuario cancela suscripción

#### Cancelar al final del período

```bash
curl -X POST http://localhost:9000/api/suscripciones/cancelar/sub_abc123def456 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "tipoCancelacion": "FIN_PERIODO"
  }'
```

Respuesta:
```json
{
  "mensaje": "La suscripción se cancelará al final del período actual (2026-04-17)",
  "fechaCancelacion": "2026-04-17T15:30:00",
  "cancelacionInmediata": false
}
```

#### Verificar estado después de cancelación

```bash
curl -H "X-User-Id: 1" http://localhost:9000/api/suscripciones/estado
```

Respuesta:
```json
{
  "id": "sub_abc123def456",
  "plan": "premium",
  "estado": "ACTIVA",
  "inicioPeriodoActual": "2026-03-17T15:30:00",
  "finPeriodoActual": "2026-04-17T15:30:00",
  "diasRestantes": 30,
  "horasRestantes": 720,
  "cancelarAlFinalPeriodo": true
}
```

---

## PRUEBAS

### Colección de Postman

#### Endpoints de Planes
- GET /api/suscripciones/planes - Listar todos los planes
- GET /api/suscripciones/planes/{id} - Obtener plan por ID

#### Endpoints de Suscripciones
- POST /api/suscripciones/suscribir - Crear suscripción
- GET /api/suscripciones/estado - Estado de suscripción actual
- GET /api/suscripciones/historial - Historial de suscripciones
- POST /api/suscripciones/cancelar/{id} - Cancelar suscripción

#### Endpoints de Pagos
- POST /api/pagos/crear-preferencia - Crear preferencia de pago
- GET /api/pagos/historial - Historial de pagos

#### Webhook (simulado)
- POST /api/webhooks/mercadopago - Simular webhook

### Pruebas con cURL

```bash
# 1. Crear suscripción
curl -X POST http://localhost:9000/api/suscripciones/suscribir \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"planId":2,"metodoPago":"MERCADOPAGO"}'

# 2. Crear preferencia de pago
curl -X POST http://localhost:9000/api/pagos/crear-preferencia \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "X-User-Email: test@example.com" \
  -d '{"suscripcionId":"sub_abc123def456","planId":2}'

# 3. Verificar estado
curl -H "X-User-Id: 1" http://localhost:9000/api/suscripciones/estado
```

### Simular Webhook Manualmente

```bash
curl -X POST http://localhost:9000/api/webhooks/mercadopago \
  -H "Content-Type: application/json" \
  -d '{
    "action": "payment.updated",
    "api_version": "v1",
    "data": {
      "id": "1234567890"
    },
    "type": "payment",
    "user_id": "3233058352"
  }'
```

---

## SOLUCIÓN DE PROBLEMAS

### Problema 1: "Error al crear suscripción: Usuario no encontrado"

Causa: El header X-User-Id no se está enviando o el ID no existe

Solución:
```bash
# Verificar que el header se envía correctamente
curl -H "X-User-Id: 1" http://localhost:9000/api/suscripciones/estado

# Verificar que el usuario existe en Auth Service
curl http://localhost:8000/api/auth/profile -H "Authorization: Bearer {token}"
```

### Problema 2: "El pago no se procesa después del webhook"

Causa: La URL de webhook en MercadoPago no coincide con la de Ngrok

Solución:
1. Verificar URL actual de Ngrok:
   ```bash
   # Ver en consola de Ngrok
   Forwarding: https://abc123.ngrok-free.app -> http://localhost:9000
   ```
2. Actualizar application.yml:
   ```yaml
   notification-url: https://abc123.ngrok-free.app/api/webhooks/mercadopago
   ```
3. Reiniciar Subscription Service
4. Actualizar webhook en panel de MercadoPago

### Problema 3: "No se actualiza el rol en Auth Service"

Causa: Feign Client no puede conectarse a Auth Service

Solución:
```bash
# Verificar que Auth Service está corriendo
curl http://localhost:8000/actuator/health

# Verificar logs de Feign Client
# Buscar en los logs: "FeignClient: POST http://localhost:8000/api/upgrade/premium/1"
```

### Problema 4: "La suscripción queda en PENDIENTE_PAGO para siempre"

Causa: El webhook no llega o no se procesa correctamente

Solución:
```bash
# 1. Verificar que Ngrok está corriendo
# 2. Verificar que el webhook está configurado en MercadoPago
# 3. Verificar logs del webhook
# 4. Simular webhook manualmente:
curl -X POST http://localhost:9000/api/webhooks/mercadopago \
  -H "Content-Type: application/json" \
  -d '{"action":"payment.updated","data":{"id":"ID_DEL_PAGO"},"type":"payment"}'
```

### Problema 5: "Error: Plan no encontrado"

Causa: El planId enviado no existe (debe ser 1 o 2)

Solución:
```bash
# Verificar planes disponibles
curl http://localhost:9000/api/suscripciones/planes
# Plan IDs: 1 (gratuito), 2 (premium)
```

### Problema 6: "Error al crear preferencia: Access token inválido"

Causa: Credenciales de MercadoPago incorrectas

Solución:
```bash
# Verificar que las credenciales en application.yml son correctas
mercadopago:
  access-token: TEST-... (comienza con TEST- para pruebas)
  public-key: TEST-...
```

### Problema 7: Ngrok dice "command not found"

Causa: Ngrok no está en el PATH o no está instalado

Solución:
```bash
# Opción A: Ejecutar con la ruta completa
C:\Tools\ngrok\ngrok.exe http 9000

# Opción B: Agregar al PATH
# 1. Buscar "Variables de entorno" en Windows
# 2. Editar Path y agregar C:\Tools\ngrok
# 3. Reiniciar terminal
```

### Problema 8: "Session Expired" en Ngrok

Causa: El token de Ngrok expiró o no está configurado

Solución:
```bash
# 1. Obtén un nuevo token en https://dashboard.ngrok.com
# 2. Reconfigura el token
ngrok config add-authtoken TU_NUEVO_TOKEN
```

### Problema 9: Ngrok Web Interface (4040) no abre

Causa: Ngrok no está corriendo o puerto 4040 ocupado

Solución:
```bash
# Verificar que Ngrok esté corriendo
# Si el puerto está ocupado, usar otro:
ngrok http 9000 --web-addr=localhost:4041
# Luego ir a: http://127.0.0.1:4041
```

### Problema 10: "ERR_NGROK_3200"

Causa: Ngrok Free plan tiene límite de conexiones o tiempo

Solución:
1. Cierra Ngrok (Ctrl+C)
2. Vuelve a iniciar:
   ```bash
   ngrok http 9000
   ```
3. Importante: La URL cambiará, actualiza application.yml y el webhook en MercadoPago

---

## LOGS Y DEPURACIÓN

### Niveles de Log Recomendados

```yaml
logging:
  level:
    com.zabora.subscription: DEBUG
    com.zabora.subscription.servicio.MercadoPagoServicio: DEBUG
    com.zabora.subscription.controlador.MercadoPagoWebhookController: DEBUG
    com.zabora.subscription.repositorio.AuthClient: DEBUG
    feign: DEBUG
    com.mercadopago: DEBUG
```

### Logs Esperados

#### Creación de Suscripción
```
═══════════════════════════════════════
CREANDO NUEVA SUSCRIPCIÓN
═══════════════════════════════════════
Usuario ID: 1
Plan ID: 2 (premium)
═══════════════════════════════════════

Suscripción creada: sub_abc123def456
Estado inicial: PENDIENTE_PAGO
```

#### Procesamiento de Webhook
```
═══════════════════════════════════════
WEBHOOK RECIBIDO DE MERCADOPAGO
═══════════════════════════════════════
Payload: {action=payment.updated, data={id=1234567890}, type=payment}
Tipo: payment
Accion: payment.updated
═══════════════════════════════════════

Payment ID detectado: 1234567890

═══════════════════════════════════════
DETALLES DEL PAGO
═══════════════════════════════════════
ID: 1234567890
Status: approved
Amount: 29900.0
Suscripción ID: sub_abc123def456
Usuario ID: 1
═══════════════════════════════════════

Pago encontrado en BD: pago_xyz789uvw123
Actualizando pago según estado de MercadoPago: approved
Pago APROBADO
Pago actualizado a COMPLETADO

═══════════════════════════════════════
ACTIVANDO SUSCRIPCIÓN
═══════════════════════════════════════
Suscripción ID: sub_abc123def456
Usuario ID: 1
Suscripción encontrada
   - Estado actual: PENDIENTE_PAGO
   - Usuario ID: 1
   - Plan: premium
Suscripción sub_abc123def456 activada exitosamente
Válida hasta: 2026-04-17T15:30:00
═══════════════════════════════════════

═══════════════════════════════════════
ACTUALIZANDO ROL EN AUTH SERVICE
═══════════════════════════════════════
POST http://localhost:8000/api/upgrade/premium/1
Rol PREMIUM actualizado en auth-service
═══════════════════════════════════════
```

---

## COMANDOS ÚTILES DE REFERENCIA

```bash
# Iniciar Subscription Service
cd C:\ZaboraServices\Zabora-Suscrip
mvn spring-boot:run

# Iniciar Ngrok
ngrok http 9000

# Ver Web Interface de Ngrok
start http://127.0.0.1:4040

# Configurar nuevo token de Ngrok
ngrok config add-authtoken TU_TOKEN

# Verificar estado del servicio
curl http://localhost:8004/actuator/health

# Ver métricas
curl http://localhost:8004/actuator/metrics

# Ver endpoints disponibles
curl http://localhost:8004/actuator/mappings

# Ver planes
curl http://localhost:9000/api/suscripciones/planes

# Ver estado de suscripción
curl -H "X-User-Id: 1" http://localhost:9000/api/suscripciones/estado

# Ver historial de pagos
curl -H "X-User-Id: 1" http://localhost:9000/api/pagos/historial

# Simular webhook
curl -X POST http://localhost:9000/api/webhooks/mercadopago \
  -H "Content-Type: application/json" \
  -d '{"action":"payment.updated","data":{"id":"1234567890"},"type":"payment"}'
```

---

## CONSIDERACIONES DE SEGURIDAD

1. Headers de autenticación: El API Gateway inyecta X-User-Id y X-User-Email desde el JWT
2. Validación de permisos: Los usuarios solo pueden ver/modificar sus propias suscripciones
3. Webhooks seguros: Validación de origen mediante secreto compartido
4. HTTPS en producción: Siempre usar HTTPS para comunicación externa

---

## DESPLIEGUE EN PRODUCCIÓN

### Configuración para Producción

```yaml
mercadopago:
  # Credenciales de PRODUCCIÓN
  access-token: APP_USR-8754802509768642-022816-29928cef3f32a3600cd6098f87947575-3233058352
  public-key: APP_USR-46460ebc-eb77-4630-a699-42155e7b3df4
  environment: production
  
  # URLs de producción
  success-url: https://zabora.com/home?payment=success
  failure-url: https://zabora.com/home?payment=failure
  pending-url: https://zabora.com/home?payment=pending
  
  webhook:
    secret: c4c69a70dcb99ebf7b530bfd4cd8080e49feed021d9bc56e5bd27d119b59eec5
    notification-url: https://api.zabora.com/api/webhooks/mercadopago
```

---

## RECURSOS ADICIONALES

- Documentación MercadoPago: https://www.mercadopago.com.co/developers
- Spring Boot: https://spring.io/projects/spring-boot
- Feign Client: https://cloud.spring.io/spring-cloud-openfeign/
- Ngrok: https://ngrok.com/docs

---
