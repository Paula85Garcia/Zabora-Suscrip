package com.zabora.subscription.controlador;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.zabora.subscription.servicio.PagoServicio;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Recibe las notificaciones de MercadoPago cuando un pago cambia de estado.
 *
 * MercadoPago envia:
 *   POST /api/webhooks/mercadopago
 *   Body: { "type": "payment", "data": { "id": "1234567890" } }
 *
 * Siempre responde 200 OK para evitar reintentos.
 */
@RestController
@RequestMapping("/api/webhooks/mercadopago")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final PagoServicio pagoServicio;

    @PostMapping
    public ResponseEntity<String> recibirWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Webhook recibido: {}", payload);
        try {
            String type = (String) payload.get("type");
            if ("payment".equals(type)) {
                procesarNotificacionPago(payload);
            } else {
                log.info("Tipo de notificacion ignorada: {}", type);
            }
        } catch (Exception e) {
            log.error("Error procesando webhook: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok("OK");
    }

    @GetMapping
    public ResponseEntity<String> verificar() {
        return ResponseEntity.ok("Webhook activo");
    }

    private void procesarNotificacionPago(Map<String, Object> payload) {
        Object dataObj = payload.get("data");
        if (!(dataObj instanceof Map)) {
            log.warn("Webhook sin campo 'data' valido");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) dataObj;
        String paymentIdStr = String.valueOf(data.get("id"));

        if (paymentIdStr == null || "null".equals(paymentIdStr)) {
            log.warn("Webhook sin payment ID");
            return;
        }

        log.info("Procesando pago ID: {}", paymentIdStr);

        try {
            // Verificar si es un ID numérico (de MercadoPago) o es el ID de suscripción
            if (paymentIdStr.startsWith("sub_")) {
                // Es un ID de suscripción, no un payment_id de MercadoPago
                log.info("ID de suscripción recibido, no es un payment_id de MercadoPago. Esperando webhook real.");
                return;
            }
            
            Long mpPaymentId = Long.parseLong(paymentIdStr);
            
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(mpPaymentId);
            log.info("Pago de MP - Status: {}, Monto: {}", payment.getStatus(), payment.getTransactionAmount());

            Map<String, Object> metadata = payment.getMetadata();
            if (metadata == null) {
                log.warn("Pago {} sin metadata. No se puede identificar la suscripcion.", paymentIdStr);
                return;
            }

            String suscripcionId = (String) metadata.get("suscripcion_id");
            Integer usuarioId = extraerUsuarioId(metadata.get("usuario_id"));

            if (suscripcionId == null || usuarioId == null) {
                log.warn("Metadata incompleta - suscripcionId: {}, usuarioId: {}", suscripcionId, usuarioId);
                return;
            }

            switch (payment.getStatus()) {
                case "approved" -> {
                    log.info("Pago aprobado - activando suscripcion: {}", suscripcionId);
                    pagoServicio.activarSuscripcionPorPago(suscripcionId, usuarioId, payment);
                }
                case "rejected", "cancelled" -> {
                    log.info("Pago rechazado/cancelado - marcando fallido: {}", suscripcionId);
                    pagoServicio.marcarPagoFallido(suscripcionId);
                }
                case "pending", "in_process" ->
                    log.info("Pago en proceso - esperando: {}", paymentIdStr);
                default ->
                    log.warn("Estado no reconocido: {}", payment.getStatus());
            }

        } catch (NumberFormatException e) {
            log.warn("ID de pago no es numérico (puede ser ID de suscripción): {}", paymentIdStr);
        } catch (MPApiException e) {
            log.error("Error API MercadoPago - Status: {}, Body: {}",
                e.getStatusCode(), e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("Error SDK MercadoPago: {}", e.getMessage());
        }
    }

    private Integer extraerUsuarioId(Object valor) {
        if (valor == null) return null;
        try {
            if (valor instanceof Integer) return (Integer) valor;
            if (valor instanceof Double) return ((Double) valor).intValue();
            String str = String.valueOf(valor);
            return str.contains(".") ? Double.valueOf(str).intValue() : Integer.parseInt(str);
        } catch (NumberFormatException e) {
            log.error("No se pudo convertir usuario_id: {}", valor);
            return null;
        }
    }
}