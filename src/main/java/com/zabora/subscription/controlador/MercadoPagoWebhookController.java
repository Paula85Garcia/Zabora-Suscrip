package com.zabora.subscription.controlador;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.AuthClient;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/mercadopago")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private final PagoRepositorio pagoRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;
    private final AuthClient authClient;

    @PostMapping
    public ResponseEntity<String> recibirWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestParam(required = false) Map<String, String> params
    ) {
        try {
            log.info("Webhook recibido de MercadoPago");
            log.info("Payload: {}", payload);

            String type = (String) payload.get("type");
            String action = (String) payload.get("action");

            log.info("Tipo: {}", type);
            log.info("Accion: {}", action);

            if ("payment".equals(type)) {
                procesarNotificacionPago(payload);
            } else {
                log.info("Tipo de notificacion no procesada: {}", type);
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error procesando webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok("ERROR");
        }
    }

    private void procesarNotificacionPago(Map<String, Object> payload) {
        try {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String paymentIdStr = (String) data.get("id");
            Long paymentId = Long.parseLong(paymentIdStr);

            log.info("Procesando pago ID: {}", paymentId);

            PaymentClient client = new PaymentClient();
            Payment payment = client.get(paymentId);

            log.info("Detalles del pago:");
            log.info("ID: {}", payment.getId());
            log.info("Status: {}", payment.getStatus());
            log.info("Amount: {}", payment.getTransactionAmount());

            Map<String, Object> metadata = payment.getMetadata();
            String suscripcionId = (String) metadata.get("suscripcion_id");
            String usuarioId = (String) metadata.get("usuario_id");

            log.info("Suscripcion ID: {}", suscripcionId);
            log.info("Usuario ID: {}", usuarioId);

            Pago pagoLocal = pagoRepositorio.findBySuscripcionIdAndEstado(
                    suscripcionId,
                    EstadoPago.PENDIENTE
            ).orElse(null);

            if (pagoLocal == null) {
                log.warn("No se encontro pago pendiente para suscripcion: {}", suscripcionId);
                return;
            }

            log.info("Pago encontrado en BD: {}", pagoLocal.getId());

            String status = payment.getStatus();
            actualizarPagoSegunEstado(pagoLocal, status, payment, suscripcionId);

        } catch (MPException | MPApiException e) {
            log.error("Error consultando pago en MercadoPago: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error procesando notificacion de pago: {}", e.getMessage(), e);
        }
    }

    private void actualizarPagoSegunEstado(Pago pago, String mpStatus, Payment mpPayment, String suscripcionId) {
        log.info("Actualizando pago segun estado de MercadoPago: {}", mpStatus);

        switch (mpStatus) {
            case "approved":
                log.info("Pago APROBADO");

                pago.setEstado(EstadoPago.COMPLETADO);
                pago.setIdIntentoPago(mpPayment.getId().toString());
                pago.setCodigoAutorizacion(mpPayment.getAuthorizationCode());
                pago.setFechaPago(LocalDateTime.now());
                pagoRepositorio.save(pago);
                log.info("Pago actualizado a COMPLETADO");

                activarSuscripcion(suscripcionId);
                break;

            case "pending":
            case "in_process":
                log.info("Pago PENDIENTE");
                break;

            case "rejected":
            case "cancelled":
                log.info("Pago FALLIDO");
                pago.setEstado(EstadoPago.FALLIDO);
                pagoRepositorio.save(pago);
                break;

            case "refunded":
            case "charged_back":
                log.info("Pago REEMBOLSADO");
                pago.setEstado(EstadoPago.REEMBOLSADO);
                pagoRepositorio.save(pago);
                break;

            default:
                log.warn("Estado de MercadoPago desconocido: {}", mpStatus);
        }
    }

    private void activarSuscripcion(String suscripcionId) {
        try {
            Optional<UsuarioSuscripcion> suscripcionOpt = suscripcionRepositorio.findById(suscripcionId);

            if (suscripcionOpt.isEmpty()) {
                log.error("Suscripcion no encontrada: {}", suscripcionId);
                return;
            }

            UsuarioSuscripcion suscripcion = suscripcionOpt.get();

            if (suscripcion.getEstado() != EstadoSuscripcion.PENDIENTE_PAGO) {
                log.warn("Suscripcion no esta en PENDIENTE_PAGO: {}", suscripcion.getEstado());
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
            suscripcion.setInicioPeriodoActual(now);
            suscripcion.setFinPeriodoActual(now.plusDays(30));
            suscripcion.setFechaActualizacion(now);

            suscripcionRepositorio.save(suscripcion);

            log.info("Suscripcion {} activada exitosamente", suscripcionId);

            String usuarioId = suscripcion.getUsuarioId();

            try {
                authClient.actualizarRolPremium(usuarioId);
                log.info("Rol PREMIUM actualizado en auth-service para usuario: {}", usuarioId);
            } catch (Exception e) {
                log.error("Error actualizando rol en auth-service: {}", e.getMessage(), e);
            }
            log.info("Usuario: {}", suscripcion.getUsuarioId());
            log.info("Valida hasta: {}", suscripcion.getFinPeriodoActual());

        } catch (Exception e) {
            log.error("Error activando suscripcion: {}", e.getMessage(), e);
        }
    }

    @GetMapping
    public ResponseEntity<String> verificarWebhook() {
        log.info("Verificacion de webhook");
        return ResponseEntity.ok("Webhook activo");
    }
}
