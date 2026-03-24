package com.zabora.subscription.integration.controller;

import com.zabora.subscription.integration.BaseIntegrationTest;
import com.zabora.subscription.integration.config.TestDataFactory;
import com.zabora.subscription.modelo.dto.CrearPagoRequest;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.repositorio.PagoRepositorio;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebhookControladorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PagoRepositorio pagoRepo;

    private static String suscripcionId;
    private static String pagoId;

    @BeforeEach
    void setup() {
        if (suscripcionId == null) {
            // Crear suscripción y pago para probar webhook
            SolicitudSuscripcionDTO solicitud = TestDataFactory.solicitudPremium();

            suscripcionId = given()
                    .spec(authenticatedRequest(20, "webhook-test@example.com", "USER"))
                    .body(solicitud)
            .when()
                    .post("/api/suscripciones/suscribir")
            .then()
                    .statusCode(200)
            .extract()
                    .path("idSuscripcion");

            CrearPagoRequest pagoRequest = TestDataFactory.pagoRequest(suscripcionId);

            pagoId = given()
                    .spec(authenticatedRequest(20, "webhook-test@example.com", "USER"))
                    .body(pagoRequest)
            .when()
                    .post("/api/pagos/crear-preferencia")
            .then()
                    .statusCode(200)
            .extract()
                    .path("paymentId");
        }
    }

    // ========== TEST 1: WEBHOOK HEALTH CHECK ==========

    @Test
    @Order(1)
    @DisplayName("✅ Debe responder al health check del webhook")
    void debeResponderHealthCheck() {
        given()
                .spec(unauthenticatedRequest())
        .when()
                .get("/api/webhooks/mercadopago")
        .then()
                .statusCode(200)
                .body(equalTo("Webhook activo"));
    }

    // ========== TEST 2: SIMULAR PAGO APROBADO ==========

    @Test
    @Order(2)
    @DisplayName("✅ Debe procesar notificación de pago aprobado")
    void debeProcesarPagoAprobado() {
        Map<String, Object> payload = TestDataFactory.webhookPayloadApproved(
                123456789L,
                suscripcionId,
                20
        );

        given()
                .spec(unauthenticatedRequest())
                .body(payload)
        .when()
                .post("/api/webhooks/mercadopago")
        .then()
                .statusCode(200)
                .body(equalTo("OK"));

        // TODO: Verificar que el pago se marcó como COMPLETADO
        // (Requiere mockear MercadoPago SDK o usar WireMock)
    }

    // ========== TEST 3: SIMULAR NOTIFICACIÓN INVÁLIDA ==========

    @Test
    @Order(3)
    @DisplayName("✅ Debe manejar gracefully notificaciones de tipo no-payment")
    void debeManejarNotificacionInvalida() {
        Map<String, Object> payload = TestDataFactory.webhookPayloadInvalid();

        given()
                .spec(unauthenticatedRequest())
                .body(payload)
        .when()
                .post("/api/webhooks/mercadopago")
        .then()
                .statusCode(200)
                .body(equalTo("OK"));  // No falla, solo ignora
    }
}