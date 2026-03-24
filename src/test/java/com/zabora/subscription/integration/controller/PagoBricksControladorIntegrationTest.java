package com.zabora.subscription.integration.controller;

import com.zabora.subscription.integration.BaseIntegrationTest;
import com.zabora.subscription.integration.config.TestDataFactory;
import com.zabora.subscription.modelo.dto.CrearPagoBricksRequest;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import org.junit.jupiter.api.*;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PagoBricksControladorIntegrationTest extends BaseIntegrationTest {

    private static String suscripcionId;

    @BeforeEach
    void setup() {
        // Crear suscripción premium para usar en tests de pago bricks
        if (suscripcionId == null) {
            SolicitudSuscripcionDTO solicitud = TestDataFactory.solicitudPremium();

            suscripcionId = given()
                    .spec(authenticatedRequest(10, "bricks-test@example.com", "USER"))
                    .body(solicitud)
            .when()
                    .post("/api/suscripciones/suscribir")
            .then()
                    .statusCode(200)
            .extract()
                    .path("idSuscripcion");
        }
    }

    // ========== TEST 1: OBTENER PUBLIC KEY PARA BRICKS ==========

    @Test
    @Order(1)
    @DisplayName("✅ Debe obtener la llave pública de MercadoPago para Bricks")
    void debeObtenerPublicKeyParaBricks() {
        given()
                .spec(unauthenticatedRequest())
        .when()
                .get("/api/pagos/bricks/public-key")
        .then()
                .statusCode(200)
                .body("publicKey", notNullValue())
                .body("publicKey", not(emptyString()));
    }

    // ========== TEST 2: CREAR PREFERENCIA PARA BRICKS ==========

    @Test
    @Order(2)
    @DisplayName("✅ Debe crear preferencia de pago para Bricks sin initPoint")
    void debeCrearPreferenciaParaBricks() {
        CrearPagoBricksRequest request = TestDataFactory.pagoBricksRequest(suscripcionId);

        given()
                .spec(authenticatedRequest(10, "bricks-test@example.com", "USER"))
                .body(request)
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(200)
                .body("preferenceId", notNullValue())
                .body("initPoint", nullValue())  // Importante: SIN initPoint para Bricks
                .body("sandboxInitPoint", nullValue())  // Importante: SIN sandboxInitPoint para Bricks
                .body("publicKey", notNullValue())
                .body("subscriptionId", equalTo(suscripcionId))
                .body("paymentId", notNullValue())
                .body("amount", equalTo(29900.0f))
                .body("currency", equalTo("COP"));
    }

    // ========== TEST 3: CREAR PREFERENCIA BRICKS CON SUSCRIPCIÓN INEXISTENTE ==========

    @Test
    @Order(3)
    @DisplayName("❌ Debe rechazar crear preferencia Bricks con suscripción inexistente")
    void debeRechazarPreferenciaBricksConSuscripcionInexistente() {
        CrearPagoBricksRequest request = TestDataFactory.pagoBricksRequest("sub_INEXISTENTE");

        given()
                .spec(authenticatedRequest(10, "bricks-test@example.com", "USER"))
                .body(request)
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(500)
                .body("error", containsStringIgnoringCase("no encontrada"));
    }

    // ========== TEST 4: VERIFICAR PAGO PSE ==========

    @Test
    @Order(4)
    @DisplayName("✅ Debe verificar estado de pago PSE")
    void debeVerificarPagoPSE() {
        // Primero creamos una preferencia para obtener un paymentId
        CrearPagoBricksRequest request = TestDataFactory.pagoBricksRequest(suscripcionId);

        String paymentId = given()
                .spec(authenticatedRequest(10, "bricks-test@example.com", "USER"))
                .body(request)
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(200)
        .extract()
                .path("paymentId");

        // Ahora verificamos el estado del pago
        given()
                .spec(unauthenticatedRequest())
        .queryParam("paymentId", paymentId)
        .when()
                .get("/api/pagos/bricks/pse/verification")
        .then()
                .statusCode(200)
                .body("paymentId", equalTo(paymentId))
                .body("estado", equalTo("PENDIENTE"))
                .body("monto", equalTo(29900.0f));
    }

    // ========== TEST 5: VERIFICAR PAGO PSE INEXISTENTE ==========

    @Test
    @Order(5)
    @DisplayName("❌ Debe rechazar verificación de pago PSE inexistente")
    void debeRechazarVerificacionPagoPSEInexistente() {
        given()
                .spec(unauthenticatedRequest())
                .queryParam("paymentId", "payment_INEXISTENTE")
        .when()
                .get("/api/pagos/bricks/pse/verification")
        .then()
                .statusCode(404);
    }

    // ========== TEST 6: DOBLE PAGO PENDIENTE BRICKS NO PERMITIDO ==========

    @Test
    @Order(6)
    @DisplayName("❌ Debe rechazar crear segunda preferencia Bricks si ya hay pago pendiente")
    void debeRechazarDoblePreferenciaBricks() {
        CrearPagoBricksRequest request = TestDataFactory.pagoBricksRequest(suscripcionId);

        // Primer intento (debe funcionar)
        given()
                .spec(authenticatedRequest(10, "bricks-test@example.com", "USER"))
                .body(request)
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(200)
                .body("initPoint", nullValue());  // Verificar que es para Bricks

        // Segundo intento (debe fallar)
        given()
                .spec(authenticatedRequest(10, "bricks-test@example.com", "USER"))
                .body(request)
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(400)
                .body("error", containsStringIgnoringCase("pendiente"));
    }

    // ========== TEST 7: USUARIO NO AUTENTICADO ==========

    @Test
    @Order(7)
    @DisplayName("❌ Debe rechazar crear preferencia Bricks sin autenticación")
    void debeRechazarPreferenciaBricksSinAutenticacion() {
        CrearPagoBricksRequest request = TestDataFactory.pagoBricksRequest(suscripcionId);

        given()
                .spec(unauthenticatedRequest())
                .body(request)
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(401);
    }

    // ========== TEST 8: VALIDAR DATOS REQUERIDOS ==========

    @Test
    @Order(8)
    @DisplayName("❌ Debe rechazar crear preferencia Bricks con datos incompletos")
    void debeRechazarPreferenciaBricksDatosIncompletos() {
        // Request sin monto
        CrearPagoBricksRequest request = CrearPagoBricksRequest.builder()
                .idSuscripcion(suscripcionId)
                .tipoPago("tarjeta_credito")
                .recibirFactura(false)
                .build(); // Sin monto

        given()
                .spec(authenticatedRequest(10, "bricks-test@example.com", "USER"))
                .body(request)
        .when()
                .post("/api/pagos/bricks/preference")
        .then()
                .statusCode(400);
    }
}
