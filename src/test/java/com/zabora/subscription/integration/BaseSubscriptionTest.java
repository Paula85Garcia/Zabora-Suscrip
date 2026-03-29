package com.zabora.subscription.integration;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseSubscriptionTest {

    @LocalServerPort
    protected int port;

    protected static final String BASE_URL = "http://localhost";
    protected static final String USER_EMAIL = "test@zabora.com";
    protected static final String ADMIN_EMAIL = "admin@zabora.com";
    protected static final String USER_ROLE = "USER";
    protected static final String ADMIN_ROLE = "ADMIN";

    @BeforeAll
    static void setupClass() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.port = port;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    // Métodos de configuración de requests
    protected RequestSpecification authenticatedRequest(Integer userId, String email, String role) {
        return new RequestSpecBuilder()
                .addHeader("X-User-Id", userId.toString())
                .addHeader("X-User-Email", email)
                .addHeader("X-User-Role", role)
                .addHeader("Content-Type", "application/json")
                .build();
    }

    protected RequestSpecification adminRequest(Integer userId) {
        return authenticatedRequest(userId, ADMIN_EMAIL, ADMIN_ROLE);
    }

    protected RequestSpecification userRequest(Integer userId) {
        return authenticatedRequest(userId, USER_EMAIL, USER_ROLE);
    }

    protected RequestSpecification unauthenticatedRequest() {
        return new RequestSpecBuilder()
                .addHeader("Content-Type", "application/json")
                .build();
    }

    // Métodos helpers para crear datos de prueba
    protected Map<String, Object> crearSuscripcionRequest(String nombrePlan, String tipoPago, Integer usuarioId) {
        Map<String, Object> request = new HashMap<>();
        request.put("nombrePlan", nombrePlan);
        request.put("tipoPago", tipoPago);
        request.put("usuarioId", usuarioId);
        return request;
    }

    protected Map<String, Object> crearPagoRequest(String suscripcionId, String tipoPago, String token) {
        Map<String, Object> request = new HashMap<>();
        request.put("suscripcionId", suscripcionId);
        request.put("tipoPago", tipoPago);
        request.put("token", token);
        request.put("transaction_amount", 29900.0);
        request.put("description", "Suscripción Premium Zabora");
        return request;
    }

    protected Map<String, Object> crearPreferenciaRequest(String suscripcionId, String tipoPago) {
        Map<String, Object> request = new HashMap<>();
        request.put("suscripcionId", suscripcionId);
        request.put("tipoPago", tipoPago);
        request.put("monto", 29900.0);
        request.put("recibirFactura", false);
        return request;
    }

    protected Map<String, Object> crearCancelacionRequest(boolean inmediata) {
        Map<String, Object> request = new HashMap<>();
        request.put("inmediata", inmediata);
        return request;
    }

    // Métodos helpers para imprimir reportes
    protected void imprimirReporte(String casoPrueba, String[][] resultados) {
        System.out.println("========================================");
        System.out.println("CASO DE PRUEBA: " + casoPrueba);
        System.out.println("========================================");
        System.out.println("Fecha ejecución: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        System.out.println("Responsable: Yuliana Yate");
        System.out.println();
        System.out.println("| Sub-caso | Resultado | Código HTTP | Datos clave |");
        System.out.println("|----------|-----------|-------------|-------------|");

        boolean todoAprobado = true;
        for (String[] resultado : resultados) {
            System.out.println("| " + resultado[0] + " | " + resultado[1] + " | " + resultado[2] + " | " + resultado[3] + " |");
            if (!"APROBO".equals(resultado[1])) {
                todoAprobado = false;
            }
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("VEREDICTO GENERAL: " + (todoAprobado ? "APROBO" : "REPROBO"));
        System.out.println("========================================");
        System.out.println();
    }

    // IDs únicos para evitar conflictos
    protected static final int[] USER_IDS = {2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009};
    protected static final int[] ADMIN_IDS = {990, 991, 992, 993, 994, 995, 996, 997, 998, 999};

    // Tokens de prueba MercadoPago
    protected static final String TOKEN_VISA_EXITOSO = "tok_test_visa";
    protected static final String TOKEN_VISA_RECHAZADO = "tok_test_rejected";
    protected static final String BANCO_PSE_BANCOLOMBIA = "1022";
}
