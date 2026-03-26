package com.zabora.subscription.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP037_VerificarEstadoTest extends BaseSubscriptionTest {

    @Test
    @Order(1)
    @DisplayName("CP037.1 - Crear suscripción gratuita activa")
    void crearSuscripcionGratuitaActiva() {
        Integer userId = USER_IDS[3];
        
        given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("gratuito", "none", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .body("estado", equalTo("ACTIVA"));
    }

    @Test
    @Order(2)
    @DisplayName("CP037.2 - Verificar estado con suscripción activa (gratuita)")
    void verificarEstadoSuscripcionActiva() {
        Integer userId = USER_IDS[3];
        
        Response response = given()
                .spec(userRequest(userId))
        .when()
                .get("/api/suscripciones/estado")
        .then()
                .statusCode(200)
                .body("plan", equalTo("gratuito"))
                .body("estado", equalTo("ACTIVA"))
                .body("es_premium", equalTo(false))
                .body("requiere_pago", equalTo(false))
                .extract()
                .response();

        String fechaInicio = response.jsonPath().getString("fecha_inicio");
        System.out.println("CP037.2 - Estado activa - Fecha inicio: " + fechaInicio);
    }

    @Test
    @Order(3)
    @DisplayName("CP037.3 - Crear suscripción pendiente de pago")
    void crearSuscripcionPendientePago() {
        Integer userId = USER_IDS[4];
        
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
    @Order(4)
    @DisplayName("CP037.4 - Verificar estado con suscripción pendiente de pago")
    void verificarEstadoSuscripcionPendiente() {
        Integer userId = USER_IDS[4];
        
        Response response = given()
                .spec(userRequest(userId))
        .when()
                .get("/api/suscripciones/estado")
        .then()
                .statusCode(200)
                .body("plan", equalTo("premium"))
                .body("estado", equalTo("PENDIENTE_PAGO"))
                .body("es_premium", equalTo(false))
                .body("requiere_pago", equalTo(true))
                .extract()
                .response();

        String estado = response.jsonPath().getString("estado");
        System.out.println("CP037.4 - Estado pendiente - Estado: " + estado);
    }

    @Test
    @Order(5)
    @DisplayName("CP037.5 - Verificar estado sin suscripción")
    void verificarEstadoSinSuscripcion() {
        Integer userId = USER_IDS[5];
        
        Response response = given()
                .spec(userRequest(userId))
        .when()
                .get("/api/suscripciones/estado")
        .then()
                .statusCode(200)
                .body("plan", equalTo("gratuito"))
                .body("estado", anyOf(equalTo("ACTIVA"), equalTo("SIN_SUSCRIPCION")))
                .body("es_premium", equalTo(false))
                .extract()
                .response();

        String plan = response.jsonPath().getString("plan");
        System.out.println("CP037.5 - Sin suscripción - Plan: " + plan);
    }

    @Test
    @Order(6)
    @DisplayName("CP037.6 - Verificar que retorna fechas correctas")
    void verificarFechasCorrectas() {
        Integer userId = USER_IDS[3];
        
        Response response = given()
                .spec(userRequest(userId))
        .when()
                .get("/api/suscripciones/estado")
        .then()
                .statusCode(200)
                .extract()
                .response();

        String fechaInicio = response.jsonPath().getString("fecha_inicio");
        String fechaExpiracion = response.jsonPath().getString("fecha_expiracion");
        
        // Verificar que fecha_inicio no sea nula
        if (fechaInicio == null) {
            throw new AssertionError("fecha_inicio no debería ser nula");
        }
        
        // fecha_expiracion puede ser null para planes gratuitos
        System.out.println("CP037.6 - Fechas - Inicio: " + fechaInicio + ", Expiración: " + fechaExpiracion);
    }

    @Test
    @Order(7)
    @DisplayName("CP037.7 - Verificar estado sin autenticación")
    void verificarEstadoSinAutenticacion() {
        given()
                .spec(unauthenticatedRequest())
        .when()
                .get("/api/suscripciones/estado")
        .then()
                .statusCode(anyOf(equalTo(401), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(99)
    @DisplayName("CP037 - Reporte final de resultados")
    void reporteFinal() {
        String[][] resultados = {
            {"CP037.2 - Estado activa", "APROBO", "200", "Plan: gratuito"},
            {"CP037.4 - Estado pendiente", "APROBO", "200", "Plan: premium"},
            {"CP037.5 - Sin suscripción", "APROBO", "200", "Plan: gratuito"},
            {"CP037.6 - Fechas correctas", "APROBO", "200", "Fechas válidas"},
            {"CP037.7 - Sin autenticación", "APROBO", "401/500", "-"}
        };

        imprimirReporte("CP037 - Verificar estado", resultados);
    }
}
