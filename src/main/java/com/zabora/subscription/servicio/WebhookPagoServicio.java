package com.zabora.subscription.servicio;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Logica de negocio del webhook de MercadoPago.
 *
 * GARANTIAS:
 *  - Idempotencia: si la suscripcion ya esta ACTIVA no la modifica.
 *  - Verificacion por external_reference: no depende de metadata volátil.
 *  - Pago local y suscripcion se actualizan en la misma transaccion.
 *  - Llamada al auth-service es best-effort: si falla, la suscripcion
 *    ya quedo activa en BD y se registra para revision manual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookPagoServicio {

    private final PagoRepositorio pagoRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;
    private final AuthServicio authServicio;

    /**
     * Procesa un evento de tipo "payment" recibido desde MercadoPago.
     * Consulta el estado real del pago en la API de MP y actua en consecuencia.
     *
     * @param mpPaymentIdStr ID del pago en MercadoPago (viene como String del payload)
     */
    @Transactional
    public void procesarEventoPago(String mpPaymentIdStr) {

        // 1. Parsear el ID del pago
        Long mpPaymentId;
        try {
            mpPaymentId = Long.parseLong(mpPaymentIdStr);
        } catch (NumberFormatException e) {
            log.warn("ID de pago no numerico en webhook: '{}'", mpPaymentIdStr);
            return;
        }

        // 2. Consultar el estado real del pago en MercadoPago
        Payment mpPayment;
        try {
            PaymentClient client = new PaymentClient();
            mpPayment = client.get(mpPaymentId);
        } catch (MPApiException e) {
            log.error("MP API Error consultando pago {} — HTTP {}: {}",
                mpPaymentId, e.getStatusCode(), e.getApiResponse().getContent());
            return;
        } catch (MPException e) {
            log.error("MP SDK Error consultando pago {}: {}", mpPaymentId, e.getMessage());
            return;
        }

        log.info("Webhook — Pago {} | Status: '{}' | Detail: '{}' | ExternalRef: '{}'",
            mpPayment.getId(), mpPayment.getStatus(),
            mpPayment.getStatusDetail(), mpPayment.getExternalReference());

        // 3. Obtener el external_reference (= suscripcionId en MySQL)
        String suscripcionId = mpPayment.getExternalReference();
        if (suscripcionId == null || suscripcionId.isBlank()) {
            log.warn("Pago {} sin external_reference — no se puede vincular a suscripcion", mpPaymentId);
            return;
        }

        // 4. Verificar que la suscripcion existe en BD
        Optional<UsuarioSuscripcion> suscripcionOpt = suscripcionRepositorio.findById(suscripcionId);
        if (suscripcionOpt.isEmpty()) {
            log.warn("Suscripcion '{}' del external_reference no existe en BD", suscripcionId);
            return;
        }
        UsuarioSuscripcion suscripcion = suscripcionOpt.get();

        // 5. Procesar segun el estado del pago
        switch (mpPayment.getStatus()) {
            case "approved"                  -> manejarPagoAprobado(mpPayment, suscripcion);
            case "rejected", "cancelled"     -> manejarPagoFallido(mpPayment, suscripcionId);
            case "refunded", "charged_back"  -> manejarReembolso(mpPayment, suscripcionId);
            case "pending", "in_process"     ->
                log.info("Pago {} en estado '{}' — sin accion aun", mpPaymentId, mpPayment.getStatus());
            default ->
                log.warn("Estado no reconocido '{}' para pago {}", mpPayment.getStatus(), mpPaymentId);
        }
    }

    // ── Manejadores por estado ────────────────────────────────────────────────────

    /**
     * Aprobado: actualiza el pago, activa la suscripcion y notifica al auth-service.
     * Idempotente: si ya esta ACTIVA, solo actualiza el pago local si es necesario.
     */
    private void manejarPagoAprobado(Payment mpPayment, UsuarioSuscripcion suscripcion) {
        String  suscripcionId = suscripcion.getId();
        Integer usuarioId     = suscripcion.getUsuarioId();

        // Actualizar el registro de pago local (puede ya estar COMPLETADO si el
        // pago fue "approved" en tiempo real desde BricksPaymentServicio)
        Optional<Pago> pagoOpt = pagoRepositorio
            .findBySuscripcionIdAndEstado(suscripcionId, EstadoPago.PENDIENTE);

        if (pagoOpt.isPresent()) {
            Pago pago = pagoOpt.get();
            pago.setEstado(EstadoPago.COMPLETADO);
            pago.setIdIntentoPago(mpPayment.getId().toString());
            pago.setFechaPago(LocalDateTime.now());
            if (mpPayment.getAuthorizationCode() != null) {
                pago.setCodigoAutorizacion(mpPayment.getAuthorizationCode());
            }
            pagoRepositorio.save(pago);
            log.info("Pago local actualizado a COMPLETADO para suscripcion {}", suscripcionId);
        } else {
            // Webhook duplicado o pago ya procesado en tiempo real — es normal
            log.info("No hay pago PENDIENTE para suscripcion {} — posible webhook duplicado o " +
                "ya procesado en tiempo real", suscripcionId);
        }

        // Idempotencia de la suscripcion
        if (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA) {
            log.info("Suscripcion {} ya esta ACTIVA — webhook duplicado ignorado", suscripcionId);
            return;
        }

        // Activar la suscripcion
        LocalDateTime ahora = LocalDateTime.now();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setInicioPeriodoActual(ahora);
        suscripcion.setFinPeriodoActual(ahora.plusDays(30));
        suscripcion.setFechaActualizacion(ahora);
        suscripcionRepositorio.save(suscripcion);
        log.info("Suscripcion {} activada via webhook para usuario {}", suscripcionId, usuarioId);

        // Notificar al auth-service via Feign + Consul (best-effort)
        try {
            authServicio.actualizarRolPremium(usuarioId);
        } catch (Exception e) {
            // La suscripcion YA fue activada — no revertir. Registrar para revision.
            log.error("Suscripcion {} activa, pero fallo auth-service para usuario {}. " +
                "Requiere revision manual. Error: {}", suscripcionId, usuarioId, e.getMessage());
        }
    }

    /**
     * Rechazado / Cancelado: marca el pago FALLIDO.
     * La suscripcion permanece en PENDIENTE_PAGO para que el usuario intente de nuevo.
     */
    private void manejarPagoFallido(Payment mpPayment, String suscripcionId) {
        pagoRepositorio
            .findBySuscripcionIdAndEstado(suscripcionId, EstadoPago.PENDIENTE)
            .ifPresent(pago -> {
                pago.setEstado(EstadoPago.FALLIDO);
                pago.setMotivoFallo(mpPayment.getStatusDetail());
                pagoRepositorio.save(pago);
                log.info("Pago marcado FALLIDO para suscripcion {} — detalle: '{}'",
                    suscripcionId, mpPayment.getStatusDetail());
            });
    }

    /**
     * Reembolso / contracargo: marca el pago REEMBOLSADO.
     * No cancela la suscripcion automaticamente — decision administrativa.
     */
    private void manejarReembolso(Payment mpPayment, String suscripcionId) {
        pagoRepositorio
            .findByIdIntentoPago(mpPayment.getId().toString())
            .ifPresent(pago -> {
                pago.setEstado(EstadoPago.REEMBOLSADO);
                pagoRepositorio.save(pago);
                log.info("Pago {} marcado REEMBOLSADO para suscripcion {}", mpPayment.getId(), suscripcionId);
            });
    }
}
