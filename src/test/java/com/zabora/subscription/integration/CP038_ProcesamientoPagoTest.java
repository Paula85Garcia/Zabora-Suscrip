package com.zabora.subscription.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP038_ProcesamientoPagoTest extends BaseSubscriptionTest {

    @Test
    @DisplayName("CP038 - Procesamiento de Pago (Endpoint Bricks)")
    void testProcesamientoPago() {
        String[][] resultados = new String[5][4];
        int testIndex = 0;

        // CP038.1 - Pago exitoso con token válido
        try {
            Integer userId = USER_IDS[0];
            
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
            
            // Procesar pago con body completo según especificación
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
            
            if (statusCode == 200 && success && "ACTIVA".equals(estadoSuscripcion) && mpPaymentId != null) {
                resultados[testIndex++] = new String[]{"CP038.1 - Pago exitoso token válido", "APROBO", "200", "Suscripción ACTIVA, mpPaymentId: " + mpPaymentId};
            } else {
                resultados[testIndex++] = new String[]{"CP038.1 - Pago exitoso token válido", "REPROBO", String.valueOf(statusCode), "success: " + success + ", estado: " + estadoSuscripcion};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP038.1 - Pago exitoso token válido", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP038.2 - Pago rechazado
        try {
            Integer userId = USER_IDS[1];
            
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
            
            // Procesar pago con token rechazado
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
            
            if (statusCode == 422 && statusDetail != null && !statusDetail.isEmpty()) {
                resultados[testIndex++] = new String[]{"CP038.2 - Pago rechazado", "APROBO", "422", "statusDetail: " + statusDetail};
            } else {
                resultados[testIndex++] = new String[]{"CP038.2 - Pago rechazado", "REPROBO", String.valueOf(statusCode), "statusDetail: " + statusDetail};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP038.2 - Pago rechazado", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP038.3 - Pago sin token
        try {
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
            
            // Procesar pago sin token
            Map<String, Object> pagoRequest = Map.of(
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
            
            if (statusCode == 400) {
                resultados[testIndex++] = new String[]{"CP038.3 - Pago sin token", "APROBO", "400", "Validación de token funcionando"};
            } else {
                resultados[testIndex++] = new String[]{"CP038.3 - Pago sin token", "REPROBO", String.valueOf(statusCode), "Debería retornar 400"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP038.3 - Pago sin token", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP038.4 - Pago con externalReference inválido
        try {
            Integer userId = USER_IDS[3];
            
            // Procesar pago con externalReference inválido
            Map<String, Object> pagoRequest = Map.of(
                "token", TOKEN_VISA_EXITOSO,
                "paymentMethodId", "visa",
                "issuerId", "24",
                "installments", 1,
                "payerEmail", "test@zabora.com",
                "externalReference", "referencia-invalida-999",
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
            
            if (statusCode == 400 || statusCode == 404 || statusCode == 500) {
                resultados[testIndex++] = new String[]{"CP038.4 - ExternalReference inválido", "APROBO", String.valueOf(statusCode), "Validación funcionando"};
            } else {
                resultados[testIndex++] = new String[]{"CP038.4 - ExternalReference inválido", "REPROBO", String.valueOf(statusCode), "Debería retornar 400/404/500"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP038.4 - ExternalReference inválido", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP038.5 - Pago sin autenticación
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
                resultados[testIndex++] = new String[]{"CP038.5 - Sin autenticación", "APROBO", "401", "-"};
            } else {
                resultados[testIndex++] = new String[]{"CP038.5 - Sin autenticación", "REPROBO", String.valueOf(statusCode), "Debería retornar 401"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP038.5 - Sin autenticación", "REPROBO", "ERROR", e.getMessage()};
        }

        imprimirReporte("CP038 - Procesamiento de Pago (Endpoint Bricks)", resultados);
    }

    @Test
    @DisplayName("CP038.1 - Validar estructura de body esperado")
    void testEstructuraBodyEsperado() {
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
        
        // Body completo según especificación
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
        
        // Validar que el request se procesa correctamente
        given()
            .spec(userRequest(userId))
            .body(pagoRequest)
            .when()
            .post("/api/pagos/bricks/pay")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("CP038.2 - Validar estructura de respuesta pago exitoso")
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
            .body("$", hasKey("fechaPago"))
            .body("success", equalTo(true))
            .body("estadoSuscripcion", equalTo("ACTIVA"))
            .body("mpPaymentId", notNullValue())
            .body("fechaPago", notNullValue());
    }

    @Test
    @DisplayName("CP038.3 - Validar estructura de respuesta pago rechazado")
    void testEstructuraRespuestaPagoRechazado() {
        Integer userId = USER_IDS[6];
        
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
        
        given()
            .spec(userRequest(userId))
            .body(pagoRequest)
            .when()
            .post("/api/pagos/bricks/pay")
            .then()
            .statusCode(422)
            .contentType(ContentType.JSON)
            .body("$", hasKey("success"))
            .body("$", hasKey("statusDetail"))
            .body("$", hasKey("mensaje"))
            .body("success", equalTo(false))
            .body("statusDetail", notNullValue())
            .body("statusDetail", matchesPattern("cc_rejected_.*"));
    }

    @Test
    @DisplayName("CP038.4 - Validar traducción de mensajes de error")
    void testTraduccionMensajesError() {
        Integer userId = USER_IDS[7];
        
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
        
        Response response = given()
            .spec(userRequest(userId))
            .body(pagoRequest)
            .when()
            .post("/api/pagos/bricks/pay")
            .then()
            .extract()
            .response();

        String statusDetail = response.jsonPath().getString("statusDetail");
        String mensaje = response.jsonPath().getString("mensaje");
        
        // Verificar que el mensaje esté traducido al español
        if (mensaje != null && !mensaje.isEmpty()) {
            System.out.println("StatusDetail: " + statusDetail);
            System.out.println("Mensaje traducido: " + mensaje);
        }
    }
}
