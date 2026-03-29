package com.zabora.subscription.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP034_FlujoPagoPSETest extends BaseSubscriptionTest {

    @Test
    @DisplayName("CP034 - Flujo Pago PSE")
    void testFlujoPagoPSE() {
        String[][] resultados = new String[4][4];
        int testIndex = 0;

        // CP034.1 - Crear suscripción premium con PSE
        try {
            Integer userId = USER_IDS[0];
            Map<String, Object> request = crearSuscripcionRequest("PREMIUM", "PSE", userId);
            
            Response response = given()
                .spec(userRequest(userId))
                .body(request)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            String estado = response.jsonPath().getString("estado");
            String requierePago = response.jsonPath().getString("requierePago");
            
            if (statusCode == 200 && "PENDIENTE_PAGO".equals(estado) && Boolean.TRUE.toString().equals(requierePago)) {
                resultados[testIndex++] = new String[]{"CP034.1 - Crear suscripción PSE", "APROBO", "200", "estado: PENDIENTE_PAGO, requierePago: true"};
            } else {
                resultados[testIndex++] = new String[]{"CP034.1 - Crear suscripción PSE", "REPROBO", String.valueOf(statusCode), "estado: " + estado + ", requierePago: " + requierePago};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP034.1 - Crear suscripción PSE", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP034.2 - Procesar pago PSE (endpoint unificado)
        try {
            Integer userId = USER_IDS[1];
            
            // Paso 1: Crear suscripción
            Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "PSE", userId);
            Response suscripcionResponse = given()
                .spec(userRequest(userId))
                .body(suscripcionRequest)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            String suscripcionId = suscripcionResponse.jsonPath().getString("id");
            
            // Paso 2: Procesar pago PSE
            Map<String, Object> pagoRequest = Map.of(
                "token", "pse_token_test",
                "paymentMethodId", "pse",
                "issuerId", BANCO_PSE_BANCOLOMBIA,
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

            int statusCode = pagoResponse.getStatusCode();
            String estado = pagoResponse.jsonPath().getString("estado");
            
            if (statusCode == 200 && ("PENDIENTE_PAGO".equals(estado) || "PROCESANDO".equals(estado))) {
                resultados[testIndex++] = new String[]{"CP034.2 - Procesar pago PSE", "APROBO", "200", "estado: " + estado + " (redirección al banco)"};
            } else {
                resultados[testIndex++] = new String[]{"CP034.2 - Procesar pago PSE", "REPROBO", String.valueOf(statusCode), "estado: " + estado};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP034.2 - Procesar pago PSE", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP034.3 - Simular webhook con pago PSE aprobado
        try {
            Integer userId = USER_IDS[2];
            
            // Paso 1: Crear suscripción y procesar pago PSE
            Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "PSE", userId);
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
                "token", "pse_token_test",
                "paymentMethodId", "pse",
                "issuerId", BANCO_PSE_BANCOLOMBIA,
                "installments", 1,
                "payerEmail", "test@zabora.com",
                "externalReference", suscripcionId,
                "transactionAmount", 29900.00,
                "description", "Suscripcion Premium Zabora"
            );
            
            given()
                .spec(userRequest(userId))
                .body(pagoRequest)
                .when()
                .post("/api/pagos/bricks/pay");
            
            // Paso 3: Simular webhook aprobado
            Map<String, Object> webhookRequest = Map.of(
                "type", "payment",
                "data", Map.of("id", "pse_payment_approved_123")
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
                resultados[testIndex++] = new String[]{"CP034.3 - Webhook PSE aprobado", "APROBO", "200", "Suscripción activada correctamente"};
            } else {
                resultados[testIndex++] = new String[]{"CP034.3 - Webhook PSE aprobado", "REPROBO", String.valueOf(statusCode), "estado final: " + estadoFinal};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP034.3 - Webhook PSE aprobado", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP034.4 - Simular webhook con pago PSE rechazado
        try {
            Integer userId = USER_IDS[3];
            
            // Paso 1: Crear suscripción y procesar pago PSE
            Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "PSE", userId);
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
                "token", "pse_token_test",
                "paymentMethodId", "pse",
                "issuerId", BANCO_PSE_BANCOLOMBIA,
                "installments", 1,
                "payerEmail", "test@zabora.com",
                "externalReference", suscripcionId,
                "transactionAmount", 29900.00,
                "description", "Suscripcion Premium Zabora"
            );
            
            given()
                .spec(userRequest(userId))
                .body(pagoRequest)
                .when()
                .post("/api/pagos/bricks/pay");
            
            // Paso 3: Simular webhook rechazado
            Map<String, Object> webhookRequest = Map.of(
                "type", "payment",
                "data", Map.of("id", "pse_payment_rejected_456")
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
            
            if (statusCode == 200 && "PENDIENTE_PAGO".equals(estadoFinal)) {
                resultados[testIndex++] = new String[]{"CP034.4 - Webhook PSE rechazado", "APROBO", "200", "Suscripción permanece PENDIENTE"};
            } else {
                resultados[testIndex++] = new String[]{"CP034.4 - Webhook PSE rechazado", "REPROBO", String.valueOf(statusCode), "estado final: " + estadoFinal};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP034.4 - Webhook PSE rechazado", "REPROBO", "ERROR", e.getMessage()};
        }

        imprimirReporte("CP034 - Flujo Pago PSE", resultados);
    }

    @Test
    @DisplayName("CP034.1 - Validar estructura de respuesta PSE")
    void testEstructuraRespuestaPSE() {
        Integer userId = USER_IDS[4];
        Map<String, Object> request = crearSuscripcionRequest("PREMIUM", "PSE", userId);
        
        given()
            .spec(userRequest(userId))
            .body(request)
            .when()
            .post("/api/suscripciones/suscribir")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("id"))
            .body("$", hasKey("estado"))
            .body("$", hasKey("requierePago"))
            .body("$", hasKey("fechaCreacion"))
            .body("$", hasKey("nombrePlan"))
            .body("$", hasKey("usuarioId"))
            .body("estado", equalTo("PENDIENTE_PAGO"))
            .body("requierePago", equalTo(true))
            .body("nombrePlan", equalTo("PREMIUM"));
    }

    @Test
    @DisplayName("CP034.2 - Validar procesamiento PSE")
    void testProcesamientoPSE() {
        Integer userId = USER_IDS[5];
        
        // Crear suscripción
        Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "PSE", userId);
        Response suscripcionResponse = given()
            .spec(userRequest(userId))
            .body(suscripcionRequest)
            .when()
            .post("/api/suscripciones/suscribir")
            .then()
            .extract()
            .response();

        String suscripcionId = suscripcionResponse.jsonPath().getString("id");
        
        // Procesar pago PSE
        Map<String, Object> pagoRequest = Map.of(
            "token", "pse_token_test",
            "paymentMethodId", "pse",
            "issuerId", BANCO_PSE_BANCOLOMBIA,
            "installments", 1,
            "payerEmail", "test@zabora.com",
            "externalReference", suscripcionId,
            "transactionAmount", 29900.00,
            "description", "Suscripcion Premium Zabora"
        );
        
        given()
            .spec(userRequest(userId))
            .body(pagoRequest)
            .when()
            .post("/api/pagos/bricks/pay")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("success"))
            .body("$", hasKey("estado"))
            .body("$", hasKey("mensaje"))
            .body("success", equalTo(true))
            .body("estado", anyOf(equalTo("PENDIENTE_PAGO"), equalTo("PROCESANDO")));
    }
}
