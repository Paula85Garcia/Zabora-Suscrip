package com.zabora.subscription.controlador;

import com.zabora.subscription.excepcion.AuthServiceException;
import com.zabora.subscription.servicio.AdminReportesServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints administrativos de suscripciones y pagos")
public class AdminController {

    private final AdminReportesServicio adminReportesServicio;

    @GetMapping("/suscripciones/dashboard")
    @Operation(summary = "Dashboard general de suscripciones")
    public ResponseEntity<?> dashboard() {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerDashboardGeneral());
        } catch (Exception e) {
            log.error("Error generando dashboard: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suscripciones/activas")
    @Operation(summary = "Listar suscripciones activas")
    public ResponseEntity<?> suscripcionesActivas() {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerSuscripcionesActivas());
        } catch (Exception e) {
            log.error("Error listando suscripciones activas: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suscripciones/recientes")
    @Operation(summary = "Listar suscripciones recientes")
    public ResponseEntity<?> suscripcionesRecientes(
            @RequestParam(defaultValue = "20") int limite) {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerSuscripcionesRecientes(limite));
        } catch (Exception e) {
            log.error("Error listando suscripciones recientes: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suscripciones/estadisticas")
    @Operation(summary = "Estadisticas de suscripciones")
    public ResponseEntity<?> estadisticasSuscripciones() {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerEstadisticasSuscripciones());
        } catch (Exception e) {
            log.error("Error obteniendo estadisticas: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suscripciones/{id}")
    @Operation(summary = "Detalle de una suscripcion")
    public ResponseEntity<?> detalleSuscripcion(@PathVariable String id) {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerDetallesSuscripcion(id));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if ("Suscripcion no encontrada".equals(msg)) {
                log.warn("Suscripcion no encontrada: {}", id);
                return ResponseEntity.status(404).body(Map.of("error", msg));
            }
            log.error("Error obteniendo detalle de suscripcion {}: {}", id, msg, e);
            return ResponseEntity.internalServerError().body(Map.of("error", msg));
        }
    }

    @PostMapping("/suscripciones/{id}/cancelar")
    @Operation(summary = "Cancelar suscripcion como administrador")
    public ResponseEntity<?> cancelarSuscripcion(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            boolean inmediata = Boolean.TRUE.equals(body.get("inmediata"));
            String motivo = body.getOrDefault("motivo", "Cancelado por administrador").toString();
            adminReportesServicio.cancelarSuscripcionAdmin(id, inmediata, motivo);
            return ResponseEntity.ok(Map.of("mensaje", "Suscripcion cancelada correctamente"));
        } catch (AuthServiceException e) {
            log.error("Cancelación admin bloqueada: auth-service: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                "error",
                "No se pudo degradar al usuario en auth-service. Reintenta o verifica que auth-service y Consul estén activos.",
                "code",
                "AUTH_SYNC_FAILED"));
        } catch (RuntimeException e) {
            log.warn("Error cancelando suscripcion {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error cancelando suscripcion {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pagos/recientes")
    @Operation(summary = "Listar pagos recientes")
    public ResponseEntity<?> pagosRecientes(
            @RequestParam(defaultValue = "20") int limite) {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerPagosRecientes(limite));
        } catch (Exception e) {
            log.error("Error listando pagos recientes: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pagos/ingresos-mensuales")
    @Operation(summary = "Ingresos mensuales agrupados")
    public ResponseEntity<?> ingresosMensuales(
            @RequestParam(defaultValue = "6") int meses) {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerIngresosMensuales(meses));
        } catch (Exception e) {
            log.error("Error calculando ingresos mensuales: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pagos/por-fecha")
    @Operation(summary = "Pagos filtrados por rango de fechas")
    public ResponseEntity<?> pagosPorFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerPagosPorFecha(inicio, fin));
        } catch (Exception e) {
            log.error("Error obteniendo pagos por fecha: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pagos/estadisticas")
    @Operation(summary = "Estadisticas de pagos")
    public ResponseEntity<?> estadisticasPagos() {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerEstadisticasPagos());
        } catch (Exception e) {
            log.error("Error obteniendo estadisticas de pagos: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/pagos/{id}/reembolsar")
    @Operation(summary = "Reembolsar un pago")
    public ResponseEntity<?> reembolsarPago(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String motivo = body.getOrDefault("motivo", "Reembolso solicitado por administrador");
            adminReportesServicio.reembolsarPago(id, motivo);
            return ResponseEntity.ok(Map.of("mensaje", "Reembolso ejecutado correctamente"));
        } catch (IllegalStateException e) {
            log.warn("Estado invalido para reembolso {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error ejecutando reembolso {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado en reembolso {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/usuarios/premium")
    @Operation(summary = "Listar usuarios con plan premium activo")
    public ResponseEntity<?> usuariosPremium() {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerUsuariosPremium());
        } catch (Exception e) {
            log.error("Error listando usuarios premium: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/logs/recientes")
    @Operation(summary = "Logs de suscripciones recientes")
    public ResponseEntity<?> logsRecientes(
            @RequestParam(defaultValue = "50") int limite) {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerLogsRecientes(limite));
        } catch (Exception e) {
            log.error("Error obteniendo logs: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/logs/suscripcion/{id}")
    @Operation(summary = "Logs de una suscripcion especifica")
    public ResponseEntity<?> logsPorSuscripcion(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limite) {
        try {
            return ResponseEntity.ok(adminReportesServicio.obtenerLogsPorSuscripcion(id, limite));
        } catch (Exception e) {
            log.error("Error obteniendo logs de suscripcion {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}