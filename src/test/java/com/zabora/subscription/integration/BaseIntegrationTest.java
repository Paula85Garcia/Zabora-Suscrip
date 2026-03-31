package com.zabora.subscription.integration;

import com.zabora.subscription.repositorio.AuthClient;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;

/**
 * Base para pruebas de integración con Rest Assured + Testcontainers (MySQL 8).
 * {@link AuthClient} (Feign) se sustituye por un mock: cancelación y premium no llaman a auth-service real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @MockBean
    protected AuthClient authClient;

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_subscriptions")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @BeforeEach
    void setUpRestAssuredAndAuthMock() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        reset(authClient);
        doNothing().when(authClient).actualizarRolPremium(anyInt());
        doNothing().when(authClient).revertirAGratuito(anyInt(), any());
    }

    protected RequestSpecification authenticatedRequest(Integer userId, String email, String role) {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("X-User-Id", String.valueOf(userId))
                .addHeader("X-User-Email", email)
                .addHeader("X-User-Role", role)
                .build();
    }

    protected RequestSpecification unauthenticatedRequest() {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .build();
    }
}