package com.zabora.subscription.integration.controller;

import com.zabora.subscription.integration.BaseIntegrationTest;
import com.zabora.subscription.integration.config.TestDataFactory;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SuscripcionControladorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UsuarioSuscripcionRepositorio suscripcionRepo;

    private static String suscripcionIdGratuita;
    private static String suscripcionIdPremium;

    @Test
    @Order(1)
    @DisplayName("Crea suscripción gratuita y queda ACTIVA")
    void debeCrearSuscripcionGratuitaYActivarla() {
        SolicitudSuscripcionDTO solicitud = TestDataFactory.solicitudGratuito();

        suscripcionIdGratuita = given()
                .spec(authenticatedRequest(1, "test@example.com", "USER"))
                .body(solicitud)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .statusCode(200)
                .body("exito", is(true))
                .body("mensaje", containsStringIgnoringCase("gratuita"))
                .body("plan", equalTo("gratuito"))
                .body("estado", equalTo("ACTIVA"))
                .body("requierePago", is(false))
                .body("idSuscripcion", notNullValue())
                .extract()
                .path("idSuscripcion");
    }

    @Test
    @Order(2)
    @DisplayName("Crea suscripción premium y queda PENDIENTE_PAGO")
    void debeCrearSuscripcionPremiumPendiente() {
        SolicitudSuscripcionDTO solicitud = TestDataFactory.solicitudPremium();

        suscripcionIdPremium = given()
                .spec(authenticatedRequest(2, "premium@example.com", "USER"))
                .body(solicitud)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .statusCode(200)
                .body("exito", is(true))
                .body("plan", equalTo("premium"))
                .body("estado", equalTo("PENDIENTE_PAGO"))
                .body("requierePago", is(true))
                .body("idSuscripcion", notNullValue())
                .extract()
                .path("idSuscripcion");
    }

    @Test
    @Order(3)
    @DisplayName("Segunda solicitud de gratuito es idempotente (200, mismo plan)")
    void segundaSuscripcionGratuitaIdempotente() {
        SolicitudSuscripcionDTO solicitud = TestDataFactory.solicitudGratuito();

        given()
                .spec(authenticatedRequest(1, "test@example.com", "USER"))
                .body(solicitud)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .statusCode(200)
                .body("plan", equalTo("gratuito"))
                .body("estado", equalTo("ACTIVA"));
    }

    @Test
    @Order(4)
    @DisplayName("GET estado del usuario autenticado")
    void debeObtenerEstadoUsuario() {
        given()
                .spec(authenticatedRequest(1, "test@example.com", "USER"))
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .statusCode(200)
                .body("plan", equalTo("gratuito"))
                .body("estado", equalTo("ACTIVA"));
    }

    @Test
    @Order(5)
    @DisplayName("GET verificar/{userId} devuelve estado de suscripción")
    void debeVerificarSuscripcionUsuario() {
        given()
                .spec(authenticatedRequest(999, "admin@example.com", "ADMIN"))
                .when()
                .get("/api/suscripciones/verificar/1")
                .then()
                .statusCode(200)
                .body("plan", equalTo("gratuito"))
                .body("estado", equalTo("ACTIVA"));
    }

    @Test
    @Order(6)
    @DisplayName("Lista planes (público)")
    void debeListarPlanes() {
        given()
                .spec(unauthenticatedRequest())
                .when()
                .get("/api/suscripciones/planes")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(2)))
                .body("[0].nombre", notNullValue())
                .body("[0].precio", notNullValue());
    }

    @Test
    @Order(7)
    @DisplayName("Cancelación inmediata (mock auth downgrade)")
    void debeCancelarSuscripcionInmediata() {
        given()
                .spec(authenticatedRequest(1, "test@example.com", "USER"))
                .queryParam("inmediata", true)
                .when()
                .post("/api/suscripciones/cancelar/" + suscripcionIdGratuita)
                .then()
                .statusCode(200)
                .body("exito", is(true))
                .body("estado", equalTo("CANCELADA"))
                .body("cancelarAlFinalPeriodo", is(false))
                .body("mensaje", containsStringIgnoringCase("inmediat"));

        var suscripcion = suscripcionRepo.findById(suscripcionIdGratuita).orElseThrow();
        Assertions.assertEquals(EstadoSuscripcion.CANCELADA, suscripcion.getEstado());
    }

    @Test
    @Order(8)
    @DisplayName("Cancelación al final del periodo (premium pendiente)")
    void debeCancelarSuscripcionAlFinal() {
        given()
                .spec(authenticatedRequest(2, "premium@example.com", "USER"))
                .queryParam("inmediata", false)
                .when()
                .post("/api/suscripciones/cancelar/" + suscripcionIdPremium)
                .then()
                .statusCode(200)
                .body("exito", is(true))
                .body("cancelarAlFinalPeriodo", is(true));
    }

    @Test
    @Order(9)
    @DisplayName("Sin headers de usuario → 401 en estado")
    void debeRechazarEstadoSinAutenticacion() {
        given()
                .spec(unauthenticatedRequest())
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .statusCode(401)
                .body("error", notNullValue());
    }
}
