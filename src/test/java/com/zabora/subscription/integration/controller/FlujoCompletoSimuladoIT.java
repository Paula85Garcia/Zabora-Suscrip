package com.zabora.subscription.integration.controller;

import com.zabora.subscription.integration.BaseIntegrationTest;
import com.zabora.subscription.integration.config.TestDataFactory;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Flujo simulado end-to-end con Rest Assured: catálogo → gratuito → premium pendiente →
 * public key Bricks → cancelación (auth Feign mockeado en {@link BaseIntegrationTest}).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FlujoCompletoSimuladoIT extends BaseIntegrationTest {

    private static final int U_GRATIS = 501;
    private static final int U_PREMIUM = 502;
    private static String subGratisId;

    @Test
    @Order(1)
    @DisplayName("1. Planes públicos")
    void paso01_planes() {
        given()
                .spec(unauthenticatedRequest())
                .when()
                .get("/api/suscripciones/planes")
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    @Order(2)
    @DisplayName("2. Alta plan gratuito")
    void paso02_suscribirGratis() {
        SolicitudSuscripcionDTO dto = TestDataFactory.solicitudGratuito();
        subGratisId = given()
                .spec(authenticatedRequest(U_GRATIS, "flujo-gratis@test.com", "USER"))
                .body(dto)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .statusCode(200)
                .body("plan", equalTo("gratuito"))
                .body("estado", equalTo("ACTIVA"))
                .extract()
                .path("idSuscripcion");
    }

    @Test
    @Order(3)
    @DisplayName("3. Estado usuario gratuito")
    void paso03_estadoGratis() {
        given()
                .spec(authenticatedRequest(U_GRATIS, "flujo-gratis@test.com", "USER"))
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .statusCode(200)
                .body("plan", equalTo("gratuito"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Upgrade a premium pendiente de pago")
    void paso04_premiumPendiente() {
        SolicitudSuscripcionDTO dto = TestDataFactory.solicitudPremium();
        given()
                .spec(authenticatedRequest(U_PREMIUM, "flujo-premium@test.com", "USER"))
                .body(dto)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .statusCode(200)
                .body("estado", equalTo("PENDIENTE_PAGO"))
                .body("requierePago", is(true))
                .body("idSuscripcion", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("5. Public key Mercado Pago (Bricks)")
    void paso05_publicKeyBricks() {
        given()
                .spec(unauthenticatedRequest())
                .when()
                .get("/api/pagos/bricks/public-key")
                .then()
                .statusCode(200)
                .body("publicKey", not(emptyString()));
    }

    @Test
    @Order(6)
    @DisplayName("6. Historial de pagos (lista, puede estar vacía)")
    void paso06_misPagos() {
        given()
                .spec(authenticatedRequest(U_PREMIUM, "flujo-premium@test.com", "USER"))
                .when()
                .get("/api/pagos/mis-pagos")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(7)
    @DisplayName("7. Cancelación inmediata gratuito (llamada a auth mockeada)")
    void paso07_cancelarGratis() {
        given()
                .spec(authenticatedRequest(U_GRATIS, "flujo-gratis@test.com", "USER"))
                .queryParam("inmediata", true)
                .when()
                .post("/api/suscripciones/cancelar/" + subGratisId)
                .then()
                .statusCode(200)
                .body("estado", equalTo("CANCELADA"));
    }

    @Test
    @Order(8)
    @DisplayName("8. Dashboard admin (headers admin)")
    void paso08_adminDashboard() {
        given()
                .spec(authenticatedRequest(1, "admin@test.com", "ADMIN"))
                .when()
                .get("/api/admin/suscripciones/dashboard")
                .then()
                .statusCode(200)
                .body("$", notNullValue());
    }

    @Test
    @Order(9)
    @DisplayName("9. Webhook GET salud")
    void paso09_webhookPing() {
        given()
                .spec(unauthenticatedRequest())
                .when()
                .get("/api/webhooks/mercadopago")
                .then()
                .statusCode(200)
                .body("status", equalTo("activo"));
    }

    @Test
    @Order(10)
    @DisplayName("10. Webhook POST acepta payload (responde OK sin MP real)")
    void paso10_webhookPost() {
        given()
                .spec(unauthenticatedRequest())
                .body(Map.of(
                        "type", "payment",
                        "data", Map.of("id", "999999999")))
                .when()
                .post("/api/webhooks/mercadopago")
                .then()
                .statusCode(200)
                .body(equalTo("OK"));
    }
}
