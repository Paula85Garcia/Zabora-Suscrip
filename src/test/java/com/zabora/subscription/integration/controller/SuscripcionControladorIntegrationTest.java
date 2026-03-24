package com.zabora.subscription.integration.controller;

import com.zabora.subscription.integration.BaseIntegrationTest;
import com.zabora.subscription.integration.config.TestDataFactory;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.PlanSuscripcionRepositorio;
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

    @Autowired
    private PlanSuscripcionRepositorio planRepo;

    private static String suscripcionIdGratuita;
    private static String suscripcionIdPremium;

    // ========== TEST 1: CREAR SUSCRIPCIÓN GRATUITA ==========

    @Test
    @Order(1)
    @DisplayName("✅ Debe crear suscripción gratuita y activarla inmediatamente")
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
                .body("mensaje", containsString("successfully"))
                .body("plan", equalTo("gratuito"))
                .body("estado", equalTo("ACTIVA"))
                .body("requierePago", is(false))
                .body("idSuscripcion", notNullValue())
        .extract()
                .path("idSuscripcion");

        System.out.println("✅ Suscripción gratuita creada: " + suscripcionIdGratuita);
    }

    // ========== TEST 2: CREAR SUSCRIPCIÓN PREMIUM ==========

    @Test
    @Order(2)
    @DisplayName("✅ Debe crear suscripción premium y dejarla en PENDIENTE_PAGO")
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
                .body("intentoPago.monto", notNullValue())
        .extract()
                .path("idSuscripcion");

        System.out.println("✅ Suscripción premium creada: " + suscripcionIdPremium);
    }

    // ========== TEST 3: NO PERMITIR DOBLE SUSCRIPCIÓN ACTIVA ==========

    @Test
    @Order(3)
    @DisplayName("❌ Debe rechazar crear segunda suscripción activa para mismo usuario")
    void debeRechazarDobleSuscripcion() {
        SolicitudSuscripcionDTO solicitud = TestDataFactory.solicitudGratuito();

        given()
                .spec(authenticatedRequest(1, "test@example.com", "USER"))
                .body(solicitud)
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(500)  // RuntimeException → 500
                .body("error", containsStringIgnoringCase("already has"));
    }

    // ========== TEST 4: OBTENER ESTADO DEL USUARIO ==========

    @Test
    @Order(4)
    @DisplayName("✅ Debe obtener estado de suscripción del usuario autenticado")
    void debeObtenerEstadoUsuario() {
        given()
                .spec(authenticatedRequest(1, "test@example.com", "USER"))
        .when()
                .get("/api/suscripciones/estado")
        .then()
                .statusCode(200)
                .body("usuario_id", equalTo(1))
                .body("plan", equalTo("gratuito"))
                .body("estado", equalTo("ACTIVA"))
                .body("es_premium", is(false));
    }

    // ========== TEST 5: VERIFICAR SUSCRIPCIÓN DE OTRO USUARIO ==========

    @Test
    @Order(5)
    @DisplayName("✅ Debe verificar suscripción de usuario específico")
    void debeVerificarSuscripcionUsuario() {
        given()
                .spec(authenticatedRequest(999, "admin@example.com", "ADMIN"))
        .when()
                .get("/api/suscripciones/verificar/1")
        .then()
                .statusCode(200)
                .body("valida", is(false))  // gratuito no es premium
                .body("plan", equalTo("gratuito"))
                .body("estado", equalTo("ACTIVA"));
    }

    // ========== TEST 6: OBTENER PLANES DISPONIBLES ==========

    @Test
    @Order(6)
    @DisplayName("✅ Debe listar todos los planes disponibles")
    void debeListarPlanes() {
        given()
                .spec(unauthenticatedRequest())  // Endpoint público
        .when()
                .get("/api/suscripciones/planes")
        .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(2)))
                .body("[0].nombre", notNullValue())
                .body("[0].precio", notNullValue());
    }

    // ========== TEST 7: CANCELAR SUSCRIPCIÓN INMEDIATA ==========

    @Test
    @Order(7)
    @DisplayName("✅ Debe cancelar suscripción inmediatamente")
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
                .body("mensaje", containsString("immediately"));

        // Verificar en BD
        var suscripcion = suscripcionRepo.findById(suscripcionIdGratuita).orElseThrow();
        Assertions.assertEquals(EstadoSuscripcion.CANCELADA, suscripcion.getEstado());
    }

    // ========== TEST 8: CANCELAR AL FINAL DEL PERÍODO ==========

    @Test
    @Order(8)
    @DisplayName("✅ Debe cancelar suscripción al final del período")
    void debeCancelarSuscripcionAlFinal() {
        // Primero crear y activar una suscripción premium para este test
        // (en producción esto requeriría un pago, aquí lo simulamos)
        
        given()
                .spec(authenticatedRequest(2, "premium@example.com", "USER"))
                .queryParam("inmediata", false)
        .when()
                .post("/api/suscripciones/cancelar/" + suscripcionIdPremium)
        .then()
                .statusCode(200)
                .body("exito", is(true))
                .body("cancelarAlFinalPeriodo", is(true))
                .body("horasHastaCancelacion", notNullValue());
    }

    // ========== TEST 9: SIN AUTENTICACIÓN → ERROR ==========

    @Test
    @Order(9)
    @DisplayName("❌ Debe rechazar petición sin headers de autenticación")
    void debeRechazarSinAutenticacion() {
        SolicitudSuscripcionDTO solicitud = TestDataFactory.solicitudGratuito();

        given()
                .spec(unauthenticatedRequest())
                .body(solicitud)
        .when()
                .post("/api/suscripciones/suscribir")
        .then()
                .statusCode(anyOf(is(401), is(500)));  // Depende de la implementación
    }
}