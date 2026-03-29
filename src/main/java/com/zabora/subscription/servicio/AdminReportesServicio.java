package com.zabora.subscription.servicio;

import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio de administración para reportes y estadísticas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportesServicio {

    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;
    private final PagoRepositorio pagoRepositorio;
    private final JdbcTemplate jdbcTemplate;

    // ═══════════════════════════════════════
    // DASHBOARD GENERAL
    // ═══════════════════════════════════════

    public Map<String, Object> obtenerDashboardGeneral() {
        log.info("Generando dashboard general");

        Map<String, Object> dashboard = new HashMap<>();

        try {
            // Total de suscripciones por estado
            long totalActivas = suscripcionRepositorio.countByEstado(EstadoSuscripcion.ACTIVA);
            long totalPendientes = suscripcionRepositorio.countByEstado(EstadoSuscripcion.PENDIENTE_PAGO);
            long totalCanceladas = suscripcionRepositorio.countByEstado(EstadoSuscripcion.CANCELADA);
            long totalExpiradas = suscripcionRepositorio.countByEstado(EstadoSuscripcion.EXPIRADA);

            dashboard.put("suscripciones", Map.of(
                "activas", totalActivas,
                "pendientes", totalPendientes,
                "canceladas", totalCanceladas,
                "expiradas", totalExpiradas,
                "total", totalActivas + totalPendientes + totalCanceladas + totalExpiradas
            ));

            // Total de pagos por estado
            long pagosCompletados = pagoRepositorio.countByEstado(EstadoPago.COMPLETADO);
            long pagosPendientes = pagoRepositorio.countByEstado(EstadoPago.PENDIENTE);
            long pagosFallidos = pagoRepositorio.countByEstado(EstadoPago.FALLIDO);

            dashboard.put("pagos", Map.of(
                "completados", pagosCompletados,
                "pendientes", pagosPendientes,
                "fallidos", pagosFallidos,
                "total", pagosCompletados + pagosPendientes + pagosFallidos
            ));

            // Ingresos totales del mes actual
            BigDecimal ingresosMesActual = calcularIngresosMesActual();
            dashboard.put("ingresosMesActual", ingresosMesActual);

            // Ingresos totales
            BigDecimal ingresosTotales = calcularIngresosTotales();
            dashboard.put("ingresosTotales", ingresosTotales);

            // Tasa de conversión
            Map<String, Object> conversion = obtenerTasaConversion();
            dashboard.put("conversion", conversion);

            log.info("Dashboard generado exitosamente");

        } catch (Exception e) {
            log.error("Error generando dashboard", e);
            throw new RuntimeException("Error generando dashboard: " + e.getMessage());
        }

        return dashboard;
    }

    // ═══════════════════════════════════════
    // SUSCRIPCIONES
    // ═══════════════════════════════════════

    public List<Map<String, Object>> obtenerSuscripcionesActivas() {
        String sql = """
            SELECT 
                su.id AS suscripcion_id,
                su.usuario_id,
                ps.nombre AS plan_nombre,
                ps.precio AS plan_precio,
                su.estado,
                su.inicio_periodo_actual,
                su.fin_periodo_actual,
                DATEDIFF(su.fin_periodo_actual, CURDATE()) AS dias_restantes
            FROM suscripciones_usuarios su
            JOIN planes_suscripcion ps ON su.plan_id = ps.id
            WHERE su.estado = 'ACTIVA'
            ORDER BY su.fecha_creacion DESC
        """;

        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> obtenerSuscripcionesRecientes(int limite) {
        String sql = """
            SELECT 
                su.id AS suscripcion_id,
                su.usuario_id,
                ps.nombre AS plan_nombre,
                ps.precio AS plan_precio,
                su.estado,
                su.fecha_creacion,
                su.inicio_periodo_actual,
                su.fin_periodo_actual
            FROM suscripciones_usuarios su
            JOIN planes_suscripcion ps ON su.plan_id = ps.id
            ORDER BY su.fecha_creacion DESC
            LIMIT ?
        """;

        return jdbcTemplate.queryForList(sql, limite);
    }

    public Map<String, Object> obtenerEstadisticasSuscripciones() {
        Map<String, Object> stats = new HashMap<>();

        // Por estado
        stats.put("porEstado", Map.of(
            "ACTIVA", suscripcionRepositorio.countByEstado(EstadoSuscripcion.ACTIVA),
            "PENDIENTE_PAGO", suscripcionRepositorio.countByEstado(EstadoSuscripcion.PENDIENTE_PAGO),
            "CANCELADA", suscripcionRepositorio.countByEstado(EstadoSuscripcion.CANCELADA),
            "EXPIRADA", suscripcionRepositorio.countByEstado(EstadoSuscripcion.EXPIRADA)
        ));

        // Por plan
        String sqlPorPlan = """
            SELECT 
                ps.nombre AS plan,
                COUNT(su.id) AS total
            FROM suscripciones_usuarios su
            JOIN planes_suscripcion ps ON su.plan_id = ps.id
            WHERE su.estado = 'ACTIVA'
            GROUP BY ps.nombre
        """;

        List<Map<String, Object>> porPlan = jdbcTemplate.queryForList(sqlPorPlan);
        stats.put("porPlan", porPlan);

        return stats;
    }

    // ═══════════════════════════════════════
    // PAGOS
    // ═══════════════════════════════════════

    public List<Map<String, Object>> obtenerPagosRecientes(int limite) {
        String sql = """
            SELECT 
                p.id AS pago_id,
                p.usuario_id,
                p.suscripcion_id,
                p.monto,
                p.moneda,
                p.metodo_pago,
                p.estado,
                p.fecha_pago,
                p.fecha_creacion,
                ps.nombre AS plan_nombre
            FROM pagos p
            JOIN suscripciones_usuarios su ON p.suscripcion_id = su.id
            JOIN planes_suscripcion ps ON su.plan_id = ps.id
            ORDER BY p.fecha_creacion DESC
            LIMIT ?
        """;

        return jdbcTemplate.queryForList(sql, limite);
    }

    public List<Map<String, Object>> obtenerIngresosMensuales(int meses) {
        String sql = """
            SELECT 
                DATE_FORMAT(fecha_pago, '%Y-%m') AS mes,
                COUNT(id) AS total_pagos,
                SUM(monto) AS ingresos_totales
            FROM pagos
            WHERE estado = 'COMPLETADO'
            AND fecha_pago >= DATE_SUB(CURDATE(), INTERVAL ? MONTH)
            GROUP BY DATE_FORMAT(fecha_pago, '%Y-%m')
            ORDER BY mes DESC
        """;

        return jdbcTemplate.queryForList(sql, meses);
    }

    public Map<String, Object> obtenerEstadisticasPagos() {
        Map<String, Object> stats = new HashMap<>();

        // Por estado
        stats.put("porEstado", Map.of(
            "COMPLETADO", pagoRepositorio.countByEstado(EstadoPago.COMPLETADO),
            "PENDIENTE", pagoRepositorio.countByEstado(EstadoPago.PENDIENTE),
            "FALLIDO", pagoRepositorio.countByEstado(EstadoPago.FALLIDO),
            "REEMBOLSADO", pagoRepositorio.countByEstado(EstadoPago.REEMBOLSADO)
        ));

        // Por método de pago
        String sqlPorMetodo = """
            SELECT 
                metodo_pago,
                COUNT(*) AS total,
                SUM(monto) AS monto_total
            FROM pagos
            WHERE estado = 'COMPLETADO'
            GROUP BY metodo_pago
        """;

        List<Map<String, Object>> porMetodo = jdbcTemplate.queryForList(sqlPorMetodo);
        stats.put("porMetodoPago", porMetodo);

        // Tasa de éxito
        long totalIntentos = pagoRepositorio.count();
        long totalExitosos = pagoRepositorio.countByEstado(EstadoPago.COMPLETADO);
        double tasaExito = totalIntentos > 0 ? (totalExitosos * 100.0 / totalIntentos) : 0;

        stats.put("tasaExito", tasaExito);

        return stats;
    }

    public List<Map<String, Object>> obtenerPagosPorFecha(LocalDate fechaInicio, LocalDate fechaFin) {
        String sql = """
            SELECT 
                p.id AS pago_id,
                p.usuario_id,
                p.monto,
                p.moneda,
                p.metodo_pago,
                p.estado,
                p.fecha_pago,
                ps.nombre AS plan_nombre
            FROM pagos p
            JOIN suscripciones_usuarios su ON p.suscripcion_id = su.id
            JOIN planes_suscripcion ps ON su.plan_id = ps.id
            WHERE DATE(p.fecha_pago) BETWEEN ? AND ?
            ORDER BY p.fecha_pago DESC
        """;

        return jdbcTemplate.queryForList(sql, fechaInicio, fechaFin);
    }

    // ═══════════════════════════════════════
    // USUARIOS
    // ═══════════════════════════════════════

    public List<Map<String, Object>> obtenerUsuariosPremium() {
        String sql = """
            SELECT 
                su.usuario_id,
                ps.nombre AS plan,
                su.inicio_periodo_actual,
                su.fin_periodo_actual,
                DATEDIFF(su.fin_periodo_actual, CURDATE()) AS dias_restantes
            FROM suscripciones_usuarios su
            JOIN planes_suscripcion ps ON su.plan_id = ps.id
            WHERE su.estado = 'ACTIVA'
            AND ps.nombre = 'premium'
            ORDER BY su.fecha_creacion DESC
        """;

        return jdbcTemplate.queryForList(sql);
    }

    public Map<String, Object> obtenerTasaConversion() {
        String sql = """
            SELECT 
                COUNT(DISTINCT CASE WHEN ps.nombre = 'gratuito' THEN su.usuario_id END) AS usuarios_free,
                COUNT(DISTINCT CASE WHEN ps.nombre = 'premium' THEN su.usuario_id END) AS usuarios_premium,
                COUNT(DISTINCT su.usuario_id) AS usuarios_totales
            FROM suscripciones_usuarios su
            JOIN planes_suscripcion ps ON su.plan_id = ps.id
        """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql);

        long usuariosFree = ((Number) result.get("usuarios_free")).longValue();
        long usuariosPremium = ((Number) result.get("usuarios_premium")).longValue();
        long usuariosTotales = ((Number) result.get("usuarios_totales")).longValue();

        double tasaConversion = usuariosTotales > 0 
            ? (usuariosPremium * 100.0 / usuariosTotales) 
            : 0;

        Map<String, Object> conversion = new HashMap<>();
        conversion.put("usuariosFree", usuariosFree);
        conversion.put("usuariosPremium", usuariosPremium);
        conversion.put("usuariosTotales", usuariosTotales);
        conversion.put("tasaConversion", tasaConversion);

        return conversion;
    }

    // ═══════════════════════════════════════
    // LOGS
    // ═══════════════════════════════════════

    public List<Map<String, Object>> obtenerLogsRecientes(int limite) {
        String sql = """
            SELECT 
                ls.id,
                ls.suscripcion_id,
                ls.usuario_id,
                ls.accion,
                ls.estado_anterior,
                ls.estado_nuevo,
                ls.descripcion,
                ls.fecha_creacion
            FROM logs_suscripciones ls
            ORDER BY ls.fecha_creacion DESC
            LIMIT ?
        """;

        return jdbcTemplate.queryForList(sql, limite);
    }

    public List<Map<String, Object>> obtenerLogsPorSuscripcion(String suscripcionId, int limite) {
        String sql = """
            SELECT 
                ls.id,
                ls.accion,
                ls.estado_anterior,
                ls.estado_nuevo,
                ls.descripcion,
                ls.realizado_por,
                ls.fecha_creacion
            FROM logs_suscripciones ls
            WHERE ls.suscripcion_id = ?
            ORDER BY ls.fecha_creacion DESC
            LIMIT ?
        """;

        return jdbcTemplate.queryForList(sql, suscripcionId, limite);
    }

    public Map<String, Object> obtenerDetallesSuscripcion(String id) {
        UsuarioSuscripcion suscripcion = suscripcionRepositorio.findById(id)
            .orElseThrow(() -> new RuntimeException("Suscripción no encontrada"));

        Map<String, Object> detalles = new HashMap<>();
        detalles.put("suscripcion", suscripcion);

        // Pagos asociados
        List<Pago> pagos = pagoRepositorio.findBySuscripcionIdOrderByFechaCreacionDesc(id);
        detalles.put("pagos", pagos);

        // Logs
        detalles.put("logs", obtenerLogsPorSuscripcion(id, 20));

        return detalles;
    }

    // ═══════════════════════════════════════
    // ACCIONES ADMINISTRATIVAS
    // ═══════════════════════════════════════

    @Transactional
    public void cancelarSuscripcionAdmin(String id, boolean inmediata, String motivo) {
        UsuarioSuscripcion suscripcion = suscripcionRepositorio.findById(id)
            .orElseThrow(() -> new RuntimeException("Suscripción no encontrada"));

        if (inmediata) {
            suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
        } else {
            suscripcion.setCancelarAlFinalPeriodo(true);
        }

        suscripcion.setFechaCancelacion(LocalDateTime.now());

        suscripcionRepositorio.save(suscripcion);

        log.info("Suscripción {} cancelada por admin. Inmediata: {}, Motivo: {}", 
                 id, inmediata, motivo);
    }

    @Transactional
    public void reembolsarPago(String id, String motivo) {
        Pago pago = pagoRepositorio.findById(id)
            .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        pago.setEstado(EstadoPago.REEMBOLSADO);
        pagoRepositorio.save(pago);

        log.info("Pago {} marcado como REEMBOLSADO. Motivo: {}", id, motivo);
    }

    // ═══════════════════════════════════════
    // HELPERS PRIVADOS
    // ═══════════════════════════════════════

    private BigDecimal calcularIngresosMesActual() {
        String sql = """
            SELECT COALESCE(SUM(monto), 0) AS total
            FROM pagos
            WHERE estado = 'COMPLETADO'
            AND MONTH(fecha_pago) = MONTH(CURDATE())
            AND YEAR(fecha_pago) = YEAR(CURDATE())
        """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql);
        return (BigDecimal) result.get("total");
    }

    private BigDecimal calcularIngresosTotales() {
        String sql = """
            SELECT COALESCE(SUM(monto), 0) AS total
            FROM pagos
            WHERE estado = 'COMPLETADO'
        """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql);
        return (BigDecimal) result.get("total");
    }
}