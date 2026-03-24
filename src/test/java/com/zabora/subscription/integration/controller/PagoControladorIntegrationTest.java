package com.zabora.subscription.integration.controller;

import com.zabora.subscription.integration.BaseIntegrationTest;
import com.zabora.subscription.integration.config.TestDataFactory;
import com.zabora.subscription.modelo.dto.CrearPagoRequest;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import org.junit.jupiter.api.*;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PagoControladorIntegrationTest extends BaseIntegrationTest {

    private static String suscripcionId;

    @BeforeAll
    static void crearSuscripcionPremium() {
        // Este método se ejecutará antes de todos los tests
        // Necesitamos tener acceso al puerto, por lo que se moverá a @BeforeEach
    }

    @BeforeEach
    void setup() {
        // Crear suscripción premium para usar en tests de pago
        if (suscripcionId == null) {
            SolicitudSuscripcionDTO solicitud = TestDataFactory.solicitudPremium();

            suscripcionId = given()
                    .spec(authenticatedRequest(10, "pago-test@example.com", "USER"))
                    .body(solicitud)
            .when()
                    .post("/api/suscripciones/suscribir")
            .then()
                    .statusCode(200)
            .extract()
                    .path("idSuscripcion");
        }
    }

    // ========== TEST 1: CREAR PREFERENCIA DE PAGO ==========

    @Test
    @Order(1)
    @DisplayName("✅ Debe crear preferencia de pago con datos válidos")
    void debeCrearPreferenciaDePago() {
        CrearPagoRequest request = TestDataFactory.pagoRequest(suscripcionId);

        given()
                .spec(authenticatedRequest(10, "pago-test@example.com", "USER"))
                .body(request)
        .when()
                .post("/api/pagos/crear-preferencia")
        .then()
                .statusCode(200)
                .body("preferenceId", notNullValue())
                .body("initPoint", notNullValue())
                .body("publicKey", notNullValue())
                .body("subscriptionId", equalTo(suscripcionId))
                .body("paymentId", notNullValue())
                .body("amount", equalTo(29900.0f));
    }

    // ========== TEST 2: CREAR PREFERENCIA CON SUSCRIPCIÓN INEXISTENTE ==========

    @Test
    @Order(2)
    @DisplayName("❌ Debe rechazar crear preferencia con suscripción inexistente")
    void debeRechazarPreferenciaConSuscripcionInexistente() {
        CrearPagoRequest request = TestDataFactory.pagoRequest("sub_INEXISTENTE");

        given()
                .spec(authenticatedRequest(10, "pago-test@example.com", "USER"))
                .body(request)
        .when()
                .post("/api/pagos/crear-preferencia")
        .then()
                .statusCode(500)
                .body("error", containsStringIgnoringCase("no encontrada"));
    }

    // ========== TEST 3: OBTENER PUBLIC KEY ==========

    @Test
    @Order(3)
    @DisplayName("✅ Debe obtener la llave pública de MercadoPago")
    void debeObtenerPublicKey() {
        given()
                .spec(unauthenticatedRequest())
        .when()
                .get("/api/pagos/public-key")
        .then()
                .statusCode(200)
                .body("publicKey", notNullValue())
                .body("publicKey", not(emptyString()));
    }

    // ========== TEST 4: DOBLE PAGO PENDIENTE NO PERMITIDO ==========

    @Test
    @Order(4)
    @DisplayName("❌ Debe rechazar crear segunda preferencia si ya hay pago pendiente")
    void debeRechazarDoblePreferencia() {
        CrearPagoRequest request = TestDataFactory.pagoRequest(suscripcionId);

        // Primer intento (debe funcionar)
        given()
                .spec(authenticatedRequest(10, "pago-test@example.com", "USER"))
                .body(request)
        .when()
                .post("/api/pagos/crear-preferencia")
        .then()
                .statusCode(200);

        // Segundo intento (debe fallar)
        given()
                .spec(authenticatedRequest(10, "pago-test@example.com", "USER"))
                .body(request)
        .when()
                .post("/api/pagos/crear-preferencia")
        .then()
                .statusCode(400)
                .body("error", containsStringIgnoringCase("pendiente"));
    }
}