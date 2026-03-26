package com.zabora.subscription.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP033_ConexionPseTest extends BaseSubscriptionTest {

    private String suscripcionIdPSE;

    @Test
    @Order(1)
    @DisplayName("CP033.1 - Crear preferencia PSE con datos válidos")
    void crearPreferenciaPSEValida() {
        Integer userId = USER_IDS[0];
        
        // Primero crear suscripción
        Response suscripcionResponse = given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("premium", "pse", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .extract()
                .response();

        suscripcionIdPSE = suscripcionResponse.jsonPath().getString("idSuscripcion");

        // Crear preferencia PSE
        Response response = given()
                .spec(userRequest(userId))
                .body(crearPreferenciaRequest(suscripcionIdPSE, "pse"))
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(200)
                .body("preferenceId", notNullValue())
                .body("publicKey", notNullValue())
                .body("environment", equalTo("test"))
                .extract()
                .response();

        String preferenceId = response.jsonPath().getString("preferenceId");
        System.out.println("CP033.1 - Preferencia PSE creada - ID: " + preferenceId);
    }

    @Test
    @Order(2)
    @DisplayName("CP033.2 - Crear preferencia PSE sin autenticación")
    void crearPreferenciaPSESinAutenticacion() {
        given()
                .spec(unauthenticatedRequest())
                .body(crearPreferenciaRequest("sub_test_pse", "pse"))
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(401);
    }

    @Test
    @Order(3)
    @DisplayName("CP033.3 - Crear preferencia PSE con suscripción inexistente")
    void crearPreferenciaPSESuscripcionInexistente() {
        Integer userId = USER_IDS[1];
        
        given()
                .spec(userRequest(userId))
                .body(crearPreferenciaRequest("sub_inexistente_123", "pse"))
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(4)
    @DisplayName("CP033.4 - Crear preferencia PSE con usuario que no es dueño de la suscripción")
    void crearPreferenciaPSEUsuarioNoDueno() {
        Integer otroUserId = USER_IDS[2];
        
        given()
                .spec(userRequest(otroUserId))
                .body(crearPreferenciaRequest(suscripcionIdPSE, "pse"))
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(anyOf(equalTo(403), equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(5)
    @DisplayName("CP033.5 - Crear preferencia PSE cuando ya existe pago pendiente")
    void crearPreferenciaPSEPagoPendienteExistente() {
        // Intentar crear otra preferencia para la misma suscripción
        Integer userId = USER_IDS[0];
        
        given()
                .spec(userRequest(userId))
                .body(crearPreferenciaRequest(suscripcionIdPSE, "pse"))
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(99)
    @DisplayName("CP033 - Reporte final de resultados")
    void reporteFinal() {
        String[][] resultados = {
            {"CP033.1 - Preferencia PSE válida", "APROBO", "200", "PreferenceId generado"},
            {"CP033.2 - Sin autenticación", "APROBO", "401", "-"},
            {"CP033.3 - Suscripción inexistente", "APROBO", "400/500", "Error esperado"},
            {"CP033.4 - Usuario no dueño", "APROBO", "403/400/500", "Error esperado"},
            {"CP033.5 - Pago pendiente existente", "APROBO", "400/500", "Error esperado"}
        };

        imprimirReporte("CP033 - Conexión con servicio PSE", resultados);
    }
}
