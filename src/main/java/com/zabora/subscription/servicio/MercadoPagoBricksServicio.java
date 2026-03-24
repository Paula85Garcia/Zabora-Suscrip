package com.zabora.subscription.servicio;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.modelo.dto.CrearPagoBricksRequest;
import com.zabora.subscription.modelo.dto.CrearPagoBricksResponse;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para integración con MercadoPago Checkout Bricks
 * Proporciona métodos optimizados para Payment Brick, PSE Brick y Wallet Brick
 */
@Slf4j
@Service
public class MercadoPagoBricksServicio {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    @Value("${mercadopago.environment:test}")
    private String environment;

    @Value("${mercadopago.webhook.notification-url:#{null}}")
    private String notificationUrl;

    @Value("${mercadopago.success-url}")
    private String successUrl;

    @Value("${mercadopago.failure-url}")
    private String failureUrl;

    @Value("${mercadopago.pending-url}")
    private String pendingUrl;

    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;
    private final PagoRepositorio pagoRepositorio;

    public MercadoPagoBricksServicio(
            UsuarioSuscripcionRepositorio suscripcionRepositorio,
            PagoRepositorio pagoRepositorio) {
        this.suscripcionRepositorio = suscripcionRepositorio;
        this.pagoRepositorio = pagoRepositorio;
    }

    @PostConstruct
    public void init() {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            log.info("═══════════════════════════════════════");
            log.info("MERCADOPAGO BRICKS INICIALIZADO");
            log.info("═══════════════════════════════════════");
            log.info("Environment: {}", environment);
            log.info("Public Key: {}", publicKey);
            log.info("Notification URL: {}", notificationUrl);
            log.info("═══════════════════════════════════════");
        } catch (Exception e) {
            log.error("Error inicializando MercadoPago Bricks", e);
        }
    }

    /**
     * Crear preferencia de pago optimizada para Checkout Bricks
     */
    public CrearPagoBricksResponse crearPreferenciaBricks(
            CrearPagoBricksRequest request, 
            Integer usuarioId) {

        String usuarioEmail = obtenerUsuarioEmail();

        log.info("═══════════════════════════════════════");
        log.info("CREANDO PREFERENCIA BRICKS");
        log.info("═══════════════════════════════════════");
        log.info("Usuario ID: {}", usuarioId);
        log.info("Usuario Email: {}", usuarioEmail);
        log.info("Suscripción ID: {}", request.getIdSuscripcion());
        log.info("Monto: {}", request.getMonto());
        log.info("Tipo Pago: {}", request.getTipoPago());
        log.info("═══════════════════════════════════════");

        try {
            // Verificar suscripción
            UsuarioSuscripcion suscripcion = suscripcionRepositorio
                    .findById(request.getIdSuscripcion())
                    .orElseThrow(() -> new RuntimeException("Suscripción no encontrada"));

            if (!suscripcion.getUsuarioId().equals(usuarioId)) {
                throw new RuntimeException("La suscripción no pertenece al usuario");
            }

            // Verificar que no haya pagos pendientes
            boolean existePagoPendiente = pagoRepositorio
                    .existsBySuscripcionIdAndEstado(
                        request.getIdSuscripcion(), 
                        EstadoPago.PENDIENTE
                    );

            if (existePagoPendiente) {
                throw new RuntimeException("Ya existe un pago pendiente para esta suscripción");
            }

            // Item de la preferencia
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id("suscripcion-premium-zabora")
                    .title("Suscripción Premium Zabora")
                    .description("Plan Premium mensual - Acceso ilimitado")
                    .quantity(1)
                    .currencyId("COP")
                    .unitPrice(request.getMonto())
                    .build();

            // Metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("suscripcion_id", request.getIdSuscripcion());
            metadata.put("usuario_id", usuarioId);
            metadata.put("tipo_pago", request.getTipoPago());

            // Payer
            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .email(usuarioEmail)
                    .build();

            // Payment Methods (configuración para Bricks)
            PreferencePaymentMethodsRequest paymentMethods = PreferencePaymentMethodsRequest.builder()
                    .installments(1) // 1 cuota para suscripciones mensuales
                    .defaultInstallments(1)
                    .build();

            // Build preference PARA BRICKS (SIN back URLs para evitar redirección)
            PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                    .items(List.of(item))
                    // NO incluir backUrls para Bricks - esto evita la redirección
                    .metadata(metadata)
                    .payer(payer)
                    .paymentMethods(paymentMethods)
                    .externalReference(request.getIdSuscripcion())
                    .statementDescriptor("ZABORA PREMIUM")
                    .binaryMode(true) // Importante: true para Bricks (solo aprobado/rechazado)
                    .expires(false); // Sin expiración para Bricks

            if (notificationUrl != null && !notificationUrl.trim().isEmpty()) {
                builder.notificationUrl(notificationUrl);
            }

            PreferenceRequest preferenceRequest = builder.build();

            log.info("Enviando solicitud a MercadoPago API...");

            // Llamar API
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            // Guardar pago en BD
            Pago pago = new Pago();
            pago.setId(UUID.randomUUID().toString());
            pago.setSuscripcionId(request.getIdSuscripcion());
            pago.setUsuarioId(usuarioId);
            pago.setMonto(request.getMonto());
            pago.setMoneda("COP");
            pago.setMetodoPago(mapearTipoPago(request.getTipoPago()));
            pago.setEstado(EstadoPago.PENDIENTE);
            pago.setIdIntentoPago(preference.getId());
            pago.setFechaCreacion(LocalDateTime.now());

            pagoRepositorio.save(pago);

            log.info("═══════════════════════════════════════");
            log.info("PREFERENCIA BRICKS CREADA");
            log.info("═══════════════════════════════════════");
            log.info("Preference ID: {}", preference.getId());
            log.info("Pago BD ID: {}", pago.getId());
            log.info("═══════════════════════════════════════");

            return CrearPagoBricksResponse.builder()
                    .preferenceId(preference.getId())
                    .initPoint(null)  // Importante: SIN initPoint para Bricks
                    .sandboxInitPoint(null)  // Importante: SIN sandboxInitPoint para Bricks
                    .publicKey(publicKey)
                    .amount(request.getMonto())
                    .currency("COP")
                    .subscriptionId(request.getIdSuscripcion())
                    .paymentId(pago.getId())
                    .build();

        } catch (MPApiException e) {
            log.error("═══════════════════════════════════════");
            log.error("ERROR API MERCADOPAGO");
            log.error("═══════════════════════════════════════");
            log.error("Status Code: {}", e.getStatusCode());
            log.error("Message: {}", e.getMessage());

            String errorContent = "Error desconocido";
            try {
                if (e.getApiResponse() != null) {
                    errorContent = e.getApiResponse().getContent();
                    log.error("Contenido del error: {}", errorContent);
                }
            } catch (Exception ex) {
                log.error("No se pudo extraer contenido del error", ex);
            }

            throw new RuntimeException("Error API MercadoPago: " + errorContent);

        } catch (MPException e) {
            log.error("═══════════════════════════════════════");
            log.error("ERROR SDK MERCADOPAGO");
            log.error("═══════════════════════════════════════");
            log.error("Message: {}", e.getMessage());
            log.error("═══════════════════════════════════════");

            throw new RuntimeException("Error SDK MercadoPago: " + e.getMessage());
        }
    }

    /**
     * Verificar estado de un pago PSE
     * PSE requiere redirección al banco y este método verifica el resultado
     */
    public Map<String, Object> verificarPagoPSE(String paymentId) {
        log.info("Verificando pago PSE: {}", paymentId);

        Map<String, Object> response = new HashMap<>();

        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(paymentId));

            response.put("paymentId", payment.getId());
            response.put("status", payment.getStatus());
            response.put("statusDetail", payment.getStatusDetail());
            response.put("transactionAmount", payment.getTransactionAmount());
            response.put("dateApproved", payment.getDateApproved());
            response.put("paymentMethodId", payment.getPaymentMethodId());

            log.info("Estado PSE: {}", payment.getStatus());

            return response;

        } catch (Exception e) {
            log.error("Error verificando pago PSE", e);
            throw new RuntimeException("Error verificando pago PSE: " + e.getMessage());
        }
    }

    public String getPublicKey() {
    return publicKey;
}

private String obtenerUsuarioEmail() {
    try {
        String email = UserContext.get().getEmail();
        log.info("Email desde contexto: {}", email);
        return email != null ? email : "test_user@zabora.com";
    } catch (Exception e) {
        log.warn("Email no disponible en contexto, usando default");
        return "test_user@zabora.com";
    }
}

    private String mapearTipoPago(String tipoPago) {
        if ("tarjeta_credito".equalsIgnoreCase(tipoPago) || 
            "card".equalsIgnoreCase(tipoPago)) {
            return "TARJETA_CREDITO";
        } else if ("pse".equalsIgnoreCase(tipoPago)) {
            return "PSE";
        }
        return "TARJETA_CREDITO";
    }
    
    public Map<String, Object> procesarPagoPSE(
            Map<String, Object> paymentData,
            Pago pagoPendiente,
            String suscripcionId) {

        try {
            log.info("Procesando pago PSE para suscripción: {}", suscripcionId);
            log.info("Payment data completo: {}", paymentData);
            log.info("Monto del pago pendiente: {}", pagoPendiente.getMonto());

            PaymentClient paymentClient = new PaymentClient();

            Map<String, Object> formData = (Map<String, Object>) paymentData.get("formData");
            log.info("FormData extraído: {}", formData);
            
            String email = obtenerUsuarioEmail();
            log.info("Email a usar: {}", email);
            
            PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
                    .transactionAmount(pagoPendiente.getMonto())
                    .description("Suscripción Premium Zabora - PSE")
                    .paymentMethodId("pse")
                    .payer(PaymentPayerRequest.builder()
                            .email(email)
                            .build())
                    .metadata(Map.of(
                        "suscripcion_id", suscripcionId,
                        "usuario_id", pagoPendiente.getUsuarioId(),
                        "payment_type", "PSE"
                    ))
                    .build();

            // Agregar notificationUrl solo si está configurado (usando reflection si es necesario)
            if (notificationUrl != null && !notificationUrl.trim().isEmpty()) {
                paymentRequest = PaymentCreateRequest.builder()
                    .transactionAmount(pagoPendiente.getMonto())
                    .description("Suscripción Premium Zabora - PSE")
                    .paymentMethodId("pse")
                    .payer(PaymentPayerRequest.builder()
                            .email(email)
                            .build())
                    .metadata(Map.of(
                        "suscripcion_id", suscripcionId,
                        "usuario_id", pagoPendiente.getUsuarioId(),
                        "payment_type", "PSE"
                    ))
                    .notificationUrl(notificationUrl)
                    .build();
            }

            Payment payment = paymentClient.create(paymentRequest);

            log.info("Pago PSE creado en MP: ID={}, Status={}", 
                     payment.getId(), payment.getStatus());

            pagoPendiente.setIdIntentoPago(payment.getId().toString());

            if ("approved".equals(payment.getStatus())) {
                pagoPendiente.setEstado(EstadoPago.COMPLETADO);
                pagoPendiente.setFechaPago(LocalDateTime.now());
                pagoPendiente.setCodigoAutorizacion(payment.getAuthorizationCode());
            } else if ("rejected".equals(payment.getStatus())) {
                pagoPendiente.setEstado(EstadoPago.FALLIDO);
            } else {
                pagoPendiente.setEstado(EstadoPago.PENDIENTE);
            }

            pagoRepositorio.save(pagoPendiente);

            Map<String, Object> result = new HashMap<>();
            result.put("success", "approved".equals(payment.getStatus()));
            result.put("status", payment.getStatus());
            result.put("statusDetail", payment.getStatusDetail());
            result.put("paymentId", payment.getId());
            result.put("message", "approved".equals(payment.getStatus()) 
                ? "Pago PSE aprobado" : "Pago PSE " + payment.getStatus());

            return result;

        } catch (MPApiException e) {
            log.error("Error API MP PSE: {}", e.getMessage());
            log.error("Error response: {}", e.getApiResponse());
            log.error("Status code: {}", e.getStatusCode());
            throw new RuntimeException("Error procesando pago PSE: " + e.getMessage());
        } catch (MPException e) {
            log.error("Error SDK MP PSE: {}", e.getMessage());
            throw new RuntimeException("Error SDK PSE: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error general procesando PSE: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando pago PSE: " + e.getMessage());
        }
    }

    public Map<String, Object> procesarPagoConToken(
        String token,
        String paymentMethodId,
        String issuerId,
        Integer installments,
        String email,
        Pago pagoPendiente,
        String suscripcionId) {

    try {
        log.info("Procesando pago con token para suscripción: {}", suscripcionId);

        PaymentClient paymentClient = new PaymentClient();

        PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
                .transactionAmount(pagoPendiente.getMonto())
                .token(token)
                .description("Suscripción Premium Zabora")
                .installments(installments)
                .paymentMethodId(paymentMethodId)
                .issuerId(issuerId)
                .payer(PaymentPayerRequest.builder().email(email).build())
                .metadata(Map.of(
                    "suscripcion_id", suscripcionId,
                    "usuario_id", pagoPendiente.getUsuarioId()
                ))
                .build();

        // Agregar notificationUrl solo si está configurado
        if (notificationUrl != null && !notificationUrl.trim().isEmpty()) {
            paymentRequest = PaymentCreateRequest.builder()
                .transactionAmount(pagoPendiente.getMonto())
                .token(token)
                .description("Suscripción Premium Zabora")
                .installments(installments)
                .paymentMethodId(paymentMethodId)
                .issuerId(issuerId)
                .payer(PaymentPayerRequest.builder().email(email).build())
                .metadata(Map.of(
                    "suscripcion_id", suscripcionId,
                    "usuario_id", pagoPendiente.getUsuarioId()
                ))
                .notificationUrl(notificationUrl)
                .build();
        }

        Payment payment = paymentClient.create(paymentRequest);

        log.info("Pago creado en MP: ID={}, Status={}", 
                 payment.getId(), payment.getStatus());

        pagoPendiente.setIdIntentoPago(payment.getId().toString());

        if ("approved".equals(payment.getStatus())) {
            pagoPendiente.setEstado(EstadoPago.COMPLETADO);
            pagoPendiente.setFechaPago(LocalDateTime.now());
            pagoPendiente.setCodigoAutorizacion(payment.getAuthorizationCode());
        } else if ("rejected".equals(payment.getStatus())) {
            pagoPendiente.setEstado(EstadoPago.FALLIDO);
        }

        pagoRepositorio.save(pagoPendiente);

        Map<String, Object> result = new HashMap<>();
        result.put("success", "approved".equals(payment.getStatus()));
        result.put("status", payment.getStatus());
        result.put("statusDetail", payment.getStatusDetail());
        result.put("paymentId", payment.getId());
        result.put("message", "approved".equals(payment.getStatus()) 
            ? "Pago aprobado" : "Pago " + payment.getStatus());

        return result;

    } catch (MPApiException e) {
        log.error("Error API MP: {}", e.getMessage());
        throw new RuntimeException("Error procesando pago: " + e.getMessage());
    } catch (MPException e) {
        log.error("Error SDK MP: {}", e.getMessage());
        throw new RuntimeException("Error SDK: " + e.getMessage());
    }
}
}
