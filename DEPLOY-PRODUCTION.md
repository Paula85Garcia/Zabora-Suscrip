# Despliegue a producción — Subscription Service (Azure VM)

Guía orientada a cuando el **backend** corre en una **máquina virtual de Azure** (o similar), con **API Gateway**, **Consul** y **MySQL** accesibles desde esa red.

## Arquitectura esperada

```
Internet → (opcional) Azure Load Balancer / App Gateway
    → API Gateway (:9000) en VM o otra VM
    → Consul (:8500)
    → subscription-service (:8004) + auth-service + otros
    → MySQL (Azure Database for MySQL, VM dedicada o contenedor en red privada)
```

El **frontend** (Angular) debe apuntar al **Gateway** (no al puerto 8004 directo), salvo entornos de desarrollo.

## Qué cambiar respecto a local

| Área | Local | Producción |
|------|--------|------------|
| `MYSQL_URL` / host DB | `localhost` | Host interno de Azure (FQDN o IP privada), **SSL recomendado** (`sslMode=REQUIRED` o según proveedor) |
| `CONSUL_HOST` | `localhost` | IP/DNS de la VM o servicio donde corre Consul |
| `MP_WEBHOOK_NOTIFICATION_URL` | ngrok | `https://tu-dominio.com/api/webhooks/mercadopago` vía **Gateway** (misma URL pública) |
| `MP_SUCCESS_URL` / failure / pending | `http://localhost:4200/...` | URLs **HTTPS** del front en producción |
| `JWT_SECRET` | dev | **Idéntico** al `jwt.secret` de auth-service y `security.jwt.secret-key` del Gateway |
| `mercadopago.environment` | `test` | `production` solo con credenciales **producción** de Mercado Pago |
| CORS / cookies | relajado | Configurar orígenes reales del front en el Gateway si habilitas `globalcors` |
| Logs / SQL | `DEBUG`, `show-sql: true` | `INFO`, `show-sql: false` |
| `JPA_DDL_AUTO` | `update` | Valor acordado con el equipo (`validate` o migraciones Flyway/Liquibase en entornos maduros) |

## Azure VM: aspectos prácticos

1. **Puertos NSG (Network Security Group)**  
   - Abre solo lo necesario: típicamente **443** (y 80 si rediriges) hacia el Gateway o reverse proxy.  
   - **No** expongas MySQL ni Consul a Internet; solo red virtual / subred interna.

2. **Reverse proxy (recomendado)**  
   - Nginx o Caddy delante del Gateway: TLS (Let’s Encrypt), `proxy_pass` a `http://127.0.0.1:9000`.  
   - El webhook de Mercado Pago debe llegar como `POST https://tudominio/api/webhooks/mercadopago` (sin cortar el cuerpo).

3. **Servicio systemd (ejemplo)**  
   - Empaeta el JAR (`mvn package -DskipTests`) y usa `EnvironmentFile=/etc/zabora/subscription.env` con permisos `chmod 600` y propietario root o el usuario del servicio.  
   - Variables: las mismas que en `.env.example` (ver README del módulo).

4. **Salud y arranque**  
   - Arranca **Consul** y **MySQL** antes que los microservicios.  
   - Comprueba en Consul que el servicio aparece como **`zabora-subscription-service`** (debe coincidir con `lb://zabora-subscription-service` en el Gateway).

5. **Mercado Pago**  
   - En el panel de MP, URL de notificación = la pública del webhook (vía Gateway).  
   - Credenciales de **producción** distintas de prueba; rota si alguna vez se filtraron.

6. **Correo (`spring.mail`)**  
   - Si usas Gmail, contraseña de aplicación; en producción suele preferirse SendGrid, SES, etc.

## Checklist rápido

- [ ] Variables de entorno definidas en la VM (sin commitear).  
- [ ] JWT igual en Gateway, auth-service y subscription-service.  
- [ ] Webhook MP público en Gateway (`/api/webhooks/**`) y HTTPS válido.  
- [ ] Front en Angular con `environment.prod` apuntando al dominio del Gateway.  
- [ ] Firewall Azure + SO alineados con puertos expuestos.  
- [ ] Backups de MySQL y plan de restauración.

## Credenciales: cómo entregarlas (equipo / VM)

**No** uses el repositorio Git, capturas en Slack sin cifrar ni el cuerpo del correo en texto plano para tokens de MP o JWT.

Opciones razonables:

1. **Azure Key Vault** + identidad administrada en la VM para leer secretos al arrancar (o inyectar en CI/CD).  
2. **Variables en pipeline** (GitHub Actions / Azure DevOps) que escriben un `EnvironmentFile` en despliegue por SSH, sin dejar el secreto en logs.  
3. **Gestor de secretos** (1Password, Bitwarden) con enlace de un solo uso o vault compartido al equipo.  
4. **SSH a la VM**: archivo en `/etc/zabora/*.env`, `chmod 600`, dueño del usuario del servicio.  
5. Rotación: si un secreto estuvo en un `application.yml` versionado o en chat, **rótalo** (MP token, contraseña MySQL, app password de correo, webhook secret).

---

Más detalle de integración con Gateway y errores frecuentes: [README.md](./README.md) y [api-gateway/README.md](../api-gateway/README.md).
