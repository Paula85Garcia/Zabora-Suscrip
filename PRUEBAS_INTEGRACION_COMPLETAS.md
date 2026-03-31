# PRUEBAS DE INTEGRACIÓN COMPLETAS - SUSCRIPTION SERVICE

## Overview

Este documento describe el conjunto completo de pruebas de integración generadas para validar el flujo de suscripciones del backend refactorizado. Las pruebas cubren todos los casos de prueba CP032-CP039 y simulación de webhooks de MercadoPago.

## Arquitectura del Backend Refactorizado

- **BricksPaymentController**: Maneja pagos directos con token de tarjeta
- **WebhookController**: Centraliza notificaciones de MercadoPago
- **BricksPaymentServicio**: Integra pago + activación en una transacción
- **AuthServicio**: Maneja comunicación con auth-service via Feign
- **WebhookPagoServicio**: Procesa eventos asíncronos de MP

## Estructura de Pruebas

```
src/test/java/com/zabora/subscription/integration/
├── BaseSubscriptionTest.java           (Configuración base)
├── CP032_SeleccionMetodoPagoTest.java   (Selección método pago)
├── CP033_FlujoPagoTarjetaTest.java      (Pago exitoso con tarjeta)
├── CP034_FlujoPagoPSETest.java          (Pago con PSE - simulado)
├── CP035_CreacionSuscripcionTest.java   (Creación suscripción)
├── CP036_CancelacionSuscripcionTest.java (Cancelación)
├── CP037_VerificacionEstadoTest.java    (Verificación estado)
├── CP038_ProcesamientoPagoTest.java     (Procesamiento pago)
├── CP039_ReportesAdminTest.java         (Reportes admin)
└── WebhookSimulationTest.java           (Simulación webhook)
```

## Configuración Base (BaseSubscriptionTest.java)

### Características
- `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`
- `@ActiveProfiles("test")`
- Puerto aleatorio para evitar conflictos
- Configuración de RestAssured con logs
- Métodos helpers para autenticación y creación de datos

### Métodos Helpers Disponibles
```java
// Autenticación
authenticatedRequest(userId, email, role)  // Headers X-User-Id, X-User-Email, X-User-Role
adminRequest(userId)                      // Rol ADMIN
userRequest(userId)                       // Rol USER
unauthenticatedRequest()                  // Sin headers

// Creación de datos de prueba
crearSuscripcionRequest(plan, tipoPago, usuarioId)
crearPagoRequest(suscripcionId, tipoPago, token)
crearPreferenciaRequest(suscripcionId, tipoPago)
crearCancelacionRequest(inmediata)

// Reportes
imprimirReporte(casoPrueba, resultados)
```

### Tokens de Prueba MercadoPago
```java
TOKEN_VISA_EXITOSO = "tok_test_visa"
TOKEN_VISA_RECHAZADO = "tok_test_rejected"
BANCO_PSE_BANCOLOMBIA = "1022"
```

## Casos de Prueba Detallados

### CP032 - Selección de Método de Pago
**Endpoint**: `POST /api/suscripciones/suscribir`

#### Sub-casos validados:
- **CP032.1**: Tarjeta de crédito con usuario autenticado → 200 OK, estado PENDIENTE_PAGO
- **CP032.2**: PSE con usuario autenticado → 200 OK, estado PENDIENTE_PAGO
- **CP032.3**: Sin autenticación → 401/403
- **CP032.4**: Usuario ya con suscripción activa → 400/500 error
- **CP032.5**: Plan inexistente → 400/500 error
- **CP032.6**: Datos incompletos (nombrePlan vacío) → 400

### CP033 - Flujo Pago Tarjeta (COMPLETO)
**Endpoints**: 
- `POST /api/suscripciones/suscribir` → crear suscripción
- `POST /api/pagos/bricks/pay` → procesar pago con token

#### Sub-casos validados:
- **CP033.1**: Pago exitoso → 200 OK, success true, suscripción ACTIVA
- **CP033.2**: Pago rechazado → 422 UNPROCESSABLE_ENTITY, error con statusDetail
- **CP033.3**: Pago con suscripción inexistente → 400/500
- **CP033.4**: Pago sin autenticación → 401
- **CP033.5**: Pago con suscripción ya activa → 400
- **CP033.6**: Pago con monto incorrecto → 422/400

#### Body esperado para pago:
```json
{
  "token": "tok_test_visa",
  "paymentMethodId": "visa",
  "issuerId": "24",
  "installments": 1,
  "payerEmail": "test@zabora.com",
  "externalReference": "uuid-de-suscripcion",
  "transactionAmount": 29900.00,
  "description": "Suscripcion Premium Zabora"
}
```

### CP034 - Flujo Pago PSE
**Simulación**: PSE requiere redirección al banco, el webhook confirma después

#### Sub-casos validados:
- **CP034.1**: Crear suscripción premium → 200 OK, estado PENDIENTE_PAGO
- **CP034.2**: Procesar pago PSE (endpoint unificado) → verifica que acepta PSE
- **CP034.3**: Simular webhook con pago PSE aprobado → activación exitosa
- **CP034.4**: Simular webhook con pago PSE rechazado → suscripción permanece PENDIENTE

### CP035 - Creación de Suscripción
**Endpoint**: `POST /api/suscripciones/suscribir`

#### Sub-casos validados:
- **CP035.1**: Plan premium con usuario autenticado → 200, estado PENDIENTE_PAGO, requierePago=true
- **CP035.2**: Plan gratuito con usuario autenticado → 200, estado ACTIVA, requierePago=false
- **CP035.3**: Verificar persistencia en BD → consulta estado y valida
- **CP035.4**: Intentar crear segunda suscripción activa → error
- **CP035.5**: Plan no existe → error

### CP036 - Cancelación de Suscripción
**Endpoint**: `POST /api/suscripciones/cancelar/{suscripcionId}?inmediata=true/false`

#### Sub-casos validados:
- **CP036.1**: Cancelación inmediata → estado CANCELADA
- **CP036.2**: Cancelación al final del período → cancelarAlFinalPeriodo=true
- **CP036.3**: Cancelar suscripción inexistente → 400/500
- **CP036.4**: Cancelar suscripción de otro usuario → 403
- **CP036.5**: Cancelar suscripción ya cancelada → error
- **CP036.6**: Cancelar sin autenticación → 401

### CP037 - Verificación de Estado
**Endpoint**: `GET /api/suscripciones/estado`

#### Sub-casos validados:
- **CP037.1**: Usuario con suscripción ACTIVA → retorna plan premium, fechas, días restantes
- **CP037.2**: Usuario con suscripción PENDIENTE_PAGO → estado PENDIENTE_PAGO
- **CP037.3**: Usuario sin suscripción → plan gratuito, estado SIN_SUSCRIPCION
- **CP037.4**: Sin autenticación → 401

#### Campos validados en respuesta:
- `id`, `estado`, `nombrePlan`, `fechaCreacion`, `fechaInicio`, `fechaExpiracion`
- `diasRestantes`, `requierePago`, `usuarioId`, `tipoPago`

### CP038 - Procesamiento de Pago (Endpoint Bricks)
**Endpoint**: `POST /api/pagos/bricks/pay`

#### Sub-casos validados:
- **CP038.1**: Pago exitoso con token válido → 200, success true, suscripción ACTIVA
- **CP038.2**: Pago rechazado → 422, error con statusDetail traducido
- **CP038.3**: Pago sin token → 400 validation error
- **CP038.4**: Pago con externalReference inválido → 400
- **CP038.5**: Pago sin autenticación → 401

### CP039 - Reportes Administrativos
**Endpoints**:
- `GET /api/admin/dashboard`
- `GET /api/admin/suscripciones/activas`
- `GET /api/admin/pagos/recientes`
- `GET /api/admin/reportes/ingresos-mensuales`
- `GET /api/admin/usuarios/premium`

#### Sub-casos validados:
- **CP039.1**: Admin autenticado obtiene dashboard → 200, estructura válida
- **CP039.2**: Admin obtiene suscripciones activas → 200, lista no vacía
- **CP039.3**: Admin obtiene pagos recientes → 200
- **CP039.4**: Admin obtiene ingresos mensuales → 200, formato correcto
- **CP039.5**: Admin obtiene usuarios premium → 200
- **CP039.6**: Usuario normal accede a endpoint admin → 403

### WebhookSimulationTest
**Endpoint**: `POST /api/webhooks/mercadopago`

#### Payload MP real:
```json
{
  "type": "payment",
  "data": { "id": "1234567890" }
}
```

#### Sub-casos validados:
- **Webhook.1**: Webhook con payment ID válido y externalReference correcto → procesa, suscripción se activa
- **Webhook.2**: Webhook duplicado (mismo payment ID) → idempotente, no duplica activación
- **Webhook.3**: Webhook con payment ID inexistente en MP → error manejado, responde 200
- **Webhook.4**: Webhook con tipo diferente a "payment" → ignorado, responde 200
- **Webhook.5**: Verificar que webhook no reintenta (siempre responde 200)

## Configuración de Pruebas

### application-test.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  cloud:
    consul:
      enabled: false

server:
  port: 0

mercadopago:
  access-token: TEST_TOKEN
  public-key: TEST_PUBLIC_KEY
  environment: test

email:
  enabled: false

# Deshabilitar Feign para pruebas (mockear)
feign:
  client:
    config:
      default:
        url: http://localhost:9999
```

## Ejecución de Pruebas

### Compilar y ejecutar todas las pruebas
```bash
mvn clean test -Dtest=*IntegrationTest
```

### Ejecutar pruebas específicas
```bash
mvn test -Dtest=CP033_FlujoPagoTarjetaTest
mvn test -Dtest=CP039_ReportesAdminTest
mvn test -Dtest=WebhookSimulationTest
```

### Generar reporte de pruebas
```bash
mvn surefire-report:report
```

### Ejecutar con script automatizado
```bash
# En Windows
run_all_integration_tests.bat

# En Linux/Mac
chmod +x run_all_integration_tests.sh
./run_all_integration_tests.sh
```

## Formato de Reporte en Consola

Cada test genera un reporte detallado:

```
========================================
CASO DE PRUEBA: CP033 - Flujo Pago Tarjeta
========================================
Fecha ejecución: 28/03/2026 15:30
Responsable: Yuliana Yate

| Sub-caso | Resultado | Código HTTP | Datos clave |
|----------|-----------|-------------|-------------|
| CP033.1 - Pago exitoso | APROBO | 200 | Suscripción ACTIVA, mpPaymentId: 12345 |
| CP033.2 - Pago rechazado | APROBO | 422 | statusDetail: cc_rejected_insufficient_amount |
| CP033.3 - Suscripción inexistente | APROBO | 400 | "Suscripcion no encontrada" |
| CP033.4 - Sin autenticación | APROBO | 401 | - |
| CP033.5 - Suscripción ya activa | APROBO | 400 | "ya esta activa" |
| CP033.6 - Monto incorrecto | APROBO | 400/422 | - |

========================================
VEREDICTO GENERAL: APROBO
========================================
```

## Datos Obtenidos para Documentación

De cada ejecución se extrae:

- **Códigos HTTP reales** para cada sub-caso
- **Body de respuesta** (estructura exacta)
- **IDs generados** (suscripción, pago)
- **Fechas** (fecha_creacion, fecha_inicio, fecha_expiracion)
- **Mensajes de error exactos**
- **Tiempos de respuesta** (para rendimiento)
- **Estructura completa** de respuesta de cada endpoint

## Entregables Finales

1. **Todos los archivos de prueba** generados y ejecutables
2. **Reporte de resultados** con cada sub-caso detallado
3. **Evidencia de ejecución** (logs, capturas)
4. **Documentación actualizada** con resultados reales
5. **Lista de incidencias** (si algún test falla, documentado)

## Dependencias Requeridas

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.3.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

## Consideraciones Adicionales

1. **Base de datos H2 en memoria** para pruebas aisladas
2. **Puertos aleatorios** para evitar conflictos
3. **Mock de servicios externos** (MercadoPago, Auth-service)
4. **Logs detallados** para depuración
5. **Reportes HTML** generados automáticamente
6. **Validación de estructura** de respuestas JSON
7. **Pruebas de idempotencia** para webhooks
8. **Validación de seguridad** (autenticación y autorización)

## Flujo Completo Validado

El conjunto de pruebas valida el flujo completo:

1. **Login** → Autenticación vía headers
2. **Suscripción** → Creación de suscripción premium/gratuita
3. **Pago** → Procesamiento con tarjeta/PSE
4. **Activación** → Webhook confirma y activa
5. **Verificación** → Consulta de estado y fechas
6. **Reportes** → Dashboard administrativo
7. **Cancelación** → Inmediata o fin de período

---

**Responsable**: Yuliana Yate  
**Fecha de creación**: 28/03/2026  
**Versión**: 1.0.0
