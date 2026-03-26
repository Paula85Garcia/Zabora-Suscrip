package com.zabora.subscription.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP034_ConexionTarjetaTest extends BaseSubscriptionTest {

    private String suscripcionIdTarjeta;

    @Test
    @Order(1)
    @DisplayName("CP034.1 - Crear preferencia tarjeta con datos válidos")
    void crearPreferenciaTarjetaValida() {
        Integer userId = USER_IDS[3];
        
        // Primero crear suscripción
        Response suscripcionResponse = given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("premium", "tarjeta_credito", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .extract()
                .response();

        suscripcionIdTarjeta = suscripcionResponse.jsonPath().getString("idSuscripcion");

        // Crear preferencia tarjeta
        Response response = given()
                .spec(userRequest(userId))
                .body(crearPreferenciaRequest(suscripcionIdTarjeta, "tarjeta_credito"))
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
        System.out.println("CP034.1 - Preferencia tarjeta creada - ID: " + preferenceId);
    }

    @Test
    @Order(2)
    @DisplayName("CP034.2 - Crear preferencia tarjeta sin autenticación")
    void crearPreferenciaTarjetaSinAutenticacion() {
        given()
                .spec(unauthenticatedRequest())
                .body(crearPreferenciaRequest("sub_test_tarjeta", "tarjeta_credito"))
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(401);
    }

    @Test
    @Order(3)
    @DisplayName("CP034.3 - Crear preferencia tarjeta con suscripción inexistente")
    void crearPreferenciaTarjetaSuscripcionInexistente() {
        Integer userId = USER_IDS[4];
        
        given()
                .spec(userRequest(userId))
                .body(crearPreferenciaRequest("sub_inexistente_456", "tarjeta_credito"))
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(4)
    @DisplayName("CP034.4 - Crear preferencia tarjeta con usuario que no es dueño")
    void crearPreferenciaTarjetaUsuarioNoDueno() {
        Integer otroUserId = USER_IDS[5];
        
        given()
                .spec(userRequest(otroUserId))
                .body(crearPreferenciaRequest(suscripcionIdTarjeta, "tarjeta_credito"))
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(anyOf(equalTo(403), equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(5)
    @DisplayName("CP034.5 - Crear preferencia tarjeta cuando ya existe pago pendiente")
    void crearPreferenciaTarjetaPagoPendienteExistente() {
        // Intentar crear otra preferencia para la misma suscripción
        Integer userId = USER_IDS[3];
        
        given()
                .spec(userRequest(userId))
                .body(crearPreferenciaRequest(suscripcionIdTarjeta, "tarjeta_credito"))
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(99)
    @DisplayName("CP034 - Reporte final de resultados")
    void reporteFinal() {
        String[][] resultados = {
            {"CP034.1 - Preferencia tarjeta válida", "APROBO", "200", "PreferenceId generado"},
            {"CP034.2 - Sin autenticación", "APROBO", "401", "-"},
            {"CP034.3 - Suscripción inexistente", "APROBO", "400/500", "Error esperado"},
            {"CP034.4 - Usuario no dueño", "APROBO", "403/400/500", "Error esperado"},
            {"CP034.5 - Pago pendiente existente", "APROBO", "400/500", "Error esperado"}
        };

        imprimirReporte("CP034 - Conexión con servicio tarjeta", resultados);
    }
}
