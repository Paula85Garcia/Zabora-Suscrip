package com.zabora.subscription.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP037_VerificacionEstadoTest extends BaseSubscriptionTest {

    @Test
    @DisplayName("CP037 - Verificación de Estado")
    void testVerificacionEstado() {
        String[][] resultados = new String[4][4];
        int testIndex = 0;

        // CP037.1 - Usuario con suscripción ACTIVA
        try {
            Integer userId = USER_IDS[0];
            
            // Crear suscripción gratuita activa
            Map<String, Object> suscripcionRequest = crearSuscripcionRequest("GRATUITO", "NONE", userId);
            given()
                .spec(userRequest(userId))
                .body(suscripcionRequest)
                .when()
                .post("/api/suscripciones/suscribir");
            
            // Verificar estado
            Response response = given()
                .spec(userRequest(userId))
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            String estado = response.jsonPath().getString("estado");
            String plan = response.jsonPath().getString("nombrePlan");
            String fechaInicio = response.jsonPath().getString("fechaInicio");
            String fechaExpiracion = response.jsonPath().getString("fechaExpiracion");
            
            if (statusCode == 200 && "ACTIVA".equals(estado) && "GRATUITO".equals(plan) && 
                fechaInicio != null && fechaExpiracion != null) {
                resultados[testIndex++] = new String[]{"CP037.1 - Usuario suscripción ACTIVA", "APROBO", "200", "plan: GRATUITO, fechas válidas"};
            } else {
                resultados[testIndex++] = new String[]{"CP037.1 - Usuario suscripción ACTIVA", "REPROBO", String.valueOf(statusCode), "estado: " + estado + ", plan: " + plan};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP037.1 - Usuario suscripción ACTIVA", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP037.2 - Usuario con suscripción PENDIENTE_PAGO
        try {
            Integer userId = USER_IDS[1];
            
            // Crear suscripción premium (queda pendiente de pago)
            Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
            given()
                .spec(userRequest(userId))
                .body(suscripcionRequest)
                .when()
                .post("/api/suscripciones/suscribir");
            
            // Verificar estado
            Response response = given()
                .spec(userRequest(userId))
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            String estado = response.jsonPath().getString("estado");
            String plan = response.jsonPath().getString("nombrePlan");
            boolean requierePago = response.jsonPath().getBoolean("requierePago");
            
            if (statusCode == 200 && "PENDIENTE_PAGO".equals(estado) && "PREMIUM".equals(plan) && requierePago) {
                resultados[testIndex++] = new String[]{"CP037.2 - Usuario PENDIENTE_PAGO", "APROBO", "200", "plan: PREMIUM, requierePago: true"};
            } else {
                resultados[testIndex++] = new String[]{"CP037.2 - Usuario PENDIENTE_PAGO", "REPROBO", String.valueOf(statusCode), "estado: " + estado + ", plan: " + plan};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP037.2 - Usuario PENDIENTE_PAGO", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP037.3 - Usuario sin suscripción
        try {
            Integer userId = USER_IDS[2];
            
            // No crear ninguna suscripción para este usuario
            
            // Verificar estado
            Response response = given()
                .spec(userRequest(userId))
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            String estado = response.jsonPath().getString("estado");
            String plan = response.jsonPath().getString("nombrePlan");
            
            if (statusCode == 200 && ("SIN_SUSCRIPCION".equals(estado) || "GRATUITO".equals(plan))) {
                resultados[testIndex++] = new String[]{"CP037.3 - Usuario sin suscripción", "APROBO", "200", "estado: SIN_SUSCRIPCION o plan GRATUITO"};
            } else {
                resultados[testIndex++] = new String[]{"CP037.3 - Usuario sin suscripción", "REPROBO", String.valueOf(statusCode), "estado: " + estado + ", plan: " + plan};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP037.3 - Usuario sin suscripción", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP037.4 - Sin autenticación
        try {
            Response response = given()
                .spec(unauthenticatedRequest())
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            
            if (statusCode == 401) {
                resultados[testIndex++] = new String[]{"CP037.4 - Sin autenticación", "APROBO", "401", "-"};
            } else {
                resultados[testIndex++] = new String[]{"CP037.4 - Sin autenticación", "REPROBO", String.valueOf(statusCode), "Debería retornar 401"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP037.4 - Sin autenticación", "REPROBO", "ERROR", e.getMessage()};
        }

        imprimirReporte("CP037 - Verificación de Estado", resultados);
    }

    @Test
    @DisplayName("CP037.1 - Validar estructura de respuesta suscripción ACTIVA")
    void testEstructuraRespuestaSuscripcionActiva() {
        Integer userId = USER_IDS[3];
        
        // Crear suscripción gratuita activa
        Map<String, Object> suscripcionRequest = crearSuscripcionRequest("GRATUITO", "NONE", userId);
        given()
            .spec(userRequest(userId))
            .body(suscripcionRequest)
            .when()
            .post("/api/suscripciones/suscribir");
        
        // Verificar estructura completa de respuesta
        given()
            .spec(userRequest(userId))
            .when()
            .get("/api/suscripciones/estado")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("id"))
            .body("$", hasKey("estado"))
            .body("$", hasKey("nombrePlan"))
            .body("$", hasKey("fechaCreacion"))
            .body("$", hasKey("fechaInicio"))
            .body("$", hasKey("fechaExpiracion"))
            .body("$", hasKey("diasRestantes"))
            .body("$", hasKey("requierePago"))
            .body("$", hasKey("usuarioId"))
            .body("estado", equalTo("ACTIVA"))
            .body("nombrePlan", equalTo("GRATUITO"))
            .body("requierePago", equalTo(false))
            .body("diasRestantes", greaterThanOrEqualTo(0))
            .body("fechaInicio", notNullValue())
            .body("fechaExpiracion", notNullValue());
    }

    @Test
    @DisplayName("CP037.2 - Validar estructura de respuesta suscripción PENDIENTE_PAGO")
    void testEstructuraRespuestaSuscripcionPendiente() {
        Integer userId = USER_IDS[4];
        
        // Crear suscripción premium pendiente de pago
        Map<String, Object> suscripcionRequest = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
        given()
            .spec(userRequest(userId))
            .body(suscripcionRequest)
            .when()
            .post("/api/suscripciones/suscribir");
        
        // Verificar estructura completa de respuesta
        given()
            .spec(userRequest(userId))
            .when()
            .get("/api/suscripciones/estado")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("id"))
            .body("$", hasKey("estado"))
            .body("$", hasKey("nombrePlan"))
            .body("$", hasKey("fechaCreacion"))
            .body("$", hasKey("requierePago"))
            .body("$", hasKey("usuarioId"))
            .body("$", hasKey("tipoPago"))
            .body("estado", equalTo("PENDIENTE_PAGO"))
            .body("nombrePlan", equalTo("PREMIUM"))
            .body("requierePago", equalTo(true))
            .body("tipoPago", equalTo("TARJETA"));
    }

    @Test
    @DisplayName("CP037.3 - Validar cálculo de días restantes")
    void testCalculoDiasRestantes() {
        Integer userId = USER_IDS[5];
        
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
        
        // Activar la suscripción
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
        
        // Verificar que los días restantes se calculen correctamente
        given()
            .spec(userRequest(userId))
            .when()
            .get("/api/suscripciones/estado")
            .then()
            .statusCode(200)
            .body("diasRestantes", greaterThanOrEqualTo(0))
            .body("diasRestantes", lessThanOrEqualTo(365)); // Máximo un año para suscripción premium
    }

    @Test
    @DisplayName("CP037.4 - Validar formato de fechas")
    void testFormatoFechas() {
        Integer userId = USER_IDS[6];
        
        // Crear suscripción gratuita activa
        Map<String, Object> suscripcionRequest = crearSuscripcionRequest("GRATUITO", "NONE", userId);
        given()
            .spec(userRequest(userId))
            .body(suscripcionRequest)
            .when()
            .post("/api/suscripciones/suscribir");
        
        // Verificar que las fechas tengan formato ISO 8601
        given()
            .spec(userRequest(userId))
            .when()
            .get("/api/suscripciones/estado")
            .then()
            .statusCode(200)
            .body("fechaCreacion", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"))
            .body("fechaInicio", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"))
            .body("fechaExpiracion", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
    }
}
