package com.zabora.subscription.controlador;

import com.zabora.subscription.servicio.WebhookPagoServicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Receptor de notificaciones de MercadoPago.
 *
 * REGLAS:
 *  1. Siempre responde 200 OK para evitar reintentos de MP que saturarian el servicio.
 *  2. Toda la logica de negocio vive en WebhookPagoServicio.
 *  3. Este controller es sin estado y reentrante.
 *
 * REEMPLAZA los dos MercadoPagoWebhookController que habia en el proyecto.
 *
 * Payload tipico de MP:
 *  { "type": "payment", "action": "payment.updated", "data": { "id": "1234567890" } }
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/mercadopago")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookPagoServicio webhookPagoServicio;

    @PostMapping
    public ResponseEntity<String> recibirWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestParam(value = "type", required = false) String tipoParam,
            @RequestParam(value = "id",   required = false) String idParam) {

        log.info("Webhook recibido — tipo param: '{}', id param: '{}'", tipoParam, idParam);
        log.debug("Payload: {}", payload);

        try {
            String tipo = (String) payload.getOrDefault("type", tipoParam);

            if ("payment".equals(tipo)) {
                String mpPaymentId = extraerPaymentId(payload, idParam);
                if (mpPaymentId != null) {
                    webhookPagoServicio.procesarEventoPago(mpPaymentId);
                } else {
                    log.warn("Webhook 'payment' sin ID de pago en el payload ni en query params");
                }
            } else {
                log.info("Evento '{}' ignorado — solo se procesan eventos 'payment'", tipo);
            }

        } catch (Exception e) {
            // Capturamos todo para garantizar 200 al final y evitar reintentos de MP
            log.error("Error procesando webhook: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok("OK");
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> verificar() {
        return ResponseEntity.ok(Map.of(
            "status",   "activo",
            "servicio", "zabora-subscription-webhook"
        ));
    }

    /**
     * Extrae el ID del pago del payload o del query param.
     * MP puede enviarlo de distintas formas segun la version del evento.
     */
    private String extraerPaymentId(Map<String, Object> payload, String idParam) {
        // Forma principal: { "data": { "id": "123" } }
        Object dataObj = payload.get("data");
        if (dataObj instanceof Map<?, ?> data) {
            Object idObj = data.get("id");
            if (idObj != null) {
                String id = String.valueOf(idObj);
                if (!id.isBlank() && !"null".equals(id)) return id;
            }
        }
        // Forma alternativa: query param ?id=123 (envios de prueba de MP)
        if (idParam != null && !idParam.isBlank()) return idParam;

        // Forma legacy: { "id": "123" } directamente en el body
        Object rootId = payload.get("id");
        if (rootId != null) {
            String id = String.valueOf(rootId);
            if (!id.isBlank() && !"null".equals(id)) return id;
        }
        return null;
    }
}
