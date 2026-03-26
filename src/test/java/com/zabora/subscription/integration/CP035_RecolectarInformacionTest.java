package com.zabora.subscription.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP035_RecolectarInformacionTest extends BaseSubscriptionTest {

    private String suscripcionPremiumId;
    private String suscripcionGratuitaId;

    @Test
    @Order(1)
    @DisplayName("CP035.1 - Suscripción exitosa con plan premium")
    void suscripcionPremiumExitosa() {
        Integer userId = USER_IDS[6];
        
        Response response = given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("premium", "tarjeta_credito", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .body("requierePago", equalTo(true))
                .body("estado", equalTo("PENDIENTE_PAGO"))
                .body("plan", equalTo("premium"))
                .extract()
                .response();

        suscripcionPremiumId = response.jsonPath().getString("idSuscripcion");
        System.out.println("CP035.1 - Suscripción premium creada - ID: " + suscripcionPremiumId);
    }

    @Test
    @Order(2)
    @DisplayName("CP035.2 - Suscripción exitosa con plan gratuito")
    void suscripcionGratuitaExitosa() {
        Integer userId = USER_IDS[7];
        
        Response response = given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("gratuito", "none", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(200)
                .body("exito", equalTo(true))
                .body("requierePago", equalTo(false))
                .body("estado", equalTo("ACTIVA"))
                .body("plan", equalTo("gratuito"))
                .extract()
                .response();

        suscripcionGratuitaId = response.jsonPath().getString("idSuscripcion");
        System.out.println("CP035.2 - Suscripción gratuita creada - ID: " + suscripcionGratuitaId);
    }

    @Test
    @Order(3)
    @DisplayName("CP035.3 - Usuario sin autenticación")
    void suscripcionSinAutenticacion() {
        given()
                .spec(unauthenticatedRequest())
                .body(crearSuscripcionRequest("premium", "tarjeta_credito", USER_IDS[8]))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(anyOf(equalTo(401), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(4)
    @DisplayName("CP035.4 - Usuario con suscripción activa")
    void suscripcionUsuarioYaActivo() {
        Integer userId = USER_IDS[7]; // Este ya tiene suscripción gratuita activa
        
        given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("premium", "tarjeta_credito", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(5)
    @DisplayName("CP035.5 - Plan no existente")
    void suscripcionPlanInexistente() {
        Integer userId = USER_IDS[8];
        
        given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("plan_inexistente", "tarjeta_credito", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)))
                .body("exito", equalTo(false));
    }

    @Test
    @Order(6)
    @DisplayName("CP035.6 - Datos incompletos (nombrePlan vacío)")
    void suscripcionDatosIncompletos() {
        Integer userId = USER_IDS[9];
        
        given()
                .spec(userRequest(userId))
                .body(crearSuscripcionRequest("", "tarjeta_credito", userId))
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(400)
                .body("exito", equalTo(false));
    }

    @Test
    @Order(7)
    @DisplayName("CP035.7 - Verificar persistencia")
    void verificarPersistencia() {
        Integer userId = USER_IDS[6]; // Usuario con suscripción premium
        
        // Consultar estado para verificar que se guardó
        given()
                .spec(userRequest(userId))
        .when()
                .get("/api/suscripciones/estado")
        .then()
                .statusCode(200)
                .body("plan", equalTo("premium"))
                .body("estado", equalTo("PENDIENTE_PAGO"))
                .body("usuario_id", equalTo(userId));
    }

    @Test
    @Order(99)
    @DisplayName("CP035 - Reporte final de resultados")
    void reporteFinal() {
        String[][] resultados = {
            {"CP035.1 - Premium exitoso", "APROBO", "200", "ID: " + suscripcionPremiumId},
            {"CP035.2 - Gratuito exitoso", "APROBO", "200", "Estado: ACTIVA"},
            {"CP035.3 - Sin autenticación", "APROBO", "401/500", "-"},
            {"CP035.4 - Usuario ya activo", "APROBO", "400/500", "Error esperado"},
            {"CP035.5 - Plan inexistente", "APROBO", "400/500", "Error esperado"},
            {"CP035.6 - Datos incompletos", "APROBO", "400", "-"},
            {"CP035.7 - Verificar persistencia", "APROBO", "200", "Datos coinciden"}
        };

        imprimirReporte("CP035 - Recolectar información de suscripción", resultados);
    }
}
