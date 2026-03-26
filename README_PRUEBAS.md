# 📋 GUÍA DE EJECUCIÓN DE PRUEBAS AUTOMATIZADAS

## 🎯 OBJETIVO
Este documento explica cómo ejecutar las pruebas automatizadas con Rest Assured para los casos de prueba CP032 a CP039 del módulo Subscription Service.

## 📁 ESTRUCTURA DE ARCHIVOS

```
src/test/java/com/zabora/subscription/integration/
├── BaseSubscriptionTest.java           (Clase base con configuración común)
├── CP032_SeleccionMetodoPagoTest.java   (Selección de método de pago)
├── CP033_ConexionPseTest.java           (Conexión con servicio PSE)
├── CP034_ConexionTarjetaTest.java       (Conexión con servicio tarjeta)
├── CP035_RecolectarInformacionTest.java (Recolectar información suscripción)
├── CP036_CancelarSuscripcionTest.java   (Cancelar suscripción)
├── CP037_VerificarEstadoTest.java       (Verificar estado)
├── CP038_FinalizarPagoExitosoTest.java  (Finalizar pago exitoso)
├── CP039_ReportarIngresosTest.java      (Reportar ingresos)
├── TestSuiteRunner.java                 (Suite de pruebas completa)
└── README_PRUEBAS.md                    (Este archivo)
```

## 🔧 REQUISITOS PREVIOS

### 1. Backend Corriendo
Asegúrate de que el backend esté corriendo en el puerto 8004:
```bash
cd Zabora-Suscrip
java -jar target/subscription-service-1.0.0.jar
```

### 2. Base de Datos Configurada
- MySQL corriendo en localhost:3306
- Base de datos `zabora_subscriptions` creada
- Usuario y contraseña configurados en application.yml

## 🚀 EJECUCIÓN DE PRUEBAS

### Opción 1: Ejecutar Todas las Pruebas
```bash
cd Zabora-Suscrip
mvn test -Dtest=TestSuiteRunner
```

### Opción 2: Ejecutar Prueba Individual
```bash
# Ejecutar solo CP032
mvn test -Dtest=CP032_SeleccionMetodoPagoTest

# Ejecutar solo CP038
mvn test -Dtest=CP038_FinalizarPagoExitosoTest
```

### Opción 3: Ejecutar desde IDE
- Abre cualquier clase de prueba en tu IDE
- Haz clic derecho en la clase y selecciona "Run Tests"
- Los reportes se imprimirán en la consola

## 📊 FORMATO DE SALIDA

Cada prueba genera un reporte con este formato:

```
========================================
CASO DE PRUEBA: CP035 - Recolectar información de suscripción
========================================
Fecha ejecución: 25/03/2026 22:10
Responsable: Yuliana Yate

| Sub-caso | Resultado | Código HTTP | Datos clave |
|----------|-----------|-------------|-------------|
| CP035.1 - Premium exitoso | APROBO | 200 | ID: sub_xxx |
| CP035.2 - Gratuito exitoso | APROBO | 200 | Estado: ACTIVA |
| CP035.3 - Sin autenticación | APROBO | 401 | - |
| CP035.4 - Usuario ya activo | APROBO | 500 | Mensaje: "ya tiene" |
| CP035.5 - Plan inexistente | APROBO | 500 | Mensaje: "not found" |
| CP035.6 - Datos incompletos | APROBO | 400 | - |
| CP035.7 - Verificar persistencia | APROBO | 200 | Datos coinciden |

========================================
VEREDICTO GENERAL: APROBO
========================================
```

## 🎭 DATOS DE PRUEBA UTILIZADOS

### Usuarios de Prueba
- **Usuarios normales**: IDs 2000-2009, email `test@zabora.com`, role USER
- **Administradores**: IDs 990-999, email `admin@zabora.com`, role ADMIN

### Tokens de MercadoPago
- **Tarjeta exitosa**: `tok_test_visa`
- **Tarjeta rechazada**: `tok_test_rejected`
- **PSE**: Tokens generados dinámicamente

### Endpoints Probados
- `POST /api/suscripciones/suscribir` - Crear suscripción
- `POST /api/pagos/bricks/preference` - Crear preferencia de pago
- `POST /api/pagos/bricks/process` - Procesar pago
- `POST /api/suscripciones/cancelar/{id}` - Cancelar suscripción
- `GET /api/suscripciones/estado` - Verificar estado
- `GET /api/admin/reportes/ingresos-mensuales` - Reportes admin

## 🔍 VALIDACIONES REALIZADAS

### Autenticación y Autorización
- ✅ Requests con autenticación válida
- ✅ Requests sin autenticación (deben fallar con 401)
- ✅ Usuarios normales intentando acceder a endpoints admin (deben fallar con 403)
- ✅ Usuarios intentando acceder a recursos de otros usuarios

### Validaciones de Negocio
- ✅ Creación de suscripciones con planes válidos
- ✅ Intentos de crear suscripciones duplicadas
- ✅ Cancelación de suscripciones propias y ajenas
- ✅ Procesamiento de pagos con diferentes tokens

### Validaciones Técnicas
- ✅ Códigos de estado HTTP correctos
- ✅ Estructura de respuestas válida
- ✅ Persistencia de datos en base de datos

## 📋 CASOS DE PRUEBA DETALLADOS

### CP032 - Selección de Método de Pago
- Selección de tarjeta y PSE con autenticación
- Intentos sin autenticación
- Usuarios con suscripciones existentes
- Planes inexistentes

### CP033 - Conexión con PSE
- Creación de preferencias PSE
- Validación de autenticación y autorización
- Manejo de pagos pendientes existentes

### CP034 - Conexión con Tarjeta
- Creación de preferencias de tarjeta
- Validaciones similares a CP033 pero para tarjetas

### CP035 - Recolectar Información
- Creación de suscripciones premium y gratuitas
- Validación de datos incompletos
- Verificación de persistencia

### CP036 - Cancelar Suscripción
- Cancelación inmediata y diferida
- Intentos de cancelación no autorizados
- Cancelación de suscripciones inexistentes

### CP037 - Verificar Estado
- Consulta de estados de diferentes tipos de suscripción
- Validación de fechas y estructura de datos
- Acceso no autenticado

### CP038 - Finalizar Pago
- Procesamiento de pagos exitosos y rechazados
- Tokens de prueba de MercadoPago
- Validación de estados de pago

### CP039 - Reportar Ingresos
- Acceso a reportes administrativos
- Restricciones para usuarios normales
- Estructura de datos de reportes

## 🚨 SOLUCIÓN DE PROBLEMAS

### Error 401 No Autorizado
- Verifica que los headers de autenticación estén configurados
- Revisa que el ID de usuario sea válido

### Error 403 Prohibido
- Verifica los roles de usuario
- Asegúrate de que los usuarios normales no accedan a endpoints admin

### Error 500 Error del Servidor
- Revisa los logs del backend
- Verifica que la base de datos esté accesible
- Comprueba que los endpoints existan

### Error de Conexión
- Asegúrate de que el backend esté corriendo en el puerto 8004
- Verifica que no haya firewalls bloqueando la conexión

## 📈 RESULTADOS ESPERADOS

Al ejecutar todas las pruebas, deberías obtener:

1. **8 reportes individuales** - Uno por cada caso de prueba (CP032-CP039)
2. **Veredicto general** - APROBO/REPROBO para cada caso
3. **Detalles de ejecución** - Códigos HTTP y datos clave
4. **Logs detallados** - Para análisis de problemas

Los resultados te permitirán:
- ✅ Documentar el estado actual del sistema
- ✅ Identificar problemas de funcionamiento
- ✅ Generar evidencia para la documentación técnica
- ✅ Validar el cumplimiento de requisitos

---

**Nota**: Estas pruebas están diseñadas para ejecutarse en un entorno de prueba. No ejecutes en producción sin modificar los datos de prueba.
