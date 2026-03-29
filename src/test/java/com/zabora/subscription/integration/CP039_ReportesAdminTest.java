package com.zabora.subscription.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CP039_ReportesAdminTest extends BaseSubscriptionTest {

    @Test
    @DisplayName("CP039 - Reportes Administrativos")
    void testReportesAdmin() {
        String[][] resultados = new String[6][4];
        int testIndex = 0;

        // CP039.1 - Admin autenticado obtiene dashboard
        try {
            Integer adminId = ADMIN_IDS[0];
            
            Response response = given()
                .spec(adminRequest(adminId))
                .when()
                .get("/api/admin/dashboard")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            
            // Validar estructura básica del dashboard
            boolean tieneEstructuraValida = response.jsonPath().getMap("$") != null;
            
            if (statusCode == 200 && tieneEstructuraValida) {
                resultados[testIndex++] = new String[]{"CP039.1 - Dashboard admin", "APROBO", "200", "Estructura válida"};
            } else {
                resultados[testIndex++] = new String[]{"CP039.1 - Dashboard admin", "REPROBO", String.valueOf(statusCode), "Estructura inválida"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP039.1 - Dashboard admin", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP039.2 - Admin obtiene suscripciones activas
        try {
            Integer adminId = ADMIN_IDS[1];
            
            Response response = given()
                .spec(adminRequest(adminId))
                .when()
                .get("/api/admin/suscripciones/activas")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            boolean tieneLista = response.jsonPath().getList("$") != null;
            
            if (statusCode == 200 && tieneLista) {
                resultados[testIndex++] = new String[]{"CP039.2 - Suscripciones activas", "APROBO", "200", "Lista obtenida"};
            } else {
                resultados[testIndex++] = new String[]{"CP039.2 - Suscripciones activas", "REPROBO", String.valueOf(statusCode), "No se obtuvo lista"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP039.2 - Suscripciones activas", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP039.3 - Admin obtiene pagos recientes
        try {
            Integer adminId = ADMIN_IDS[2];
            
            Response response = given()
                .spec(adminRequest(adminId))
                .when()
                .get("/api/admin/pagos/recientes")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            boolean tieneLista = response.jsonPath().getList("$") != null;
            
            if (statusCode == 200 && tieneLista) {
                resultados[testIndex++] = new String[]{"CP039.3 - Pagos recientes", "APROBO", "200", "Lista obtenida"};
            } else {
                resultados[testIndex++] = new String[]{"CP039.3 - Pagos recientes", "REPROBO", String.valueOf(statusCode), "No se obtuvo lista"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP039.3 - Pagos recientes", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP039.4 - Admin obtiene ingresos mensuales
        try {
            Integer adminId = ADMIN_IDS[3];
            
            Response response = given()
                .spec(adminRequest(adminId))
                .when()
                .get("/api/admin/reportes/ingresos-mensuales")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            boolean tieneDatos = response.jsonPath().getMap("$") != null;
            
            if (statusCode == 200 && tieneDatos) {
                resultados[testIndex++] = new String[]{"CP039.4 - Ingresos mensuales", "APROBO", "200", "Formato correcto"};
            } else {
                resultados[testIndex++] = new String[]{"CP039.4 - Ingresos mensuales", "REPROBO", String.valueOf(statusCode), "Formato incorrecto"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP039.4 - Ingresos mensuales", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP039.5 - Admin obtiene usuarios premium
        try {
            Integer adminId = ADMIN_IDS[4];
            
            Response response = given()
                .spec(adminRequest(adminId))
                .when()
                .get("/api/admin/usuarios/premium")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            boolean tieneLista = response.jsonPath().getList("$") != null;
            
            if (statusCode == 200 && tieneLista) {
                resultados[testIndex++] = new String[]{"CP039.5 - Usuarios premium", "APROBO", "200", "Lista obtenida"};
            } else {
                resultados[testIndex++] = new String[]{"CP039.5 - Usuarios premium", "REPROBO", String.valueOf(statusCode), "No se obtuvo lista"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP039.5 - Usuarios premium", "REPROBO", "ERROR", e.getMessage()};
        }

        // CP039.6 - Usuario normal accede a endpoint admin
        try {
            Integer userId = USER_IDS[0];
            
            Response response = given()
                .spec(userRequest(userId))
                .when()
                .get("/api/admin/dashboard")
                .then()
                .extract()
                .response();

            int statusCode = response.getStatusCode();
            
            if (statusCode == 403) {
                resultados[testIndex++] = new String[]{"CP039.6 - Usuario normal acceso admin", "APROBO", "403", "Acceso denegado correctamente"};
            } else {
                resultados[testIndex++] = new String[]{"CP039.6 - Usuario normal acceso admin", "REPROBO", String.valueOf(statusCode), "Debería retornar 403"};
            }
        } catch (Exception e) {
            resultados[testIndex++] = new String[]{"CP039.6 - Usuario normal acceso admin", "REPROBO", "ERROR", e.getMessage()};
        }

        imprimirReporte("CP039 - Reportes Administrativos", resultados);
    }

    @Test
    @DisplayName("CP039.1 - Validar estructura dashboard admin")
    void testEstructuraDashboardAdmin() {
        Integer adminId = ADMIN_IDS[5];
        
        given()
            .spec(adminRequest(adminId))
            .when()
            .get("/api/admin/dashboard")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("totalSuscripciones"))
            .body("$", hasKey("suscripcionesActivas"))
            .body("$", hasKey("suscripcionesPremium"))
            .body("$", hasKey("ingresosMensuales"))
            .body("$", hasKey("usuariosTotales"))
            .body("$", hasKey("usuariosPremium"))
            .body("$", hasKey("pagosRecientes"))
            .body("totalSuscripciones", greaterThanOrEqualTo(0))
            .body("suscripcionesActivas", greaterThanOrEqualTo(0))
            .body("suscripcionesPremium", greaterThanOrEqualTo(0))
            .body("ingresosMensuales", greaterThanOrEqualTo(0.0))
            .body("usuariosTotales", greaterThanOrEqualTo(0))
            .body("usuariosPremium", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("CP039.2 - Validar estructura suscripciones activas")
    void testEstructuraSuscripcionesActivas() {
        Integer adminId = ADMIN_IDS[6];
        
        given()
            .spec(adminRequest(adminId))
            .when()
            .get("/api/admin/suscripciones/activas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", isA(List.class))
            .body("$", everyItem(hasKey("id")))
            .body("$", everyItem(hasKey("usuarioId")))
            .body("$", everyItem(hasKey("nombrePlan")))
            .body("$", everyItem(hasKey("estado")))
            .body("$", everyItem(hasKey("fechaCreacion")))
            .body("$", everyItem(hasKey("fechaInicio")))
            .body("$", everyItem(hasKey("fechaExpiracion")));
    }

    @Test
    @DisplayName("CP039.3 - Validar estructura pagos recientes")
    void testEstructuraPagosRecientes() {
        Integer adminId = ADMIN_IDS[7];
        
        given()
            .spec(adminRequest(adminId))
            .when()
            .get("/api/admin/pagos/recientes")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", isA(List.class))
            .body("$", everyItem(hasKey("id")))
            .body("$", everyItem(hasKey("suscripcionId")))
            .body("$", everyItem(hasKey("monto")))
            .body("$", everyItem(hasKey("estado")))
            .body("$", everyItem(hasKey("fechaPago")))
            .body("$", everyItem(hasKey("mpPaymentId")))
            .body("$", everyItem(hasKey("tipoPago")));
    }

    @Test
    @DisplayName("CP039.4 - Validar estructura ingresos mensuales")
    void testEstructuraIngresosMensuales() {
        Integer adminId = ADMIN_IDS[8];
        
        given()
            .spec(adminRequest(adminId))
            .when()
            .get("/api/admin/reportes/ingresos-mensuales")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("mesActual"))
            .body("$", hasKey("mesAnterior"))
            .body("$", hasKey("variacionPorcentual"))
            .body("$", hasKey("ingresosPorMes"))
            .body("mesActual", greaterThanOrEqualTo(0.0))
            .body("mesAnterior", greaterThanOrEqualTo(0.0))
            .body("ingresosPorMes", isA(Map.class));
    }

    @Test
    @DisplayName("CP039.5 - Validar estructura usuarios premium")
    void testEstructuraUsuariosPremium() {
        Integer adminId = ADMIN_IDS[9];
        
        given()
            .spec(adminRequest(adminId))
            .when()
            .get("/api/admin/usuarios/premium")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", isA(List.class))
            .body("$", everyItem(hasKey("id")))
            .body("$", everyItem(hasKey("email")))
            .body("$", everyItem(hasKey("nombrePlan")))
            .body("$", everyItem(hasKey("fechaInicio")))
            .body("$", everyItem(hasKey("fechaExpiracion")))
            .body("$", everyItem(hasKey("diasRestantes")));
    }

    @Test
    @DisplayName("CP039.6 - Validar acceso denegado usuario normal")
    void testAccesoDenegadoUsuarioNormal() {
        Integer userId = USER_IDS[1];
        
        // Probar múltiples endpoints admin
        String[] adminEndpoints = {
            "/api/admin/dashboard",
            "/api/admin/suscripciones/activas",
            "/api/admin/pagos/recientes",
            "/api/admin/reportes/ingresos-mensuales",
            "/api/admin/usuarios/premium"
        };
        
        for (String endpoint : adminEndpoints) {
            given()
                .spec(userRequest(userId))
                .when()
                .get(endpoint)
                .then()
                .statusCode(403);
        }
    }

    @Test
    @DisplayName("CP039.7 - Validar acceso denegado sin autenticación")
    void testAccesoDenegadoSinAutenticacion() {
        // Probar múltiples endpoints admin sin autenticación
        String[] adminEndpoints = {
            "/api/admin/dashboard",
            "/api/admin/suscripciones/activas",
            "/api/admin/pagos/recientes",
            "/api/admin/reportes/ingresos-mensuales",
            "/api/admin/usuarios/premium"
        };
        
        for (String endpoint : adminEndpoints) {
            given()
                .spec(unauthenticatedRequest())
                .when()
                .get(endpoint)
                .then()
                .statusCode(401);
        }
    }
}
