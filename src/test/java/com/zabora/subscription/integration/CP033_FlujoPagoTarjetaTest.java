package com.zabora.subscription.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP033_FlujoPagoTarjetaTest extends BaseSubscriptionTest {

    @Test
    @DisplayName("CP033 - Flujo Pago Tarjeta (COMPLETO)")
    void testFlujoPagoTarjeta() {
        String[][] resultados = new String[6][4];
        int testIndex = 0;

        // CP033.1 - Pago exitoso
        try {
            Integer userId = USER_IDS[0];
            
            // Paso 1: Crear suscripción
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
            
            // Paso 2: Procesar pago con token exitoso
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

            int statusCode = pagoResponse.getStatusCode();
            boolean success = pagoResponse.jsonPath().getBoolean("success");
            String estadoSuscripcion = pagoResponse.jsonPath().getString("estadoSuscripcion");
            String mpPaymentId = pagoResponse.jsonPath().getString("mpPaymentId");
            
            if (statusCode == 200 && success && "ACTIVA".equals(estadoSuscripcion)) {
                resultados[testIndex++] = new String[]{"CP033.1 - Pago exitoso", "APROBO", "200", "Suscripción ACTIVA, mpPaymentId: " + mpPaymentId};
            } else {
                resultados[testIndex++] = new String[]{"CP033.1 - Pago exitoso", "REPROBO", String.valueOf(statusCode), "success: " + success + ", estado: " + estadoSuscripcion};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP033.1 - Pago exitoso", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP033.2 - Pago rechazado
        try {
            Integer userId = USER_IDS[1];
            
            // Paso 1: Crear suscripción
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
            
            // Paso 2: Procesar pago con token rechazado
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

            int statusCode = pagoResponse.getStatusCode();
            String statusDetail = pagoResponse.jsonPath().getString("statusDetail");
            
            if (statusCode == 422 && statusDetail != null) {
                resultados[testIndex++] = new String[]{"CP033.2 - Pago rechazado", "APROBO", "422", "statusDetail: " + statusDetail};
            } else {
                resultados[testIndex++] = new String[]{"CP033.2 - Pago rechazado", "REPROBO", String.valueOf(statusCode), "statusDetail: " + statusDetail};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP033.2 - Pago rechazado", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP033.3 - Pago con suscripción inexistente
        try {
            Integer userId = USER_IDS[2];
            
            Map<String, Object> pagoRequest = Map.of(
                "token", TOKEN_VISA_EXITOSO,
                "paymentMethodId", "visa",
                "issuerId", "24",
                "installments", 1,
                "payerEmail", "test@zabora.com",
                "externalReference", "suscripcion-inexistente-999",
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
            
            if (statusCode == 400 || statusCode == 500) {
                resultados[testIndex++] = new String[]{"CP033.3 - Suscripción inexistente", "APROBO", String.valueOf(statusCode), "Suscripcion no encontrada"};
            } else {
                resultados[testIndex++] = new String[]{"CP033.3 - Suscripción inexistente", "REPROBO", String.valueOf(statusCode), "Debería retornar 400/500"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP033.3 - Suscripción inexistente", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP033.4 - Pago sin autenticación
        try {
            Map<String, Object> pagoRequest = Map.of(
                "token", TOKEN_VISA_EXITOSO,
                "paymentMethodId", "visa",
                "issuerId", "24",
                "installments", 1,
                "payerEmail", "test@zabora.com",
                "externalReference", "test-suscripcion",
                "transactionAmount", 29900.00,
                "description", "Suscripcion Premium Zabora"
            );
            
            Response pagoResponse = given()
                .spec(unauthenticatedRequest())
                .body(pagoRequest)
                .when()
                .post("/api/pagos/bricks/pay")
                .then()
                .extract()
                .response();

            int statusCode = pagoResponse.getStatusCode();
            
            if (statusCode == 401) {
                resultados[testIndex++] = new String[]{"CP033.4 - Sin autenticación", "APROBO", "401", "-"};
            } else {
                resultados[testIndex++] = new String[]{"CP033.4 - Sin autenticación", "REPROBO", String.valueOf(statusCode), "Debería retornar 401"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP033.4 - Sin autenticación", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP033.5 - Pago con suscripción ya activa
        try {
            Integer userId = USER_IDS[3];
            
            // Paso 1: Crear suscripción y activarla
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
            
            // Activar la suscripción (simulación)
            Map<String, Object> pagoRequest1 = Map.of(
                "token", TOKEN_VISA_EXITOSO,
                "paymentMethodId", "visa",
                "issuerId", "24",
                "installments", 1,
                "payerEmail", "test@zabora.com",
                "externalReference", suscripcionId,
                "transactionAmount", 29900.00,
                "description", "Suscripcion Premium Zabora"
            );
            
            given()
                .spec(userRequest(userId))
                .body(pagoRequest1)
                .when()
                .post("/api/pagos/bricks/pay");
            
            // Intentar pagar nuevamente
            Map<String, Object> pagoRequest2 = Map.of(
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
                .body(pagoRequest2)
                .when()
                .post("/api/pagos/bricks/pay")
                .then()
                .extract()
                .response();

            int statusCode = pagoResponse.getStatusCode();
            
            if (statusCode == 400) {
                resultados[testIndex++] = new String[]{"CP033.5 - Suscripción ya activa", "APROBO", "400", "ya esta activa"};
            } else {
                resultados[testIndex++] = new String[]{"CP033.5 - Suscripción ya activa", "REPROBO", String.valueOf(statusCode), "Debería retornar 400"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP033.5 - Suscripción ya activa", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP033.6 - Pago con monto incorrecto
        try {
            Integer userId = USER_IDS[4];
            
            // Paso 1: Crear suscripción
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
            
            // Paso 2: Procesar pago con monto incorrecto
            Map<String, Object> pagoRequest = Map.of(
                "token", TOKEN_VISA_EXITOSO,
                "paymentMethodId", "visa",
                "issuerId", "24",
                "installments", 1,
                "payerEmail", "test@zabora.com",
                "externalReference", suscripcionId,
                "transactionAmount", 100.00, // Monto incorrecto
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
            
            if (statusCode == 400 || statusCode == 422) {
                resultados[testIndex++] = new String[]{"CP033.6 - Monto incorrecto", "APROBO", String.valueOf(statusCode), "-"};
            } else {
                resultados[testIndex++] = new String[]{"CP033.6 - Monto incorrecto", "REPROBO", String.valueOf(statusCode), "Debería retornar 400/422"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP033.6 - Monto incorrecto", "REPROBO", "ERROR", e.getMessage()};
        }

        imprimirReporte("CP033 - Flujo Pago Tarjeta", resultados);
    }

    @Test
    @DisplayName("CP033.1 - Validar estructura de respuesta pago exitoso")
    void testEstructuraRespuestaPagoExitoso() {
        Integer userId = USER_IDS[5];
        
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
        
        given()
            .spec(userRequest(userId))
            .body(pagoRequest)
            .when()
            .post("/api/pagos/bricks/pay")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("success"))
            .body("$", hasKey("mpPaymentId"))
            .body("$", hasKey("estadoSuscripcion"))
            .body("$", hasKey("mensaje"))
            .body("success", equalTo(true))
            .body("estadoSuscripcion", equalTo("ACTIVA"))
            .body("mpPaymentId", notNullValue());
    }
}
