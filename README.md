# Zabora Subscription Service - Entorno de Pruebas

![Java](https://img.shields.io/badge/Java-17-blue) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green) ![H2 Database](https://img.shields.io/badge/H2-Database-lightgrey) ![License](https://img.shields.io/badge/License-MIT-yellow)

Microservicio de suscripciones para la plataforma Zabora, orientado solo a pruebas: datos mock, pagos simulados y autenticación básica.


---

## Contenido

<details>
<summary>Usuarios de Prueba</summary>

| Usuario | Contraseña | Rol   | Uso recomendado               |
| ------- | ---------- | ----- | ----------------------------- |
| admin   | admin123   | ADMIN | Operaciones administrativas   |
| usuario | usuario123 | USER  | Operaciones de usuario normal |

**Headers requeridos para endpoints protegidos**

```
Authorization: Basic YWRtaW46YWRtaW4xMjM=  # admin
X-Usuario-Id: [ID_DEL_USUARIO]
Content-Type: application/json  # Para POST/PUT
```

</details>

<details>
<summary>Datos Mock</summary>

### Suscripciones

```json
{
  "usuario_001": {"plan":"premium","estado":"ACTIVA","metodos_pago":["Visa 4242","PSE Bancolombia"],"suscripcion_id":"sub_001"},
  "usuario_002": {"plan":"gratuito","estado":"ACTIVA","metodos_pago":[],"suscripcion_id":"sub_002"}
}
```

### Planes

| Plan     | Precio      | Límites principales                                                                             |
| -------- | ----------- | ----------------------------------------------------------------------------------------------- |
| Gratuito | $0 COP      | 2 condiciones médicas, 2 alergias, 1 preferencia, 7 ingredientes, 4 recetas favoritas           |
| Premium  | $29,900 COP | 3 condiciones médicas, 4 alergias, 1 preferencia, 20 ingredientes, recetas favoritas ilimitadas |

</details>

<details>
<summary>Endpoints de Prueba</summary>

### Públicos

```
GET  /api/suscripciones/datos-mock
GET  /api/suscripciones/planes
POST /api/pagos/pago-prueba
GET  /swagger-ui.html
GET  /h2-console
```

### Protegidos (requieren autenticación)

```
POST /api/suscripciones/suscribir
POST /api/suscripciones/cancelar/{id}
GET  /api/suscripciones/estado
POST /api/pagos/procesar
GET  /api/pagos/metodos
POST /api/pagos/metodos
```

</details>

<details>
<summary>Tokens de Prueba Stripe</summary>

| Token                   | Comportamiento     | Uso recomendado                |
| ----------------------- | ------------------ | ------------------------------ |
| tok_visa                | Pago exitoso       | Flujos normales                |
| tok_visa_chargeDeclined | Pago rechazado     | Pruebas de error               |
| tok_3ds                 | Requiere 3D Secure | Pruebas de autenticación       |
| tok_pending             | Pago pendiente     | Pruebas de estados intermedios |

> No usar claves reales de Stripe. Para pruebas, define variables de entorno:
> STRIPE_SECRET_KEY y STRIPE_PUBLIC_KEY con valores de prueba.

</details>

<details>
<summary>Ejemplos de Requests</summary>

### Suscribirse a Premium

```http
POST /api/suscripciones/suscribir
Headers:
  X-Usuario-Id: usuario_001
  Authorization: Basic YWRtaW46YWRtaW4xMjM=
  Content-Type: application/json

Body:
{
  "nombrePlan": "premium",
  "tipoPago": "TARJETA_CREDITO",
  "tokenTarjetaPrueba": "tok_visa"
}
```

### Procesar pago manual

```http
POST /api/pagos/procesar
Headers:
  X-Usuario-Id: usuario_001
  Authorization: Basic YWRtaW46YWRtaW4xMjM=
  Content-Type: application/json

Body:
{
  "idSuscripcion": "sub_001",
  "monto": 29900.00,
  "tipoPago": "TARJETA_CREDITO",
  "tokenTarjetaPrueba": "tok_visa"
}
```

</details>

<details>
<summary>Primeros pasos</summary>

1. Verificar que la aplicación esté corriendo:

```bash
curl http://localhost:8080/api/suscripciones/datos-mock
```

2. Probar autenticación básica:

```bash
curl -u admin:admin123 http://localhost:8080/api/suscripciones/estado -H "X-Usuario-Id: usuario_001"
```

3. Revisar logs para mensajes de error detallados.

</details>

<details>
<summary>Recursos adicionales</summary>

* Swagger UI: `http://localhost:8080/swagger-ui.html`
* H2 Console: `http://localhost:8080/h2-console`

</details>

