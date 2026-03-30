package com.zabora.subscription.servicio;

import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentAdditionalInfoRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerAddressRequest;
import com.mercadopago.client.payment.PaymentPayerPhoneRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.client.payment.PaymentTransactionDetailsRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mercadopago.resources.payment.Payment;
import com.zabora.subscription.excepcion.PagoRechazadoException;
import com.zabora.subscription.modelo.dto.BricksPaymentDTO;
import com.zabora.subscription.modelo.dto.BricksPsePaymentDTO;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * FIX CAL-5: La llamada HTTP a MercadoPago ahora se hace FUERA de @Transactional.
 *            procesarPagoConBricks() valida y llama a MP sin transaccion.
 *            guardarResultadoPago() persiste el resultado en una transaccion corta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BricksPaymentServicio {

    private static final ObjectMapper OM = new ObjectMapper();

    private final PagoRepositorio pagoRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;
    private final AuthServicio authServicio;
    private final EmailService emailService;
    private final ServicioNotificaciones servicioNotificaciones;

    @Value("${mercadopago.webhook.notification-url}")
    private String notificationUrl;

    @Value("${mercadopago.pending-url}")
    private String pseCallbackUrl;

    /**
     * Procesa el pago con MercadoPago Checkout Bricks.
     * 
     * NO es @Transactional para evitar mantener la conexion a BD abierta
     * durante la llamada HTTP a MercadoPago.
     */
    public Map<String, Object> procesarPagoConBricks(BricksPaymentDTO dto, Integer usuarioId) {

        // 1. Validar suscripcion (lectura sin transaccion — OK)
        UsuarioSuscripcion suscripcion = suscripcionRepositorio
            .findById(dto.getExternalReference())
            .orElseThrow(() -> new IllegalStateException(
                "Suscripcion no encontrada: " + dto.getExternalReference()));

        if (!suscripcion.getUsuarioId().equals(usuarioId)) {
            throw new SecurityException("La suscripcion no pertenece al usuario autenticado");
        }
        validarSuscripcionPermitePagoBricks(suscripcion);

        // 2. Llamar a MercadoPago (SIN transaccion abierta)
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

        // 3. Persistir resultado en transaccion corta
        return guardarResultadoPago(
            dto.getExternalReference(),
            dto.getTransactionAmount(),
            dto.getPaymentMethodId(),
            usuarioId,
            mpPayment,
            suscripcion,
            dto.getRecibirFactura(),
            dto.getPayerEmail());
    }

    /**
     * Crea pago PSE en Mercado Pago (sin token). El comprador debe abrir {@code redirectUrl} del JSON de respuesta.
     */
    public Map<String, Object> procesarPagoPse(BricksPsePaymentDTO dto, Integer usuarioId, String clientIp) {

        UsuarioSuscripcion suscripcion = suscripcionRepositorio
            .findById(dto.getExternalReference())
            .orElseThrow(() -> new IllegalStateException(
                "Suscripcion no encontrada: " + dto.getExternalReference()));

        if (!suscripcion.getUsuarioId().equals(usuarioId)) {
            throw new SecurityException("La suscripcion no pertenece al usuario autenticado");
        }
        validarSuscripcionPermitePagoBricks(suscripcion);

        if (!StringUtils.hasText(notificationUrl) || !StringUtils.hasText(pseCallbackUrl)) {
            throw new IllegalStateException("Faltan mercadopago.webhook.notification-url o mercadopago.pending-url");
        }

        String ip = StringUtils.hasText(clientIp) ? clientIp.trim() : "127.0.0.1";

        IdentificationRequest identification = IdentificationRequest.builder()
            .type(dto.getIdentificationType().trim())
            .number(dto.getIdentificationNumber().trim())
            .build();

        PaymentPayerAddressRequest address = PaymentPayerAddressRequest.builder()
            .zipCode(blankToDefault(dto.getZipCode(), "110111"))
            .streetName(blankToDefault(dto.getStreetName(), "Calle"))
            .streetNumber(blankToDefault(dto.getStreetNumber(), "1"))
            .neighborhood(blankToDefault(dto.getNeighborhood(), "Centro"))
            .city(blankToDefault(dto.getCity(), "Bogota"))
            .federalUnit(blankToDefault(dto.getFederalUnit(), "DC"))
            .build();

        PaymentPayerPhoneRequest phone = PaymentPayerPhoneRequest.builder()
            .areaCode(blankToDefault(dto.getPhoneAreaCode(), "601"))
            .number(blankToDefault(dto.getPhoneNumber(), "1234"))
            .build();

        String rawEntity = dto.getEntityType().trim().toLowerCase(Locale.ROOT);
        String entityTypeNormalizado = "association".equals(rawEntity) ? "association" : "individual";

        PaymentPayerRequest payer = PaymentPayerRequest.builder()
            .email(dto.getPayerEmail().trim())
            .entityType(entityTypeNormalizado)
            .firstName(dto.getFirstName().trim())
            .lastName(dto.getLastName().trim())
            .identification(identification)
            .address(address)
            .phone(phone)
            .build();

        PaymentAdditionalInfoRequest additionalInfo = PaymentAdditionalInfoRequest.builder()
            .ipAddress(ip)
            .build();

        PaymentTransactionDetailsRequest transactionDetails = PaymentTransactionDetailsRequest.builder()
            .financialInstitution(dto.getFinancialInstitution().trim())
            .build();

        String desc = StringUtils.hasText(dto.getDescription())
            ? dto.getDescription().trim()
            : "Suscripcion Premium Zabora";

        PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
            .transactionAmount(dto.getTransactionAmount())
            .description(desc)
            .paymentMethodId("pse")
            .externalReference(dto.getExternalReference())
            .callbackUrl(pseCallbackUrl)
            .notificationUrl(notificationUrl)
            .additionalInfo(additionalInfo)
            .transactionDetails(transactionDetails)
            .payer(payer)
            .build();

        Payment mpPayment;
        try {
            PaymentClient client = new PaymentClient();
            String idemKey = "pse-" + dto.getExternalReference() + "-" + UUID.randomUUID();
            MPRequestOptions opts = MPRequestOptions.builder()
                .customHeaders(Map.of("X-Idempotency-Key", idemKey))
                .build();
            mpPayment = client.create(paymentRequest, opts);
            log.info("Pago PSE MP creado — ID: {}, Status: '{}', Detail: '{}'",
                mpPayment.getId(), mpPayment.getStatus(), mpPayment.getStatusDetail());
        } catch (MPApiException e) {
            log.error("MP API Error (PSE) — HTTP {}: {}", e.getStatusCode(), e.getApiResponse().getContent());
            throw new RuntimeException("Error de MercadoPago: " + e.getMessage());
        } catch (MPException e) {
            log.error("MP SDK Error (PSE): {}", e.getMessage());
            throw new RuntimeException("Error de comunicacion con MercadoPago: " + e.getMessage());
        }

        return guardarResultadoPago(
            dto.getExternalReference(),
            dto.getTransactionAmount(),
            "pse",
            usuarioId,
            mpPayment,
            suscripcion,
            dto.getRecibirFactura(),
            dto.getPayerEmail());
    }

    /**
     * Permite pagar con PENDIENTE_PAGO, o con ACTIVA solo si no hay ningun pago COMPLETADO
     * (caso: suscripcion marcada activa sin cobro registrado — el usuario puede volver al checkout).
     */
    private void validarSuscripcionPermitePagoBricks(UsuarioSuscripcion suscripcion) {
        EstadoSuscripcion estado = suscripcion.getEstado();
        if (estado == EstadoSuscripcion.CANCELADA || estado == EstadoSuscripcion.EXPIRADA) {
            throw new IllegalStateException(
                "La suscripcion esta en estado " + estado + " y no puede ser pagada");
        }
        if (estado == EstadoSuscripcion.PENDIENTE_PAGO) {
            return;
        }
        if (estado == EstadoSuscripcion.ACTIVA) {
            if (pagoRepositorio.existsBySuscripcionIdAndEstado(suscripcion.getId(), EstadoPago.COMPLETADO)) {
                throw new IllegalStateException("La suscripcion ya esta activa");
            }
            return;
        }
        throw new IllegalStateException(
            "La suscripcion esta en estado " + estado + " y no puede ser pagada");
    }

    private static String blankToDefault(String v, String fallback) {
        return StringUtils.hasText(v) ? v.trim() : fallback;
    }

    private static String metadatosFactura(Boolean recibirFactura, String payerEmail) {
        if (!Boolean.TRUE.equals(recibirFactura)) {
            return null;
        }
        try {
            ObjectNode n = OM.createObjectNode();
            n.put("recibirFactura", true);
            if (StringUtils.hasText(payerEmail)) {
                n.put("payerEmail", payerEmail.trim());
            }
            return OM.writeValueAsString(n);
        } catch (Exception e) {
            return "{\"recibirFactura\":true}";
        }
    }

    @Transactional
    protected Map<String, Object> guardarResultadoPago(
            String externalReference,
            BigDecimal transactionAmount,
            String paymentMethodId,
            Integer usuarioId,
            Payment mpPayment,
            UsuarioSuscripcion suscripcion,
            Boolean recibirFactura,
            String payerEmail) {

        String pagoId = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        Pago pago = new Pago();
        pago.setId(pagoId);
        pago.setSuscripcionId(externalReference);
        pago.setUsuarioId(usuarioId);
        pago.setMonto(transactionAmount);
        pago.setMoneda("COP");
        pago.setMetodoPago(resolverMetodoPago(paymentMethodId));
        String meta = metadatosFactura(recibirFactura, payerEmail);
        if (meta != null) {
            pago.setMetadatos(meta);
        }
        Long mpId = mpPayment.getId();
        pago.setIdIntentoPago(mpId != null ? mpId.toString() : "UNKNOWN");

        String mpStatus = mpPayment.getStatus();
        if (mpStatus == null || mpStatus.isBlank()) {
            log.warn("Respuesta MP sin status — paymentId: {}", mpId);
            mpStatus = "pending";
        }

        return switch (mpStatus) {

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
                        externalReference, usuarioId, e.getMessage());
                }
                log.info("Suscripcion {} activada tras pago aprobado", externalReference);
                if (Boolean.TRUE.equals(recibirFactura)) {
                    try {
                        emailService.enviarFacturaPago(pago);
                    } catch (Exception e) {
                        log.warn("Pago aprobado pero fallo envio factura por email: {}", e.getMessage());
                    }
                }
                try {
                    servicioNotificaciones.notificarPremiumActivadoBricks(
                        usuarioId,
                        externalReference,
                        mpId != null ? mpId.toString() : "");
                } catch (Exception e) {
                    log.warn("No se pudo notificar admin activación premium: {}", e.getMessage());
                }
                yield respuestaExito(mpPayment, externalReference);
            }

            case "pending", "in_process" -> {
                pago.setEstado(EstadoPago.PENDIENTE);
                pagoRepositorio.save(pago);
                log.info("Pago {} en estado '{}' — webhook activara la suscripcion",
                    mpId, mpStatus);
                yield respuestaPendiente(mpPayment, externalReference);
            }

            case "rejected" -> {
                pago.setEstado(EstadoPago.FALLIDO);
                pago.setMotivoFallo(mpPayment.getStatusDetail());
                pagoRepositorio.save(pago);
                log.warn("Pago rechazado — ID: {}, Detalle: '{}'", mpId, mpPayment.getStatusDetail());
                String idStr = mpId != null ? mpId.toString() : "";
                throw new PagoRechazadoException(mpPayment.getStatusDetail(), idStr);
            }

            default -> {
                pago.setEstado(EstadoPago.PENDIENTE);
                pagoRepositorio.save(pago);
                log.warn("Estado desconocido de MP: '{}' — tratado como pendiente", mpStatus);
                yield respuestaPendiente(mpPayment, externalReference);
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
        if (mp.getTransactionDetails() != null
                && StringUtils.hasText(mp.getTransactionDetails().getExternalResourceUrl())) {
            r.put("redirectUrl", mp.getTransactionDetails().getExternalResourceUrl());
        }
        return r;
    }
}
