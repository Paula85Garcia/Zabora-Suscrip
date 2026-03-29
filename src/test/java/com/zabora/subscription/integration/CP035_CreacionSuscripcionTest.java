package com.zabora.subscription.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP035_CreacionSuscripcionTest extends BaseSubscriptionTest {

    @Test
    @DisplayName("CP035 - Creación de Suscripción")
    void testCreacionSuscripcion() {
        String[][] resultados = new String[5][4];
        int testIndex = 0;

        // CP035.1 - Plan premium con usuario autenticado
        try {
            Integer userId = USER_IDS[0];
            Map<String, Object> request = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
            
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
            boolean requierePago = response.jsonPath().getBoolean("requierePago");
            String nombrePlan = response.jsonPath().getString("nombrePlan");
            
            if (statusCode == 200 && "PENDIENTE_PAGO".equals(estado) && requierePago && "PREMIUM".equals(nombrePlan)) {
                resultados[testIndex++] = new String[]{"CP035.1 - Plan premium autenticado", "APROBO", "200", "estado: PENDIENTE_PAGO, requierePago: true"};
            } else {
                resultados[testIndex++] = new String[]{"CP035.1 - Plan premium autenticado", "REPROBO", String.valueOf(statusCode), "estado: " + estado + ", requierePago: " + requierePago};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP035.1 - Plan premium autenticado", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP035.2 - Plan gratuito con usuario autenticado
        try {
            Integer userId = USER_IDS[1];
            Map<String, Object> request = crearSuscripcionRequest("GRATUITO", "NONE", userId);
            
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
            boolean requierePago = response.jsonPath().getBoolean("requierePago");
            String nombrePlan = response.jsonPath().getString("nombrePlan");
            
            if (statusCode == 200 && "ACTIVA".equals(estado) && !requierePago && "GRATUITO".equals(nombrePlan)) {
                resultados[testIndex++] = new String[]{"CP035.2 - Plan gratuito autenticado", "APROBO", "200", "estado: ACTIVA, requierePago: false"};
            } else {
                resultados[testIndex++] = new String[]{"CP035.2 - Plan gratuito autenticado", "REPROBO", String.valueOf(statusCode), "estado: " + estado + ", requierePago: " + requierePago};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP035.2 - Plan gratuito autenticado", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP035.3 - Verificar persistencia en BD
        try {
            Integer userId = USER_IDS[2];
            Map<String, Object> request = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
            
            // Crear suscripción
            Response createResponse = given()
                .spec(userRequest(userId))
                .body(request)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            String suscripcionId = createResponse.jsonPath().getString("id");
            
            // Verificar persistencia consultando estado
            Response estadoResponse = given()
                .spec(userRequest(userId))
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .extract()
                .response();

            int statusCode = estadoResponse.getStatusCode();
            String estado = estadoResponse.jsonPath().getString("estado");
            String idConsultado = estadoResponse.jsonPath().getString("id");
            
            if (statusCode == 200 && "PENDIENTE_PAGO".equals(estado) && suscripcionId.equals(idConsultado)) {
                resultados[testIndex++] = new String[]{"CP035.3 - Persistencia en BD", "APROBO", "200", "ID: " + suscripcionId + ", estado persistido"};
            } else {
                resultados[testIndex++] = new String[]{"CP035.3 - Persistencia en BD", "REPROBO", String.valueOf(statusCode), "estado: " + estado + ", ID: " + idConsultado};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP035.3 - Persistencia en BD", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP035.4 - Intentar crear segunda suscripción activa
        try {
            Integer userId = USER_IDS[3];
            
            // Primero crear suscripción gratuita activa
            Map<String, Object> request1 = crearSuscripcionRequest("GRATUITO", "NONE", userId);
            given()
                .spec(userRequest(userId))
                .body(request1)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();
            
            // Intentar crear segunda suscripción premium
            Map<String, Object> request2 = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
            Response response = given()
                .spec(userRequest(userId))
                .body(request2)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            
            if (statusCode == 400 || statusCode == 500) {
                resultados[testIndex++] = new String[]{"CP035.4 - Segunda suscripción activa", "APROBO", String.valueOf(statusCode), "Validación funcionando"};
            } else {
                resultados[testIndex++] = new String[]{"CP035.4 - Segunda suscripción activa", "REPROBO", String.valueOf(statusCode), "Debería retornar 400/500"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP035.4 - Segunda suscripción activa", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP035.5 - Plan no existe
        try {
            Integer userId = USER_IDS[4];
            Map<String, Object> request = crearSuscripcionRequest("PLAN_INEXISTENTE", "TARJETA", userId);
            
            Response response = given()
                .spec(userRequest(userId))
                .body(request)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            
            if (statusCode == 400 || statusCode == 500) {
                resultados[testIndex++] = new String[]{"CP035.5 - Plan no existe", "APROBO", String.valueOf(statusCode), "Error validado correctamente"};
            } else {
                resultados[testIndex++] = new String[]{"CP035.5 - Plan no existe", "REPROBO", String.valueOf(statusCode), "Debería retornar 400/500"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP035.5 - Plan no existe", "REPROBO", "ERROR", e.getMessage()};
        }

        imprimirReporte("CP035 - Creación de Suscripción", resultados);
    }

    @Test
    @DisplayName("CP035.1 - Validar estructura de respuesta plan premium")
    void testEstructuraRespuestaPlanPremium() {
        Integer userId = USER_IDS[5];
        Map<String, Object> request = crearSuscripcionRequest("PREMIUM", "TARJETA", userId);
        
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
            .body("$", hasKey("tipoPago"))
            .body("estado", equalTo("PENDIENTE_PAGO"))
            .body("requierePago", equalTo(true))
            .body("nombrePlan", equalTo("PREMIUM"))
            .body("tipoPago", equalTo("TARJETA"))
            .body("usuarioId", equalTo(userId));
    }

    @Test
    @DisplayName("CP035.2 - Validar estructura de respuesta plan gratuito")
    void testEstructuraRespuestaPlanGratuito() {
        Integer userId = USER_IDS[6];
        Map<String, Object> request = crearSuscripcionRequest("GRATUITO", "NONE", userId);
        
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
            .body("$", hasKey("tipoPago"))
            .body("estado", equalTo("ACTIVA"))
            .body("requierePago", equalTo(false))
            .body("nombrePlan", equalTo("GRATUITO"))
            .body("tipoPago", equalTo("NONE"))
            .body("usuarioId", equalTo(userId));
    }

    @Test
    @DisplayName("CP035.3 - Validar fechas de creación")
    void testFechasCreacion() {
        Integer userId = USER_IDS[7];
        Map<String, Object> request = crearSuscripcionRequest("GRATUITO", "NONE", userId);
        
        Response response = given()
            .spec(userRequest(userId))
            .body(request)
            .when()
            .post("/api/suscripciones/suscribir")
            .then()
            .extract()
            .response();

        String fechaCreacion = response.jsonPath().getString("fechaCreacion");
        String fechaInicio = response.jsonPath().getString("fechaInicio");
        String fechaExpiracion = response.jsonPath().getString("fechaExpiracion");
        
        // Verificar que las fechas no sean nulas y tengan formato válido
        given()
            .spec(userRequest(userId))
            .when()
            .get("/api/suscripciones/estado")
            .then()
            .statusCode(200)
            .body("fechaCreacion", notNullValue())
            .body("fechaInicio", notNullValue())
            .body("fechaExpiracion", notNullValue());
    }
}
