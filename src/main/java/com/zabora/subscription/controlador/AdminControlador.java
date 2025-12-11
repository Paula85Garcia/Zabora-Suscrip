package com.zabora.subscription.controlador;

import com.zabora.subscription.servicio.ReporteServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para endpoints de administración.
 * Permite a los administradores generar reportes, estadísticas y consultar suscripciones.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Endpoints para administradores")
@SecurityRequirement(name = "basicAuth") // Requiere autenticación básica para acceder
public class AdminControlador {

    // Servicio encargado de generar reportes y estadísticas
    private final ReporteServicio reporteServicio;

    /**
     * Genera un reporte mensual de ingresos.
     * 
     * @param anio Año del reporte.
     * @param mes Mes del reporte.
     * @return Mapa con información del reporte mensual.
     */
    @GetMapping("/reportes/mensual")
    @Operation(summary = "Generar reporte mensual de ingresos")
    public ResponseEntity<Map<String, Object>> reporteMensual(
            @RequestParam int anio,
            @RequestParam int mes) {

        Map<String, Object> reporte = reporteServicio.generarReporteMensual(anio, mes);
        return ResponseEntity.ok(reporte);
    }

    /**
     * Genera un reporte diario de ingresos de los últimos N días.
     *
     * @param dias Número de días a considerar (por defecto 30).
     * @return Lista de mapas con ingresos diarios.
     */
    @GetMapping("/reportes/diario")
    @Operation(summary = "Reporte de ingresos diarios (últimos N días)")
    public ResponseEntity<List<Map<String, Object>>> reporteDiario(
            @RequestParam(defaultValue = "30") int dias) {

        List<Map<String, Object>> reporte = reporteServicio.reporteIngresosDiarios(dias);
        return ResponseEntity.ok(reporte);
    }

    /**
     * Obtiene estadísticas generales del negocio.
     *
     * @return Mapa con estadísticas como total de usuarios, ingresos, etc.
     */
    @GetMapping("/estadisticas")
    @Operation(summary = "Estadísticas generales del negocio")
    public ResponseEntity<Map<String, Object>> estadisticas() {
        Map<String, Object> stats = reporteServicio.estadisticasGenerales();
        return ResponseEntity.ok(stats);
    }

    /**
     * Lista las suscripciones que están próximas a vencer.
     *
     * @param dias Número de días antes del vencimiento (por defecto 7).
     * @return Lista de suscripciones por vencer.
     */
    @GetMapping("/suscripciones/por-vencer")
    @Operation(summary = "Suscripciones que vencen pronto")
    public ResponseEntity<List<Map<String, Object>>> suscripcionesPorVencer(
            @RequestParam(defaultValue = "7") int dias) {

        List<Map<String, Object>> suscripciones = reporteServicio.suscripcionesPorVencer(dias);
        return ResponseEntity.ok(suscripciones);
    }

    /**
     * Lista los usuarios que han generado mayores ingresos.
     *
     * @param limite Cantidad máxima de usuarios a mostrar (por defecto 10).
     * @return Lista de usuarios y sus ingresos.
     */
    @GetMapping("/usuarios/top-ingresos")
    @Operation(summary = "Top usuarios por ingresos generados")
    public ResponseEntity<List<Map<String, Object>>> topUsuarios(
            @RequestParam(defaultValue = "10") int limite) {

        List<Map<String, Object>> top = reporteServicio.topUsuariosPorIngresos(limite);
        return ResponseEntity.ok(top);
    }
}
