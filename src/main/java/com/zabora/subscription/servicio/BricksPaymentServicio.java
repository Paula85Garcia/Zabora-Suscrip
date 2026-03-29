package com.zabora.subscription.servicio;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.zabora.subscription.excepcion.PagoRechazadoException;
import com.zabora.subscription.modelo.dto.BricksPaymentDTO;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BricksPaymentServicio {

    private final PagoRepositorio pagoRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;
    private final AuthServicio authServicio;

    @Transactional
    public Map<String, Object> procesarPagoConBricks(BricksPaymentDTO dto, Integer usuarioId) {

        UsuarioSuscripcion suscripcion = suscripcionRepositorio
            .findById(dto.getExternalReference())
            .orElseThrow(() -> new IllegalStateException(
                "Suscripcion no encontrada: " + dto.getExternalReference()));

        if (!suscripcion.getUsuarioId().equals(usuarioId)) {
            throw new SecurityException("La suscripcion no pertenece al usuario autenticado");
        }
        if (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA) {
            throw new IllegalStateException("La suscripcion ya esta activa");
        }
        if (suscripcion.getEstado() == EstadoSuscripcion.CANCELADA
                || suscripcion.getEstado() == EstadoSuscripcion.EXPIRADA) {
            throw new IllegalStateException(
                "La suscripcion esta en estado " + suscripcion.getEstado() + " y no puede ser pagada");
        }

        PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
            .transactionAmount(dto.getTransactionAmount())
            .token(dto.getToken())
            .description(dto.getDescription() != null ? dto.getDescription() : "Suscripcion Premium Zabora")
            .installments(dto.getInstallments())
            .paymentMethodId(dto.getPaymentMethodId())
            .issuerId(dto.getIssuerId())
            .externalReference(dto.getExternalReference())
            .payer(PaymentPayerRequest.builder()
                .email(dto.getPayerEmail())
                .build())
            .build();

        Payment mpPayment;
        try {
            PaymentClient client = new PaymentClient();
            mpPayment = client.create(paymentRequest);
            log.info("Pago MP creado — ID: {}, Status: '{}', Detail: '{}'",
                mpPayment.getId(), mpPayment.getStatus(), mpPayment.getStatusDetail());
        } catch (MPApiException e) {
            log.error("MP API Error — HTTP {}: {}", e.getStatusCode(), e.getApiResponse().getContent());
            throw new RuntimeException("Error de MercadoPago: " + e.getMessage());
        } catch (MPException e) {
            log.error("MP SDK Error: {}", e.getMessage());
            throw new RuntimeException("Error de comunicacion con MercadoPago: " + e.getMessage());
        }

        String pagoId = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        Pago pago = new Pago();
        pago.setId(pagoId);
        pago.setSuscripcionId(dto.getExternalReference());
        pago.setUsuarioId(usuarioId);
        pago.setMonto(dto.getTransactionAmount());
        pago.setMoneda("COP");
        pago.setMetodoPago(resolverMetodoPago(dto.getPaymentMethodId()));
        pago.setIdIntentoPago(mpPayment.getId().toString());

        return switch (mpPayment.getStatus()) {

            case "approved" -> {
                pago.setEstado(EstadoPago.COMPLETADO);
                pago.setFechaPago(LocalDateTime.now());
                if (mpPayment.getAuthorizationCode() != null) {
                    pago.setCodigoAutorizacion(mpPayment.getAuthorizationCode());
                }
                pagoRepositorio.save(pago);
                activarSuscripcion(suscripcion);
                try {
                    authServicio.actualizarRolPremium(usuarioId);
                } catch (Exception e) {
                    log.error("Suscripcion {} activa, pero fallo auth-service para usuario {}. Error: {}",
                        dto.getExternalReference(), usuarioId, e.getMessage());
                }
                log.info("Suscripcion {} activada tras pago aprobado", dto.getExternalReference());
                yield respuestaExito(mpPayment, dto.getExternalReference());
            }

            case "pending", "in_process" -> {
                pago.setEstado(EstadoPago.PENDIENTE);
                pagoRepositorio.save(pago);
                log.info("Pago {} en estado '{}' — webhook activara la suscripcion",
                    mpPayment.getId(), mpPayment.getStatus());
                yield respuestaPendiente(mpPayment, dto.getExternalReference());
            }

            case "rejected" -> {
                pago.setEstado(EstadoPago.FALLIDO);
                pago.setMotivoFallo(mpPayment.getStatusDetail());
                pagoRepositorio.save(pago);
                log.warn("Pago rechazado — ID: {}, Detalle: '{}'", mpPayment.getId(), mpPayment.getStatusDetail());
                throw new PagoRechazadoException(mpPayment.getStatusDetail(), mpPayment.getId().toString());
            }

            default -> {
                pago.setEstado(EstadoPago.PENDIENTE);
                pagoRepositorio.save(pago);
                log.warn("Estado desconocido de MP: '{}' — tratado como pendiente", mpPayment.getStatus());
                yield respuestaPendiente(mpPayment, dto.getExternalReference());
            }
        };
    }

    private void activarSuscripcion(UsuarioSuscripcion suscripcion) {
        if (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA) {
            log.info("Suscripcion {} ya estaba ACTIVA (idempotencia)", suscripcion.getId());
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setInicioPeriodoActual(ahora);
        suscripcion.setFinPeriodoActual(ahora.plusDays(30));
        suscripcion.setFechaActualizacion(ahora);
        suscripcionRepositorio.save(suscripcion);
        log.info("Suscripcion {} activada", suscripcion.getId());
    }

    private String resolverMetodoPago(String paymentMethodId) {
        if (paymentMethodId == null) return "TARJETA_CREDITO";
        return switch (paymentMethodId.toLowerCase()) {
            case "pse" -> "PSE";
            default    -> "TARJETA_CREDITO";
        };
    }

    private Map<String, Object> respuestaExito(Payment mp, String suscripcionId) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("status", "approved");
        r.put("message", "Pago aprobado. Tu suscripcion premium esta activa.");
        r.put("mpPaymentId", mp.getId().toString());
        r.put("suscripcionId", suscripcionId);
        return r;
    }

    private Map<String, Object> respuestaPendiente(Payment mp, String suscripcionId) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", false);
        r.put("status", mp.getStatus());
        r.put("message", "Pago pendiente de confirmacion. Te notificaremos cuando se acredite.");
        r.put("mpPaymentId", mp.getId().toString());
        r.put("suscripcionId", suscripcionId);
        return r;
    }
}
