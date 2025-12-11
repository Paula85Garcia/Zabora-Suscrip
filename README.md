Microservicio para gestión de suscripciones, pagos y reportes de Zabora.

---

# **2. Ejecutar la Aplicación**

```bash
mvn spring-boot:run
```

Si inicia correctamente:

```
Tomcat started on port(s): 8080
H2 console available at '/h2-console'
Swagger UI available at '/swagger-ui.html'
```

---

# **3. URLs Clave**

| Recurso    | URL                                                                            |
| ---------- | ------------------------------------------------------------------------------ |
| API Base   | [http://localhost:8080/api](http://localhost:8080/api)                         |
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| H2 Console | [http://localhost:8080/h2-console](http://localhost:8080/h2-console)           |

---

# **4. Usuarios de Prueba**

| Usuario          | Password   |
| ---------------- | ---------- |
| admin            | admin123   |
| usuario          | usuario123 |
| user_premium_001 | —          |
| user_free_002    | —          |
| user_pending_003 | —          |

---

# **5. Endpoints para Pruebas**

## **5.1 Check**

* `GET /api/suscripciones/planes` → Listar planes disponibles
* `GET /swagger-ui.html` → Abrir Swagger UI
* `GET /h2-console` → Abrir consola H2

---

## **5.2 Autenticación**

* `POST /api/auth/login` → Obtener token JWT
* `GET /api/auth/validate` → Validar token JWT

---

## **5.3 Planes de Suscripción**

* `GET /api/suscripciones/planes` → Obtener todos los planes

---

## **5.4 Suscripciones (Usuario)**

* `POST /api/suscripciones/suscribir` → Suscribirse a un plan (gratuito o premium)
* `GET /api/suscripciones/verificar/{usuario}` → Verificar suscripción de un usuario
* `GET /api/suscripciones/estado` → Obtener estado de suscripción (requiere Basic Auth y header `X-Usuario-Id`)
* `POST /api/suscripciones/cancelar/{idSuscripcion}` → Cancelar suscripción

---

## **5.5 Pagos y Métodos de Pago**

* `GET /api/pagos/metodos` → Listar métodos de pago del usuario
* `POST /api/pagos/registrar` → Registrar pago de suscripción
* `GET /api/pagos/estado/{idSuscripcion}` → Verificar estado del pago

---

## **5.6 Administración y Logs (Admin)**

* `GET /api/admin/estadisticas` → Estadísticas generales
* `GET /api/admin/reportes/mensual?anio=YYYY&mes=MM` → Reporte mensual
* `GET /api/admin/reportes/diario?dias=N` → Reporte diario
* `GET /api/admin/suscripciones/por-vencer?dias=N` → Suscripciones próximas a vencer
* `GET /api/admin/usuarios/top-ingresos?limite=N` → Top usuarios por ingresos
* `GET /api/admin/logs` → Ver logs del sistema

---

# **6. Consola H2**

URL: `http://localhost:8080/h2-console`

* User: `sa`
* Password: (vacío)

Ejemplos de consultas:

```sql
SELECT * FROM PLANES_SUSCRIPCION;
SELECT * FROM SUSCRIPCIONES_USUARIOS;
SELECT * FROM PAGOS;
SELECT * FROM METODOS_PAGO;
SELECT * FROM LOGS_SUSCRIPCIONES;
```

