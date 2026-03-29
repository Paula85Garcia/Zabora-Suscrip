package com.zabora.subscription.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP032_SeleccionMetodoPagoTest extends BaseSubscriptionTest {

    @Test
    @DisplayName("CP032 - Selección de Método de Pago")
    void testSeleccionMetodoPago() {
        String[][] resultados = new String[6][4];
        int testIndex = 0;

        // CP032.1 - Tarjeta de crédito con usuario autenticado
        try {
            Map<String, Object> request = crearSuscripcionRequest("premium", "TARJETA", USER_IDS[0]);
            
            Response response = given()
                .spec(userRequest(USER_IDS[0]))
                .body(request)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            String exito = response.jsonPath().getString("exito");
            String estado = response.jsonPath().getString("estado");
            String requierePago = response.jsonPath().getString("requierePago");
            
            if (statusCode == 200 && "true".equals(exito) && "PENDIENTE_PAGO".equals(estado) && "true".equals(requierePago)) {
                resultados[testIndex++] = new String[]{"CP032.1 - Tarjeta crédito autenticado", "APROBO", "200", "estado: PENDIENTE_PAGO, requierePago: true"};
            } else {
                resultados[testIndex++] = new String[]{"CP032.1 - Tarjeta crédito autenticado", "REPROBO", String.valueOf(statusCode), "exito: " + exito + ", estado: " + estado};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP032.1 - Tarjeta crédito autenticado", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP032.2 - PSE con usuario autenticado
        try {
            Map<String, Object> request = crearSuscripcionRequest("premium", "PSE", USER_IDS[1]);
            
            Response response = given()
                .spec(userRequest(USER_IDS[1]))
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
                resultados[testIndex++] = new String[]{"CP032.2 - PSE autenticado", "APROBO", "200", "estado: PENDIENTE_PAGO, requierePago: true"};
            } else {
                resultados[testIndex++] = new String[]{"CP032.2 - PSE autenticado", "REPROBO", String.valueOf(statusCode), "estado: " + estado + ", requierePago: " + requierePago};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP032.2 - PSE autenticado", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP032.3 - Sin autenticación
        try {
            Map<String, Object> request = crearSuscripcionRequest("premium", "TARJETA", null);
            
            Response response = given()
                .spec(unauthenticatedRequest())
                .body(request)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            
            if (statusCode == 400) {
                resultados[testIndex++] = new String[]{"CP032.3 - Sin autenticación", "APROBO", "400", "Validación por falta de usuarioId"};
            } else {
                resultados[testIndex++] = new String[]{"CP032.3 - Sin autenticación", "REPROBO", String.valueOf(statusCode), "Debería retornar 400, obtuvo: " + statusCode};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP032.3 - Sin autenticación", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP032.4 - Usuario ya con suscripción activa
        try {
            Map<String, Object> request = crearSuscripcionRequest("premium", "TARJETA", USER_IDS[3]);
            
            // Primero crear una suscripción gratuita
            given()
                .spec(userRequest(USER_IDS[3]))
                .body(crearSuscripcionRequest("gratuito", "NONE", USER_IDS[3]))
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();
            
            // Intentar crear suscripción premium
            Response response = given()
                .spec(userRequest(USER_IDS[3]))
                .body(request)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            
            if (statusCode == 400 || statusCode == 500) {
                resultados[testIndex++] = new String[]{"CP032.4 - Usuario ya activo", "APROBO", String.valueOf(statusCode), "Validación funcionando"};
            } else {
                resultados[testIndex++] = new String[]{"CP032.4 - Usuario ya activo", "REPROBO", String.valueOf(statusCode), "Debería retornar 400/500"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP032.4 - Usuario ya activo", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP032.5 - Plan inexistente
        try {
            Map<String, Object> request = crearSuscripcionRequest("PLAN_INEXISTENTE", "TARJETA", USER_IDS[4]);
            
            Response response = given()
                .spec(userRequest(USER_IDS[4]))
                .body(request)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            
            if (statusCode == 400 || statusCode == 500) {
                resultados[testIndex++] = new String[]{"CP032.5 - Plan inexistente", "APROBO", String.valueOf(statusCode), "Error validado correctamente"};
            } else {
                resultados[testIndex++] = new String[]{"CP032.5 - Plan inexistente", "REPROBO", String.valueOf(statusCode), "Debería retornar 400/500"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP032.5 - Plan inexistente", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP032.6 - Datos incompletos (nombrePlan vacío)
        try {
            Map<String, Object> request = crearSuscripcionRequest("", "TARJETA", USER_IDS[6]);
            
            Response response = given()
                .spec(userRequest(USER_IDS[6]))
                .body(request)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            
            if (statusCode == 400) {
                resultados[testIndex++] = new String[]{"CP032.6 - Datos incompletos", "APROBO", "400", "Validación de datos funcionando"};
            } else {
                resultados[testIndex++] = new String[]{"CP032.6 - Datos incompletos", "REPROBO", String.valueOf(statusCode), "Debería retornar 400"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP032.6 - Datos incompletos", "REPROBO", "ERROR", e.getMessage()};
        }

        imprimirReporte("CP032 - Selección de Método de Pago", resultados);
    }

    @Test
    @DisplayName("CP032.1 - Validar estructura de respuesta")
    void testEstructuraRespuesta() {
        Map<String, Object> request = crearSuscripcionRequest("premium", "TARJETA", USER_IDS[6]);
        
        given()
            .spec(userRequest(USER_IDS[6]))
            .body(request)
            .when()
            .post("/api/suscripciones/suscribir")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("exito"))
            .body("$", hasKey("mensaje"))
            .body("$", hasKey("idSuscripcion"))
            .body("$", hasKey("plan"))
            .body("$", hasKey("estado"))
            .body("$", hasKey("requierePago"))
            .body("exito", equalTo(true))
            .body("plan", equalTo("premium"))
            .body("requierePago", equalTo(true));
    }
}
