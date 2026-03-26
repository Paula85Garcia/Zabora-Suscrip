package com.zabora.subscription;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void getPlanes_DebeRetornar200() {
        given()
                .when()
                .get("/api/suscripciones/planes")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", isA(java.util.List.class));
    }

    @Test
    void getEstado_ConHeadersValidos_DebeRetornar200() {
        given()
                .header("X-User-Id", "1")
                .header("X-User-Email", "test@example.com")
                .header("X-User-Role", "USER")
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("es_premium", isA(Boolean.class))
                .body("estado", isA(String.class));
    }

    @Test
    void getEstado_SinHeaders_DebeRetornar400() {
        given()
                .when()
                .get("/api/suscripciones/estado")
                .then()
                .statusCode(400);
    }

    @Test
    void suscribirse_ConDatosValidos_DebeRetornar200() {
        Map<String, Object> solicitud = new HashMap<>();
        solicitud.put("nombrePlan", "gratuito");
        solicitud.put("tipoPago", "free");

        given()
                .header("X-User-Id", "1")
                .header("X-User-Email", "test@example.com")
                .header("X-User-Role", "USER")
                .contentType(ContentType.JSON)
                .body(solicitud)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("exito", equalTo(true))
                .body("estado", equalTo("ACTIVA"));
    }

    @Test
    void suscribirse_SinHeaders_DebeRetornar400() {
        Map<String, Object> solicitud = new HashMap<>();
        solicitud.put("nombrePlan", "premium");
        solicitud.put("tipoPago", "card");

        given()
                .contentType(ContentType.JSON)
                .body(solicitud)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .statusCode(400);
    }

    @Test
    void suscribirse_DatosInvalidos_DebeRetornar400() {
        Map<String, Object> solicitud = new HashMap<>();
        solicitud.put("nombrePlan", "");
        solicitud.put("tipoPago", "");

        given()
                .header("X-User-Id", "1")
                .header("X-User-Email", "test@example.com")
                .header("X-User-Role", "USER")
                .contentType(ContentType.JSON)
                .body(solicitud)
                .when()
                .post("/api/suscripciones/suscribir")
                .then()
                .statusCode(400);
    }
}
