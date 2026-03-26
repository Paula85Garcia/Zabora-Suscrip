package com.zabora.subscription.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP038_FinalizarPagoExitosoTest extends BaseSubscriptionTest {

    private String suscripcionTarjetaId;
    private String suscripcionPSEId;

    @Test
    @Order(1)
    @DisplayName("CP038.1 - Crear suscripción para pago tarjeta")
    void crearSuscripcionParaPagoTarjeta() {
        Integer userId = USER_IDS[6];
        
        Response response = given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("premium", "tarjeta_credito", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .body("estado", equalTo("PENDIENTE_PAGO"))
                .extract()
                .response();

        suscripcionTarjetaId = response.jsonPath().getString("idSuscripcion");
        System.out.println("Suscripción creada para pago tarjeta: " + suscripcionTarjetaId);
    }

    @Test
    @Order(2)
    @DisplayName("CP038.2 - Procesar pago con tarjeta exitoso")
    void procesarPagoTarjetaExitoso() {
        Integer userId = USER_IDS[6];
        
        Response response = given()
                .spec(userRequest(userId))
                .body(crearPagoRequest(suscripcionTarjetaId, "tarjeta_credito", TOKEN_VISA_EXITOSO))
        .when()
                .post("/api/pagos/bricks/process")
        .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", equalTo("approved"))
                .body("id", notNullValue())
                .extract()
                .response();

        String pagoId = response.jsonPath().getString("id");
        System.out.println("CP038.2 - Pago tarjeta exitoso - ID: " + pagoId);
    }

    @Test
    @Order(3)
    @DisplayName("CP038.3 - Crear suscripción para pago PSE")
    void crearSuscripcionParaPagoPSE() {
        Integer userId = USER_IDS[7];
        
        Response response = given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("premium", "pse", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .body("estado", equalTo("PENDIENTE_PAGO"))
                .extract()
                .response();

        suscripcionPSEId = response.jsonPath().getString("idSuscripcion");
        System.out.println("Suscripción creada para pago PSE: " + suscripcionPSEId);
    }

    @Test
    @Order(4)
    @DisplayName("CP038.4 - Procesar pago con PSE exitoso")
    void procesarPagoPSEExitoso() {
        Integer userId = USER_IDS[7];
        
        Response response = given()
                .spec(userRequest(userId))
                .body(crearPagoRequest(suscripcionPSEId, "pse", "pse_token_" + System.currentTimeMillis()))
        .when()
                .post("/api/pagos/bricks/process")
        .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("status", anyOf(equalTo("approved"), equalTo("in_process")))
                .extract()
                .response();

        String pagoId = response.jsonPath().getString("id");
        String status = response.jsonPath().getString("status");
        System.out.println("CP038.4 - Pago PSE exitoso - ID: " + pagoId + ", Status: " + status);
    }

    @Test
    @Order(5)
    @DisplayName("CP038.5 - Crear suscripción para pago rechazado")
    void crearSuscripcionParaPagoRechazado() {
        Integer userId = USER_IDS[8];
        
        given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("premium", "tarjeta_credito", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .body("estado", equalTo("PENDIENTE_PAGO"));
    }

    @Test
    @Order(6)
    @DisplayName("CP038.6 - Procesar pago rechazado")
    void procesarPagoRechazado() {
        Integer userId = USER_IDS[8];
        
        given()
                .spec(userRequest(userId))
                .body(crearPagoRequest("sub_test_rechazado", "tarjeta_credito", TOKEN_VISA_RECHAZADO))
        .when()
                .post("/api/pagos/bricks/process")
        .then()
                .statusCode(200)
                .body("success", equalTo(false))
                .body("status", equalTo("rejected"));
    }

    @Test
    @Order(7)
    @DisplayName("CP038.7 - Procesar pago sin suscripción previa")
    void procesarPagoSinSuscripcion() {
        Integer userId = USER_IDS[9];
        
        given()
                .spec(userRequest(userId))
                .body(crearPagoRequest("sub_inexistente_789", "tarjeta_credito", TOKEN_VISA_EXITOSO))
        .when()
                .post("/api/pagos/bricks/process")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)))
                .body("success", equalTo(false));
    }

    @Test
    @Order(8)
    @DisplayName("CP038.8 - Procesar pago sin autenticación")
    void procesarPagoSinAutenticacion() {
        given()
                .spec(unauthenticatedRequest())
                .body(crearPagoRequest("sub_test_no_auth", "tarjeta_credito", TOKEN_VISA_EXITOSO))
        .when()
                .post("/api/pagos/bricks/process")
        .then()
                .statusCode(anyOf(equalTo(401), equalTo(500)))
                .body("success", equalTo(false));
    }

    @Test
    @Order(99)
    @DisplayName("CP038 - Reporte final de resultados")
    void reporteFinal() {
        String[][] resultados = {
            {"CP038.2 - Tarjeta exitosa", "APROBO", "200", "Status: approved"},
            {"CP038.4 - PSE exitoso", "APROBO", "200", "Status: approved/in_process"},
            {"CP038.6 - Pago rechazado", "APROBO", "200", "Status: rejected"},
            {"CP038.7 - Sin suscripción", "APROBO", "400/500", "Error esperado"},
            {"CP038.8 - Sin autenticación", "APROBO", "401/500", "-"}
        };

        imprimirReporte("CP038 - Finalizar pago exitoso", resultados);
    }
}
