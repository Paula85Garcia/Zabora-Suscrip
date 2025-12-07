Módulo de Suscripciones - Zabora
Descripción General

Microservicio de gestión de suscripciones para la plataforma Zabora, implementando un modelo Freemium con pagos manuales (tarjeta/PSE). El servicio maneja todo el ciclo de vida de suscripciones, verificación de estado para otros servicios y facturación.

Funcionalidades Implementadas
Funcionalidades Completas

Suscripción a planes (Free y Premium)

Cancelación de suscripción con verificación de reembolso (24h)

Verificación de estado para otros microservicios (Recipe Service, Auth Service)

Procesamiento de pagos manuales (Tarjeta de crédito y PSE)

Gestión de métodos de pago

Datos quemados para pruebas inmediatas

Autenticación básica (Spring Security)

Documentación API automática (Swagger/OpenAPI)

Próximas funcionalidades

Integración real con Stripe

Base de datos MySQL persistente

Facturación DIAN

Notificaciones por correo

Reportes administrativos

Stack Tecnológico
Dependencias principales (pom.xml)
<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Base de datos -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Utilidades -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.4.0</version>
</dependency>

<!-- Documentación -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>

Configuración de Autenticación
Usuarios disponibles
Usuario	Contraseña	Rol	Uso recomendado
admin	admin123	ADMIN	Operaciones administrativas
usuario	usuario123	USER	Operaciones de usuario normal
Configuración en application.properties
# Configuración básica
server.port=8080
server.servlet.context-path=/api

# Base de datos (H2 para desarrollo)
spring.datasource.url=jdbc:h2:mem:zaboradb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Stripe (modo prueba, usar variables de entorno)
stripe.clave.secreta=${STRIPE_SECRET_KEY}
stripe.clave.publica=${STRIPE_PUBLIC_KEY}


Importante: Nunca subas tus claves reales. Define las variables de entorno STRIPE_SECRET_KEY y STRIPE_PUBLIC_KEY en tu máquina o contenedor.

📊 Datos Quemados para Pruebas
Usuarios pre-cargados
{
  "usuario_001": {
    "plan": "premium",
    "estado": "ACTIVA",
    "metodos_pago": ["Visa 4242", "PSE Bancolombia"],
    "suscripcion_id": "sub_001"
  },
  "usuario_002": {
    "plan": "gratuito", 
    "estado": "ACTIVA",
    "metodos_pago": [],
    "suscripcion_id": "sub_002"
  }
}

Planes disponibles
Plan	Precio	Límites
Gratuito	$0 COP	2 condiciones médicas, 2 alergias, 1 preferencia, 7 ingredientes, 4 recetas favoritas
Premium	$29,900 COP	3 condiciones médicas, 4 alergias, 1 preferencia, 20 ingredientes, recetas favoritas ilimitadas
Headers para Pruebas
Headers requeridos para endpoints protegidos
Authorization: Basic YWRtaW46YWRtaW4xMjM=
X-Usuario-Id: [ID_DEL_USUARIO]
Content-Type: application/json  # Solo para POST/PUT

Tokens Basic Auth pre-generados

admin:admin123 → Basic YWRtaW46YWRtaW4xMjM=

usuario:usuario123 → Basic dXN1YXJpbzp1c3VhcmlvMTIz

Endpoints Disponibles
Públicos (sin autenticación)
GET  /api/suscripciones/datos-mock
GET  /api/suscripciones/verificar/{id}
GET  /api/suscripciones/planes
POST /api/pagos/pago-prueba
GET  /swagger-ui.html
GET  /h2-console

Protegidos (requieren autenticación)
POST /api/suscripciones/suscribir
POST /api/suscripciones/cancelar/{id}
GET  /api/suscripciones/estado
POST /api/pagos/procesar
GET  /api/pagos/metodos
POST /api/pagos/metodos

Ejemplos de Requests/Responses
Suscribirse a Premium
POST /api/suscripciones/suscribir
Headers:
  X-Usuario-Id: usuario_nuevo_001
  Authorization: Basic YWRtaW46YWRtaW4xMjM=
  Content-Type: application/json

Body:
{
  "nombrePlan": "premium",
  "tipoPago": "TARJETA_CREDITO",
  "tokenTarjetaPrueba": "tok_visa"
}

Response (200 OK):
{
  "exito": true,
  "mensaje": "Suscripción premium creada. Proceda con el pago.",
  "idSuscripcion": "sub_1733550000123",
  "plan": "premium",
  "estado": "PENDIENTE_PAGO",
  "requierePago": true
}

Procesar pago manual
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

Response (200 OK):
{
  "exito": true,
  "mensaje": "Pago procesado exitosamente",
  "idPago": "pago_123456789",
  "estado": "COMPLETADO",
  "monto": 29900.00,
  "moneda": "COP"
}

Cancelar suscripción
POST /api/suscripciones/cancelar/sub_001
Headers:
  X-Usuario-Id: usuario_001
  Authorization: Basic YWRtaW46YWRtaW4xMjM=

Response (200 OK):
{
  "exito": true,
  "mensaje": "Suscripción cancelada exitosamente",
  "idSuscripcion": "sub_001",
  "plan": "premium",
  "estado": "CANCELADA"
}

Tokens de Prueba Stripe
Token	Comportamiento	Uso recomendado
tok_visa	Pago exitoso	Flujos normales
tok_visa_chargeDeclined	Pago rechazado	Pruebas de error
tok_3ds	Requiere 3D Secure	Pruebas de autenticación
tok_pending	Pago pendiente	Pruebas de estados intermedios
Logs de la Aplicación

Mensajes de inicio:

Datos mock inicializados: 2 planes, 2 suscripciones
Datos de pago mock inicializados
Zabora Subscription Service iniciado en puerto 8080
Swagger UI disponible en: http://localhost:8080/swagger-ui.html
H2 Console disponible en: http://localhost:8080/h2-console


Errores comunes y soluciones:

Error	Causa probable	Solución
401 Unauthorized	Header Authorization vacío o incorrecto	Usar Basic YWRtaW46YWRtaW4xMjM=
400 Bad Request	Falta header X-Usuario-Id	Agregar X-Usuario-Id: [id_usuario]
400 Bad Request	Usuario ya tiene suscripción activa	Usar nuevo usuario_test_XXX
500 Internal Server Error	Suscripción no encontrada	Verificar combinación usuario/suscripción
Primeros pasos para debugging

Verificar que la aplicación esté corriendo:

curl http://localhost:8080/api/suscripciones/datos-mock


Probar autenticación básica:

curl -u admin:admin123 http://localhost:8080/api/suscripciones/estado -H "X-Usuario-Id: usuario_001"


Revisar logs de la aplicación para mensajes de error detallados.
