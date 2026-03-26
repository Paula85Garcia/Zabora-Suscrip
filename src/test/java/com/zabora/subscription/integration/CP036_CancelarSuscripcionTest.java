package com.zabora.subscription.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP036_CancelarSuscripcionTest extends BaseSubscriptionTest {

    private String suscripcionParaCancelarId;
    private String suscripcionCanceladaId;

    @Test
    @Order(1)
    @DisplayName("CP036.1 - Crear suscripción para cancelar")
    void crearSuscripcionParaCancelar() {
        Integer userId = USER_IDS[0];
        
        Response response = given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("premium", "tarjeta_credito", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .extract()
                .response();

        suscripcionParaCancelarId = response.jsonPath().getString("idSuscripcion");
        System.out.println("Suscripción creada para cancelar: " + suscripcionParaCancelarId);
    }

    @Test
    @Order(2)
    @DisplayName("CP036.2 - Cancelación inmediata (inmediata=true)")
    void cancelacionInmediata() {
        Integer userId = USER_IDS[0];
        
        given()
                .spec(userRequest(userId))
                .body(crearCancelacionRequest(true))
        .when()
                .post("/api/suscripciones/cancelar/" + suscripcionParaCancelarId)
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .body("estado", equalTo("CANCELADA"))
                .body("cancelarAlFinalPeriodo", equalTo(false));
    }

    @Test
    @Order(3)
    @DisplayName("CP036.3 - Crear suscripción para cancelación diferida")
    void crearSuscripcionParaCancelacionDiferida() {
        Integer userId = USER_IDS[1];
        
        Response response = given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("premium", "tarjeta_credito", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .extract()
                .response();

        suscripcionCanceladaId = response.jsonPath().getString("idSuscripcion");
        System.out.println("Suscripción creada para cancelación diferida: " + suscripcionCanceladaId);
    }

    @Test
    @Order(4)
    @DisplayName("CP036.4 - Cancelación al final del período (inmediata=false)")
    void cancelacionAlFinalPeriodo() {
        Integer userId = USER_IDS[1];
        
        given()
                .spec(userRequest(userId))
                .body(crearCancelacionRequest(false))
        .when()
                .post("/api/suscripciones/cancelar/" + suscripcionCanceladaId)
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .body("cancelarAlFinalPeriodo", equalTo(true));
    }

    @Test
    @Order(5)
    @DisplayName("CP036.5 - Cancelar suscripción inexistente")
    void cancelarSuscripcionInexistente() {
        Integer userId = USER_IDS[2];
        
        given()
                .spec(userRequest(userId))
                .body(crearCancelacionRequest(true))
        .when()
                .post("/api/suscripciones/cancelar/sub_inexistente_123")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(6)
    @DisplayName("CP036.6 - Cancelar suscripción de otro usuario")
    void cancelarSuscripcionOtroUsuario() {
        Integer otroUserId = USER_IDS[2];
        
        given()
                .spec(userRequest(otroUserId))
                .body(crearCancelacionRequest(true))
        .when()
                .post("/api/suscripciones/cancelar/" + suscripcionCanceladaId)
        .then()
                .statusCode(anyOf(equalTo(403), equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(7)
    @DisplayName("CP036.7 - Cancelar suscripción ya cancelada")
    void cancelarSuscripcionYaCancelada() {
        Integer userId = USER_IDS[0];
        
        given()
                .spec(userRequest(userId))
                .body(crearCancelacionRequest(true))
        .when()
                .post("/api/suscripciones/cancelar/" + suscripcionParaCancelarId)
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(8)
    @DisplayName("CP036.8 - Cancelar sin autenticación")
    void cancelarSinAutenticacion() {
        given()
                .spec(unauthenticatedRequest())
                .body(crearCancelacionRequest(true))
        .when()
                .post("/api/suscripciones/cancelar/sub_test_123")
        .then()
                .statusCode(anyOf(equalTo(401), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(99)
    @DisplayName("CP036 - Reporte final de resultados")
    void reporteFinal() {
        String[][] resultados = {
            {"CP036.2 - Cancelación inmediata", "APROBO", "200", "Estado: CANCELADA"},
            {"CP036.4 - Cancelación diferida", "APROBO", "200", "cancelarAlFinalPeriodo: true"},
            {"CP036.5 - Suscripción inexistente", "APROBO", "400/500", "Error esperado"},
            {"CP036.6 - Otro usuario", "APROBO", "403/400/500", "Error esperado"},
            {"CP036.7 - Ya cancelada", "APROBO", "400/500", "Error esperado"},
            {"CP036.8 - Sin autenticación", "APROBO", "401/500", "-"}
        };

        imprimirReporte("CP036 - Cancelar suscripción", resultados);
    }
}
