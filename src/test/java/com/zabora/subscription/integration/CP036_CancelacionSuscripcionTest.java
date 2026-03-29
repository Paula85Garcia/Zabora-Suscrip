package com.zabora.subscription.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP036_CancelacionSuscripcionTest extends BaseSubscriptionTest {

    @Test
    @DisplayName("CP036 - Cancelación de Suscripción")
    void testCancelacionSuscripcion() {
        String[][] resultados = new String[6][4];
        int testIndex = 0;

        // CP036.1 - Cancelación inmediata
        try {
            Integer userId = USER_IDS[0];
            
            // Crear suscripción gratuita activa
            Map<String, Object> suscripcionRequest = crearSuscripcionRequest("GRATUITO", "NONE", userId);
            Response suscripcionResponse = given()
                .spec(userRequest(userId))
                .body(suscripcionRequest)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            String suscripcionId = suscripcionResponse.jsonPath().getString("id");
            
            // Cancelación inmediata
            Response cancelResponse = given()
                .spec(userRequest(userId))
                .queryParam("inmediata", true)
                .when()
                .post("/api/suscripciones/cancelar/" + suscripcionId)
                .then()
                .extract()
                .response();

            int statusCode = cancelResponse.getStatusCode();
            String estado = cancelResponse.jsonPath().getString("estado");
            
            if (statusCode == 200 && "CANCELADA".equals(estado)) {
                resultados[testIndex++] = new String[]{"CP036.1 - Cancelación inmediata", "APROBO", "200", "estado: CANCELADA"};
            } else {
                resultados[testIndex++] = new String[]{"CP036.1 - Cancelación inmediata", "REPROBO", String.valueOf(statusCode), "estado: " + estado};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP036.1 - Cancelación inmediata", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP036.2 - Cancelación al final del período
        try {
            Integer userId = USER_IDS[1];
            
            // Crear suscripción premium activa
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
                .post("/api/pagos/bricks/pay");
            
            // Cancelación al final del período
            Response cancelResponse = given()
                .spec(userRequest(userId))
                .queryParam("inmediata", false)
                .when()
                .post("/api/suscripciones/cancelar/" + suscripcionId)
                .then()
                .extract()
                .response();

            int statusCode = cancelResponse.getStatusCode();
            String estado = cancelResponse.jsonPath().getString("estado");
            boolean cancelarAlFinalPeriodo = cancelResponse.jsonPath().getBoolean("cancelarAlFinalPeriodo");
            
            if (statusCode == 200 && ("ACTIVA".equals(estado) || "CANCELADA_AL_FINAL_PERIODO".equals(estado)) && cancelarAlFinalPeriodo) {
                resultados[testIndex++] = new String[]{"CP036.2 - Cancelación fin período", "APROBO", "200", "cancelarAlFinalPeriodo: true"};
            } else {
                resultados[testIndex++] = new String[]{"CP036.2 - Cancelación fin período", "REPROBO", String.valueOf(statusCode), "estado: " + estado + ", cancelarAlFinal: " + cancelarAlFinalPeriodo};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP036.2 - Cancelación fin período", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP036.3 - Cancelar suscripción inexistente
        try {
            Integer userId = USER_IDS[2];
            
            Response cancelResponse = given()
                .spec(userRequest(userId))
                .queryParam("inmediata", true)
                .when()
                .post("/api/suscripciones/cancelar/suscripcion-inexistente-999")
                .then()
                .extract()
                .response();

            int statusCode = cancelResponse.getStatusCode();
            
            if (statusCode == 400 || statusCode == 404 || statusCode == 500) {
                resultados[testIndex++] = new String[]{"CP036.3 - Suscripción inexistente", "APROBO", String.valueOf(statusCode), "Error validado correctamente"};
            } else {
                resultados[testIndex++] = new String[]{"CP036.3 - Suscripción inexistente", "REPROBO", String.valueOf(statusCode), "Debería retornar 400/404/500"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP036.3 - Suscripción inexistente", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP036.4 - Cancelar suscripción de otro usuario
        try {
            Integer userId1 = USER_IDS[3];
            Integer userId2 = USER_IDS[4];
            
            // Crear suscripción para usuario 1
            Map<String, Object> suscripcionRequest = crearSuscripcionRequest("GRATUITO", "NONE", userId1);
            Response suscripcionResponse = given()
                .spec(userRequest(userId1))
                .body(suscripcionRequest)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            String suscripcionId = suscripcionResponse.jsonPath().getString("id");
            
            // Intentar cancelar con usuario 2
            Response cancelResponse = given()
                .spec(userRequest(userId2))
                .queryParam("inmediata", true)
                .when()
                .post("/api/suscripciones/cancelar/" + suscripcionId)
                .then()
                .extract()
                .response();

            int statusCode = cancelResponse.getStatusCode();
            
            if (statusCode == 403 || statusCode == 400 || statusCode == 404) {
                resultados[testIndex++] = new String[]{"CP036.4 - Cancelar de otro usuario", "APROBO", String.valueOf(statusCode), "Acceso denegado correctamente"};
            } else {
                resultados[testIndex++] = new String[]{"CP036.4 - Cancelar de otro usuario", "REPROBO", String.valueOf(statusCode), "Debería retornar 403/400/404"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP036.4 - Cancelar de otro usuario", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP036.5 - Cancelar suscripción ya cancelada
        try {
            Integer userId = USER_IDS[5];
            
            // Crear suscripción
            Map<String, Object> suscripcionRequest = crearSuscripcionRequest("GRATUITO", "NONE", userId);
            Response suscripcionResponse = given()
                .spec(userRequest(userId))
                .body(suscripcionRequest)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            String suscripcionId = suscripcionResponse.jsonPath().getString("id");
            
            // Primera cancelación
            given()
                .spec(userRequest(userId))
                .queryParam("inmediata", true)
                .when()
                .post("/api/suscripciones/cancelar/" + suscripcionId);
            
            // Segunda cancelación (debería dar error)
            Response cancelResponse = given()
                .spec(userRequest(userId))
                .queryParam("inmediata", true)
                .when()
                .post("/api/suscripciones/cancelar/" + suscripcionId)
                .then()
                .extract()
                .response();

            int statusCode = cancelResponse.getStatusCode();
            
            if (statusCode == 400 || statusCode == 500) {
                resultados[testIndex++] = new String[]{"CP036.5 - Suscripción ya cancelada", "APROBO", String.valueOf(statusCode), "Error validado correctamente"};
            } else {
                resultados[testIndex++] = new String[]{"CP036.5 - Suscripción ya cancelada", "REPROBO", String.valueOf(statusCode), "Debería retornar 400/500"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP036.5 - Suscripción ya cancelada", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP036.6 - Cancelar sin autenticación
        try {
            Response cancelResponse = given()
                .spec(unauthenticatedRequest())
                .queryParam("inmediata", true)
                .when()
                .post("/api/suscripciones/cancelar/test-suscripcion-id")
                .then()
                .extract()
                .response();

            int statusCode = cancelResponse.getStatusCode();
            
            if (statusCode == 401) {
                resultados[testIndex++] = new String[]{"CP036.6 - Sin autenticación", "APROBO", "401", "-"};
            } else {
                resultados[testIndex++] = new String[]{"CP036.6 - Sin autenticación", "REPROBO", String.valueOf(statusCode), "Debería retornar 401"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP036.6 - Sin autenticación", "REPROBO", "ERROR", e.getMessage()};
        }

        imprimirReporte("CP036 - Cancelación de Suscripción", resultados);
    }

    @Test
    @DisplayName("CP036.1 - Validar estructura de respuesta cancelación inmediata")
    void testEstructuraRespuestaCancelacionInmediata() {
        Integer userId = USER_IDS[6];
        
        // Crear suscripción
        Map<String, Object> suscripcionRequest = crearSuscripcionRequest("GRATUITO", "NONE", userId);
        Response suscripcionResponse = given()
            .spec(userRequest(userId))
            .body(suscripcionRequest)
            .when()
            .post("/api/suscripciones/suscribir")
            .then()
            .extract()
            .response();

        String suscripcionId = suscripcionResponse.jsonPath().getString("id");
        
        // Cancelar inmediatamente
        given()
            .spec(userRequest(userId))
            .queryParam("inmediata", true)
            .when()
            .post("/api/suscripciones/cancelar/" + suscripcionId)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("id"))
            .body("$", hasKey("estado"))
            .body("$", hasKey("fechaCancelacion"))
            .body("$", hasKey("mensaje"))
            .body("estado", equalTo("CANCELADA"))
            .body("fechaCancelacion", notNullValue())
            .body("mensaje", containsString("cancelada"));
    }

    @Test
    @DisplayName("CP036.2 - Validar estructura de respuesta cancelación fin período")
    void testEstructuraRespuestaCancelacionFinPeriodo() {
        Integer userId = USER_IDS[7];
        
        // Crear suscripción premium
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
        
        // Activar suscripción
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
            .post("/api/pagos/bricks/pay");
        
        // Cancelar al final del período
        given()
            .spec(userRequest(userId))
            .queryParam("inmediata", false)
            .when()
            .post("/api/suscripciones/cancelar/" + suscripcionId)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("id"))
            .body("$", hasKey("estado"))
            .body("$", hasKey("cancelarAlFinalPeriodo"))
            .body("$", hasKey("mensaje"))
            .body("cancelarAlFinalPeriodo", equalTo(true))
            .body("mensaje", containsString("final del período"));
    }
}
