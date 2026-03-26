package com.zabora.subscription.integration.controller;

import com.zabora.subscription.integration.BaseIntegrationTest;
import com.zabora.subscription.integration.config.TestDataFactory;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import org.junit.jupiter.api.*;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

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
        // Test simplificado ya que eliminamos Checkout Pro
        given()
                .spec(authenticatedRequest(10, "pago-test@example.com", "USER"))
        .when()
                .get("/api/pagos/bricks/public-key")
        .then()
                .statusCode(200)
                .body("publicKey", notNullValue());
    }

    // ========== TEST 2: OBTENER PUBLIC KEY ==========

    @Test
    @Order(2)
    @DisplayName("✅ Debe obtener la llave pública de MercadoPago")
    void debeObtenerPublicKey() {
        given()
                .spec(unauthenticatedRequest())
        .when()
                .get("/api/pagos/bricks/public-key")
        .then()
                .statusCode(200)
                .body("publicKey", notNullValue())
                .body("publicKey", not(emptyString()));
    }

    // ========== TEST 3: PROCESAR PAGO ==========

    @Test
    @Order(3)
    @DisplayName("✅ Debe procesar pago con datos válidos")
    void debeProcesarPago() {
        Map<String, Object> pagoRequest = Map.of(
            "token", "test_token_123",
            "payment_method_id", "visa",
            "transaction_amount", 29900,
            "description", "Test payment"
        );

        given()
                .spec(authenticatedRequest(10, "pago-test@example.com", "USER"))
                .body(pagoRequest)
        .when()
                .post("/api/suscripciones/procesar-pago")
        .then()
                .statusCode(200)
                .body("status", equalTo("approved"))
                .body("id", notNullValue());
    }
}