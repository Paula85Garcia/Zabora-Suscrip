package com.zabora.subscription.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class WebhookSimulationTest extends BaseSubscriptionTest {

    @Test
    @DisplayName("WebhookSimulationTest - Simulación Webhook MercadoPago")
    void testWebhookSimulation() {
        String[][] resultados = new String[5][4];
        int testIndex = 0;

        // Webhook.1 - Webhook con payment ID válido y externalReference correcto
        try {
            Integer userId = USER_IDS[0];
            
            // Crear suscripción y procesar pago
            Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
            Response suscripcionResponse = given()
                .spec(userRequest(userId))
                .body(suscripcionRequest)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            String suscripcionId = suscripcionResponse.jsonPath().getString("id");
            
            Map<String, Object> pagoRequest = Map.of(
                "token", TOKEN_VISA_EXITOSO,
                "paymentMethodId", "visa",
                "issuerId", "24",
                "installments", 1,
                "payerEmail", "test@zabora.com",
                "externalReference", suscripcionId,
                "transactionAmount", 29900.00,
                "description", "Suscripcion Premium Zabora"
            );
            
            Response pagoResponse = given()
                .spec(userRequest(userId))
                .body(pagoRequest)
                .when()
                .post("/api/pagos/bricks/pay")
                .then()
                .extract()
                .response();

            String mpPaymentId = pagoResponse.jsonPath().getString("mpPaymentId");
            
            // Simular webhook con payment ID válido
            Map<String, Object> webhookRequest = Map.of(
                "type", "payment",
                "data", Map.of("id", mpPaymentId)
            );
            
            Response webhookResponse = given()
                .spec(unauthenticatedRequest())
                .body(webhookRequest)
                .when()
                .post("/api/webhooks/mercadopago")
                .then()
                .extract()
                .response();

            int statusCode = webhookResponse.getStatusCode();
            
            // Verificar estado final de la suscripción
            Response estadoResponse = given()
                .spec(userRequest(userId))
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .extract()
                .response();

            String estadoFinal = estadoResponse.jsonPath().getString("estado");
            
            if (statusCode == 200 && "ACTIVA".equals(estadoFinal)) {
                resultados[testIndex++] = new String[]{"Webhook.1 - Payment ID válido", "APROBO", "200", "Suscripción activada correctamente"};
            } else {
                resultados[testIndex++] = new String[]{"Webhook.1 - Payment ID válido", "REPROBO", String.valueOf(statusCode), "estado final: " + estadoFinal};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"Webhook.1 - Payment ID válido", "REPROBO", "ERROR", e.getMessage()};
        }

        // Webhook.2 - Webhook duplicado (mismo payment ID)
        try {
            Integer userId = USER_IDS[1];
            
            // Crear suscripción y procesar pago
            Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
            Response suscripcionResponse = given()
                .spec(userRequest(userId))
                .body(suscripcionRequest)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            String suscripcionId = suscripcionResponse.jsonPath().getString("id");
            
            Map<String, Object> pagoRequest = Map.of(
                "token", TOKEN_VISA_EXITOSO,
                "paymentMethodId", "visa",
                "issuerId", "24",
                "installments", 1,
                "payerEmail", "test@zabora.com",
                "externalReference", suscripcionId,
                "transactionAmount", 29900.00,
                "description", "Suscripcion Premium Zabora"
            );
            
            Response pagoResponse = given()
                .spec(userRequest(userId))
                .body(pagoRequest)
                .when()
                .post("/api/pagos/bricks/pay")
                .then()
                .extract()
                .response();

            String mpPaymentId = pagoResponse.jsonPath().getString("mpPaymentId");
            
            // Primer webhook
            Map<String, Object> webhookRequest = Map.of(
                "type", "payment",
                "data", Map.of("id", mpPaymentId)
            );
            
            given()
                .spec(unauthenticatedRequest())
                .body(webhookRequest)
                .when()
                .post("/api/webhooks/mercadopago");
            
            // Segundo webhook (duplicado)
            Response webhookResponse = given()
                .spec(unauthenticatedRequest())
                .body(webhookRequest)
                .when()
                .post("/api/webhooks/mercadopago")
                .then()
                .extract()
                .response();

            int statusCode = webhookResponse.getStatusCode();
            
            if (statusCode == 200) {
                resultados[testIndex++] = new String[]{"Webhook.2 - Webhook duplicado", "APROBO", "200", "Idempotente, no duplica activación"};
            } else {
                resultados[testIndex++] = new String[]{"Webhook.2 - Webhook duplicado", "REPROBO", String.valueOf(statusCode), "Debería retornar 200"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"Webhook.2 - Webhook duplicado", "REPROBO", "ERROR", e.getMessage()};
        }

        // Webhook.3 - Webhook con payment ID inexistente en MP
        try {
            Map<String, Object> webhookRequest = Map.of(
                "type", "payment",
                "data", Map.of("id", "payment_inexistente_999999")
            );
            
            Response webhookResponse = given()
                .spec(unauthenticatedRequest())
                .body(webhookRequest)
                .when()
                .post("/api/webhooks/mercadopago")
                .then()
                .extract()
                .response();

            int statusCode = webhookResponse.getStatusCode();
            
            if (statusCode == 200) {
                resultados[testIndex++] = new String[]{"Webhook.3 - Payment ID inexistente", "APROBO", "200", "Error manejado, responde 200"};
            } else {
                resultados[testIndex++] = new String[]{"Webhook.3 - Payment ID inexistente", "REPROBO", String.valueOf(statusCode), "Debería retornar 200"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"Webhook.3 - Payment ID inexistente", "REPROBO", "ERROR", e.getMessage()};
        }

        // Webhook.4 - Webhook con tipo diferente a "payment"
        try {
            Map<String, Object> webhookRequest = Map.of(
                "type", "merchant_order",
                "data", Map.of("id", "1234567890")
            );
            
            Response webhookResponse = given()
                .spec(unauthenticatedRequest())
                .body(webhookRequest)
                .when()
                .post("/api/webhooks/mercadopago")
                .then()
                .extract()
                .response();

            int statusCode = webhookResponse.getStatusCode();
            
            if (statusCode == 200) {
                resultados[testIndex++] = new String[]{"Webhook.4 - Tipo diferente", "APROBO", "200", "Ignorado, responde 200"};
            } else {
                resultados[testIndex++] = new String[]{"Webhook.4 - Tipo diferente", "REPROBO", String.valueOf(statusCode), "Debería retornar 200"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"Webhook.4 - Tipo diferente", "REPROBO", "ERROR", e.getMessage()};
        }

        // Webhook.5 - Verificar que webhook no reintenta (siempre responde 200)
        try {
            // Enviar múltiples webhooks con el mismo payment ID
            Map<String, Object> webhookRequest = Map.of(
                "type", "payment",
                "data", Map.of("id", "test_payment_retry_123")
            );
            
            boolean todosResponden200 = true;
            for (int i = 0; i < 3; i++) {
                Response webhookResponse = given()
                    .spec(unauthenticatedRequest())
                    .body(webhookRequest)
                    .when()
                    .post("/api/webhooks/mercadopago")
                    .then()
                    .extract()
                .response();

                if (webhookResponse.getStatusCode() != 200) {
                    todosResponden200 = false;
                    break;
                }
            }
            
            if (todosResponden200) {
                resultados[testIndex++] = new String[]{"Webhook.5 - No reintenta", "APROBO", "200", "Siempre responde 200"};
            } else {
                resultados[testIndex++] = new String[]{"Webhook.5 - No reintenta", "REPROBO", "ERROR", "No siempre responde 200"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"Webhook.5 - No reintenta", "REPROBO", "ERROR", e.getMessage()};
        }

        imprimirReporte("WebhookSimulationTest - Simulación Webhook", resultados);
    }

    @Test
    @DisplayName("Webhook.1 - Validar estructura payload MP real")
    void testEstructuraPayloadMPReal() {
        // Payload real de MercadoPago
        Map<String, Object> webhookRequest = Map.of(
            "type", "payment",
            "data", Map.of("id", "1234567890")
        );
        
        given()
            .spec(unauthenticatedRequest())
            .body(webhookRequest)
            .when()
            .post("/api/webhooks/mercadopago")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("Webhook.2 - Validar procesamiento payment aprobado")
    void testProcesamientoPaymentAprobado() {
        Integer userId = USER_IDS[2];
        
        // Crear suscripción
        Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
        Response suscripcionResponse = given()
            .spec(userRequest(userId))
            .body(suscripcionRequest)
            .when()
            .post("/api/suscripciones/suscribir")
            .then()
            .extract()
            .response();

        String suscripcionId = suscripcionResponse.jsonPath().getString("id");
        
        // Procesar pago
        Map<String, Object> pagoRequest = Map.of(
            "token", TOKEN_VISA_EXITOSO,
            "paymentMethodId", "visa",
            "issuerId", "24",
            "installments", 1,
            "payerEmail", "test@zabora.com",
            "externalReference", suscripcionId,
            "transactionAmount", 29900.00,
            "description", "Suscripcion Premium Zabora"
        );
        
        Response pagoResponse = given()
            .spec(userRequest(userId))
            .body(pagoRequest)
            .when()
            .post("/api/pagos/bricks/pay")
            .then()
            .extract()
            .response();

        String mpPaymentId = pagoResponse.jsonPath().getString("mpPaymentId");
        
        // Enviar webhook de aprobación
        Map<String, Object> webhookRequest = Map.of(
            "type", "payment",
            "data", Map.of("id", mpPaymentId)
        );
        
        given()
            .spec(unauthenticatedRequest())
            .body(webhookRequest)
            .when()
            .post("/api/webhooks/mercadopago")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("procesado"))
            .body("$", hasKey("mensaje"))
            .body("procesado", equalTo(true));
    }

    @Test
    @DisplayName("Webhook.3 - Validar procesamiento payment rechazado")
    void testProcesamientoPaymentRechazado() {
        Integer userId = USER_IDS[3];
        
        // Crear suscripción
        Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
        Response suscripcionResponse = given()
            .spec(userRequest(userId))
            .body(suscripcionRequest)
            .when()
            .post("/api/suscripciones/suscribir")
            .then()
            .extract()
            .response();

        String suscripcionId = suscripcionResponse.jsonPath().getString("id");
        
        // Procesar pago rechazado
        Map<String, Object> pagoRequest = Map.of(
            "token", TOKEN_VISA_RECHAZADO,
            "paymentMethodId", "visa",
            "issuerId", "24",
            "installments", 1,
            "payerEmail", "test@zabora.com",
            "externalReference", suscripcionId,
            "transactionAmount", 29900.00,
            "description", "Suscripcion Premium Zabora"
        );
        
        Response pagoResponse = given()
            .spec(userRequest(userId))
            .body(pagoRequest)
            .when()
            .post("/api/pagos/bricks/pay")
            .then()
            .extract()
            .response();

        String mpPaymentId = pagoResponse.jsonPath().getString("mpPaymentId");
        
        // Enviar webhook de rechazo
        Map<String, Object> webhookRequest = Map.of(
            "type", "payment",
            "data", Map.of("id", mpPaymentId)
        );
        
        given()
            .spec(unauthenticatedRequest())
            .body(webhookRequest)
            .when()
            .post("/api/webhooks/mercadopago")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("procesado"))
            .body("$", hasKey("mensaje"))
            .body("procesado", equalTo(true));
    }

    @Test
    @DisplayName("Webhook.4 - Validar idempotencia")
    void testIdempotenciaWebhook() {
        Integer userId = USER_IDS[4];
        
        // Crear suscripción
        Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
        Response suscripcionResponse = given()
            .spec(userRequest(userId))
            .body(suscripcionRequest)
            .when()
            .post("/api/suscripciones/suscribir")
            .then()
            .extract()
            .response();

        String suscripcionId = suscripcionResponse.jsonPath().getString("id");
        
        // Procesar pago
        Map<String, Object> pagoRequest = Map.of(
            "token", TOKEN_VISA_EXITOSO,
            "paymentMethodId", "visa",
            "issuerId", "24",
            "installments", 1,
            "payerEmail", "test@zabora.com",
            "externalReference", suscripcionId,
            "transactionAmount", 29900.00,
            "description", "Suscripcion Premium Zabora"
        );
        
        Response pagoResponse = given()
            .spec(userRequest(userId))
            .body(pagoRequest)
            .when()
            .post("/api/pagos/bricks/pay")
            .then()
            .extract()
            .response();

        String mpPaymentId = pagoResponse.jsonPath().getString("mpPaymentId");
        
        // Enviar webhook múltiples veces
        Map<String, Object> webhookRequest = Map.of(
            "type", "payment",
            "data", Map.of("id", mpPaymentId)
        );
        
        // Primer webhook
        given()
            .spec(unauthenticatedRequest())
            .body(webhookRequest)
            .when()
            .post("/api/webhooks/mercadopago")
            .then()
            .statusCode(200);
        
        // Segundo webhook (debería ser idempotente)
        given()
            .spec(unauthenticatedRequest())
            .body(webhookRequest)
            .when()
            .post("/api/webhooks/mercadopago")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("procesado"))
            .body("$", hasKey("mensaje"))
            .body("procesado", equalTo(true));
    }
}
