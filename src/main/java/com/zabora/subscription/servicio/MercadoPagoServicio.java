package com.zabora.subscription.servicio;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.*;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.modelo.dto.CrearPagoRequest;
import com.zabora.subscription.modelo.dto.CrearPagoResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MercadoPagoServicio {

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

    @PostConstruct
    public void init() {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            log.info("===========================================");
            log.info("MERCADOPAGO INICIALIZADO");
            log.info("===========================================");
            log.info("Environment: {}", environment);
            log.info("Access Token: {}...", accessToken.substring(0, 30));
            log.info("Public Key: {}", publicKey);
            log.info("Notification URL: {}", notificationUrl);
            log.info("===========================================");
        } catch (Exception e) {
            log.error("Error inicializando MercadoPago: {}", e.getMessage());
        }
    }

    public CrearPagoResponse crearPreferenciaPago(CrearPagoRequest request) {

        Integer usuarioId = obtenerUsuarioId();
        String usuarioEmail = obtenerUsuarioEmail();
        
        log.info("===========================================");
        log.info("CREANDO PREFERENCIA DE PAGO");
        log.info("===========================================");
        log.info("Usuario ID: {}", usuarioId);
        log.info("Usuario Email: {}", usuarioEmail);
        log.info("Suscripcion ID: {}", request.getIdSuscripcion());
        log.info("Monto: {}", request.getMonto());
        log.info("===========================================");
        
        try {
            // Item
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id("suscripcion-premium")
                    .title("Suscripcion Premium Zabora")
                    .description("Plan Premium mensual")
                    .quantity(1)
                    .currencyId("COP")
                    .unitPrice(request.getMonto())
                    .build();

            // Back URLs
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(successUrl)
                    .failure(failureUrl)
                    .pending(pendingUrl)
                    .build();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("suscripcion_id", request.getIdSuscripcion());
            metadata.put("usuario_id", usuarioId);

            // Payer
            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .email(usuarioEmail)
                    .build();

            // Build preference
            PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    //.autoReturn("approved")
                    .metadata(metadata)
                    .payer(payer)
                    .externalReference(request.getIdSuscripcion())
                    .statementDescriptor("ZABORA PREMIUM");

            if (notificationUrl != null && !notificationUrl.trim().isEmpty()) {
                builder.notificationUrl(notificationUrl);
            }

            PreferenceRequest preferenceRequest = builder.build();

            log.info("Llamando a MercadoPago API...");

            // Call API
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            log.info("===========================================");
            log.info("EXITO - PREFERENCIA CREADA");
            log.info("===========================================");
            log.info("Preference ID: {}", preference.getId());
            log.info("Init Point: {}", preference.getInitPoint());
            log.info("===========================================");

            return CrearPagoResponse.builder()
                    .preferenceId(preference.getId())
                    .initPoint(preference.getInitPoint())
                    .sandboxInitPoint(preference.getSandboxInitPoint())
                    .amount(request.getMonto())
                    .currency("COP")
                    .publicKey(publicKey)
                    .subscriptionId(request.getIdSuscripcion())
                    .build();

        } catch (MPApiException e) {
            log.error("===========================================");
            log.error("ERROR API MERCADOPAGO");
            log.error("===========================================");
            log.error("Status Code: {}", e.getStatusCode());
            log.error("Message: {}", e.getMessage());
            
            String errorContent = "No disponible";
            try {
                if (e.getApiResponse() != null) {
                    errorContent = e.getApiResponse().getContent();
                    log.error("===========================================");
                    log.error("CONTENIDO DEL ERROR:");
                    log.error("{}", errorContent);
                    log.error("===========================================");
                }
            } catch (Exception ex) {
                log.error("No se pudo extraer contenido: {}", ex.getMessage());
            }
            
            throw new RuntimeException("Error API MercadoPago (" + e.getStatusCode() + "): " + errorContent);
            
        } catch (MPException e) {
            log.error("===========================================");
            log.error("ERROR SDK MERCADOPAGO");
            log.error("===========================================");
            log.error("Message: {}", e.getMessage());
            log.error("Cause: {}", e.getCause() != null ? e.getCause().getMessage() : "N/A");
            log.error("===========================================");
            
            throw new RuntimeException("Error SDK MercadoPago: " + e.getMessage());
        }
    }

    public Payment obtenerPago(Long paymentId) {
        try {
            PaymentClient client = new PaymentClient();
            return client.get(paymentId);
        } catch (MPException | MPApiException e) {
            throw new RuntimeException("Error al consultar pago: " + e.getMessage());
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    private Integer obtenerUsuarioId() {
        try {
            Integer userId = UserContext.get().getUserId();
            log.info("Usuario ID desde contexto: {}", userId);
            return userId;
        } catch (Exception e) {
            log.warn("Usuario ID no disponible en contexto");
            throw new RuntimeException("Usuario no autenticado");
        }
    }

    private String obtenerUsuarioEmail() {
        try {
            String email = UserContext.get().getEmail();
            log.info("Email desde contexto: {}", email);
            return email != null ? email : "test_user_54198363@testuser.com";
        } catch (Exception e) {
            log.warn("Email no disponible en contexto, usando default");
            return "test_user_54198363@testuser.com";
        }
    }
}