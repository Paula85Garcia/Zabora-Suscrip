package com.zabora.subscription.controlador;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.zabora.subscription.servicio.PagoServicioReal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {
    
    @Value("${stripe.webhook.secret:whsec_test_secret}")
    private String webhookSecret;
    
    private final PagoServicioReal pagoServicio;
    
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        Event event;
        
        try {
            // Verificar firma del webhook
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("📨 Webhook recibido: {}", event.getType());
            
        } catch (SignatureVerificationException e) {
            log.error("Firma de webhook inválida");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error parseando webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error");
        }
        
        // Procesar según tipo de evento
        try {
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    PaymentIntent successIntent = deserializePaymentIntent(event);
                    if (successIntent != null) {
                        pagoServicio.handlePaymentSucceeded(successIntent);
                    }
                    break;
                    
                case "payment_intent.payment_failed":
                    PaymentIntent failedIntent = deserializePaymentIntent(event);
                    if (failedIntent != null) {
                        pagoServicio.handlePaymentFailed(failedIntent);
                    }
                    break;
                    
                default:
                    log.debug("ℹ️ Evento no manejado: {}", event.getType());
            }
            
            return ResponseEntity.ok("Webhook processed");
            
        } catch (Exception e) {
            log.error("❌ Error procesando webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }
    
    private PaymentIntent deserializePaymentIntent(Event event) {
        try {
            return (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);
        } catch (Exception e) {
            log.error("❌ Error deserializando PaymentIntent: {}", e.getMessage());
            return null;
        }
    }
}