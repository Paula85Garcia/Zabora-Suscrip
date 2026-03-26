package com.zabora.subscription.integration.config;

import com.zabora.subscription.modelo.dto.CrearPagoBricksRequest;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class TestDataFactory {

    // ========== SUSCRIPCIONES ==========

    public static SolicitudSuscripcionDTO solicitudGratuito() {
        SolicitudSuscripcionDTO dto = new SolicitudSuscripcionDTO();
        dto.setNombrePlan("gratuito");
        dto.setTipoPago("none");
        return dto;
    }

    public static SolicitudSuscripcionDTO solicitudPremium() {
        SolicitudSuscripcionDTO dto = new SolicitudSuscripcionDTO();
        dto.setNombrePlan("premium");
        dto.setTipoPago("tarjeta_credito");
        return dto;
    }

    // ========== PAGOS ==========

    public static Map<String, Object> pagoRequest(String suscripcionId) {
        Map<String, Object> request = new HashMap<>();
        request.put("idSuscripcion", suscripcionId);
        request.put("monto", new BigDecimal("29900"));
        request.put("tipoPago", "tarjeta_credito");
        request.put("recibirFactura", false);
        return request;
    }

    public static Map<String, Object> pagoRequestConFactura(String suscripcionId) {
        Map<String, Object> request = pagoRequest(suscripcionId);
        request.put("recibirFactura", true);
        return request;
    }

    // ========== PAGOS BRICKS ==========

    public static CrearPagoBricksRequest pagoBricksRequest(String suscripcionId) {
        return CrearPagoBricksRequest.builder()
                .idSuscripcion(suscripcionId)
                .monto(new BigDecimal("29900"))
                .tipoPago("tarjeta_credito")
                .recibirFactura(false)
                .build();
    }

    public static CrearPagoBricksRequest pagoBricksRequestPSE(String suscripcionId) {
        return CrearPagoBricksRequest.builder()
                .idSuscripcion(suscripcionId)
                .monto(new BigDecimal("29900"))
                .tipoPago("pse")
                .recibirFactura(false)
                .build();
    }

    public static CrearPagoBricksRequest pagoBricksRequestConFactura(String suscripcionId) {
        return CrearPagoBricksRequest.builder()
                .idSuscripcion(suscripcionId)
                .monto(new BigDecimal("29900"))
                .tipoPago("tarjeta_credito")
                .recibirFactura(true)
                .build();
    }

    // ========== WEBHOOKS ==========

    public static Map<String, Object> webhookPayloadApproved(Long paymentId, String suscripcionId, Integer usuarioId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "payment");
        payload.put("action", "payment.created");

        Map<String, Object> data = new HashMap<>();
        data.put("id", String.valueOf(paymentId));
        payload.put("data", data);

        return payload;
    }

    public static Map<String, Object> webhookPayloadRejected(Long paymentId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "payment");
        payload.put("action", "payment.updated");

        Map<String, Object> data = new HashMap<>();
        data.put("id", String.valueOf(paymentId));
        payload.put("data", data);

        return payload;
    }

    public static Map<String, Object> webhookPayloadInvalid() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "merchant_order");
        payload.put("action", "merchant_order.updated");
        return payload;
    }

    // ========== MERCADOPAGO MOCK RESPONSE ==========

    public static Map<String, Object> mercadoPagoPaymentApproved(String suscripcionId, Integer usuarioId) {
        Map<String, Object> payment = new HashMap<>();
        payment.put("id", 123456789L);
        payment.put("status", "approved");
        payment.put("transaction_amount", 29900.0);
        payment.put("authorization_code", "AUTH123456");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("suscripcion_id", suscripcionId);
        metadata.put("usuario_id", usuarioId);
        payment.put("metadata", metadata);

        return payment;
    }

    public static Map<String, Object> mercadoPagoPaymentRejected(String suscripcionId, Integer usuarioId) {
        Map<String, Object> payment = new HashMap<>();
        payment.put("id", 987654321L);
        payment.put("status", "rejected");
        payment.put("transaction_amount", 29900.0);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("suscripcion_id", suscripcionId);
        metadata.put("usuario_id", usuarioId);
        payment.put("metadata", metadata);

        return payment;
    }
}