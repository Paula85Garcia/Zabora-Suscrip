package com.zabora.subscription.controlador;

import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.servicio.AdminReportesServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Reportes", description = "Endpoints administrativos para reportes y estadisticas")
public class AdminReportesController {

    private final AdminReportesServicio adminReportesServicio;

    //Validacion de rol 
    private void validarRolAdmin() {
        try {
            var userData = UserContext.get();
            if (userData == null) {
                throw new SecurityException("Acceso denegado: no hay contexto de usuario");
            }
            String role = userData.getRole();
            if (role == null || (!role.equals("ADMIN") && !role.equals("ROLE_ADMIN"))) {
                log.warn("Acceso denegado: usuario con rol '{}' intento acceder a endpoint de admin", role);
                throw new SecurityException("Acceso denegado: se requiere rol ADMIN");
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validando rol de administrador", e);
            throw new SecurityException("Acceso denegado: " + e.getMessage());
        }
    }

    // Dashboard 

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard general")
    public ResponseEntity<Map<String, Object>> obtenerDashboard() {
        try {
            validarRolAdmin();
            log.info("Admin solicito dashboard general");
            return ResponseEntity.ok(adminReportesServicio.obtenerDashboardGeneral());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo dashboard", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    //Suscripciones 

    @GetMapping("/suscripciones/activas")
    @Operation(summary = "Suscripciones activas")
    public ResponseEntity<?> obtenerSuscripcionesActivas() {
        try {
            validarRolAdmin();
            log.info("Admin solicito suscripciones activas");
            List<Map<String, Object>> resultado = adminReportesServicio.obtenerSuscripcionesActivas();
            log.info("Encontradas {} suscripciones activas", resultado.size());
            return ResponseEntity.ok(resultado);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error obteniendo suscripciones activas", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suscripciones/recientes")
    @Operation(summary = "Suscripciones recientes")
    public ResponseEntity<?> obtenerSuscripcionesRecientes(
            @RequestParam(defaultValue = "10") int limite) {
        try {
            validarRolAdmin();
            log.info("Admin solicito ultimas {} suscripciones", limite);
            List<Map<String, Object>> resultado = adminReportesServicio.obtenerSuscripcionesRecientes(limite);
            return ResponseEntity.ok(resultado);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error obteniendo suscripciones recientes", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suscripciones/estadisticas")
    @Operation(summary = "Estadisticas de suscripciones")
    public ResponseEntity<?> obtenerEstadisticasSuscripciones() {
        try {
            validarRolAdmin();
            log.info("Admin solicito estadisticas de suscripciones");
            return ResponseEntity.ok(adminReportesServicio.obtenerEstadisticasSuscripciones());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error obteniendo estadisticas", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suscripciones/{id}/detalles")
    @Operation(summary = "Detalles de una suscripcion")
    public ResponseEntity<?> obtenerDetallesSuscripcion(@PathVariable String id) {
        try {
            validarRolAdmin();
            log.info("Admin solicito detalles de suscripcion: {}", id);
            return ResponseEntity.ok(adminReportesServicio.obtenerDetallesSuscripcion(id));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error obteniendo detalles de suscripcion {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/suscripciones/{id}/cancelar")
    @Operation(summary = "Cancelar suscripcion (admin)")
    public ResponseEntity<Map<String, Object>> cancelarSuscripcionAdmin(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean inmediata,
            @RequestParam(required = false) String motivo) {
        try {
            validarRolAdmin();
            log.info("Admin cancelando suscripcion: {}, inmediata: {}", id, inmediata);
            adminReportesServicio.cancelarSuscripcionAdmin(id, inmediata, motivo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Suscripcion cancelada por administrador");
            response.put("suscripcionId", id);
            response.put("inmediata", inmediata);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado", "success", false));
        } catch (Exception e) {
            log.error("Error cancelando suscripcion {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    //  Pagos 

    @GetMapping("/pagos/recientes")
    @Operation(summary = "Pagos recientes")
    public ResponseEntity<?> obtenerPagosRecientes(
            @RequestParam(defaultValue = "20") int limite) {
        try {
            validarRolAdmin();
            log.info("Admin solicito ultimos {} pagos", limite);
            List<Map<String, Object>> resultado = adminReportesServicio.obtenerPagosRecientes(limite);
            return ResponseEntity.ok(resultado);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error obteniendo pagos recientes", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pagos/estadisticas")
    @Operation(summary = "Estadisticas de pagos")
    public ResponseEntity<?> obtenerEstadisticasPagos() {
        try {
            validarRolAdmin();
            log.info("Admin solicito estadisticas de pagos");
            return ResponseEntity.ok(adminReportesServicio.obtenerEstadisticasPagos());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error obteniendo estadisticas de pagos", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/pagos/{id}/reembolsar")
    @Operation(summary = "Reembolsar pago")
    public ResponseEntity<Map<String, Object>> reembolsarPago(
            @PathVariable String id,
            @RequestParam(required = false) String motivo) {
        try {
            validarRolAdmin();
            log.info("Admin reembolsando pago: {}", id);
            adminReportesServicio.reembolsarPago(id, motivo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Pago reembolsado exitosamente");
            response.put("pagoId", id);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado", "success", false));
        } catch (Exception e) {
            log.error("Error reembolsando pago {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Reportes ──────────────────────────────────────────────────────────────────

    @GetMapping("/reportes/ingresos-mensuales")
    @Operation(summary = "Ingresos mensuales")
    public ResponseEntity<?> obtenerIngresosMensuales(
            @RequestParam(required = false) Integer meses) {
        try {
            validarRolAdmin();
            int m = meses != null ? meses : 12;
            log.info("Admin solicito ingresos de ultimos {} meses", m);
            return ResponseEntity.ok(adminReportesServicio.obtenerIngresosMensuales(m));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error obteniendo ingresos mensuales", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/reportes/pagos-por-fecha")
    @Operation(summary = "Pagos por rango de fechas")
    public ResponseEntity<?> obtenerPagosPorFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            validarRolAdmin();
            log.info("Admin solicito pagos entre {} y {}", fechaInicio, fechaFin);
            return ResponseEntity.ok(adminReportesServicio.obtenerPagosPorFecha(fechaInicio, fechaFin));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error obteniendo pagos por fecha", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/reportes/conversion")
    @Operation(summary = "Tasa de conversion free a premium")
    public ResponseEntity<?> obtenerTasaConversion() {
        try {
            validarRolAdmin();
            log.info("Admin solicito tasa de conversion");
            return ResponseEntity.ok(adminReportesServicio.obtenerTasaConversion());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error calculando tasa de conversion", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Usuarios ──────────────────────────────────────────────────────────────────

    @GetMapping("/usuarios/premium")
    @Operation(summary = "Usuarios premium activos")
    public ResponseEntity<?> obtenerUsuariosPremium() {
        try {
            validarRolAdmin();
            log.info("Admin solicito usuarios premium");
            List<Map<String, Object>> resultado = adminReportesServicio.obtenerUsuariosPremium();
            log.info("Encontrados {} usuarios premium", resultado.size());
            return ResponseEntity.ok(resultado);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error obteniendo usuarios premium", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Logs ──────────────────────────────────────────────────────────────────────

    @GetMapping("/logs/suscripciones")
    @Operation(summary = "Logs de suscripciones")
    public ResponseEntity<?> obtenerLogsSuscripciones(
            @RequestParam(required = false) String suscripcionId,
            @RequestParam(defaultValue = "50") int limite) {
        try {
            validarRolAdmin();
            log.info("Admin solicito logs de suscripciones");
            List<Map<String, Object>> logs = suscripcionId != null
                ? adminReportesServicio.obtenerLogsPorSuscripcion(suscripcionId, limite)
                : adminReportesServicio.obtenerLogsRecientes(limite);
            log.info("Retornando {} logs", logs.size());
            return ResponseEntity.ok(logs);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
        } catch (Exception e) {
            log.error("Error obteniendo logs", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
