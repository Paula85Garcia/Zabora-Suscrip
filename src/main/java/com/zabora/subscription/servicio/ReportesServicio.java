package com.zabora.subscription.servicio;

import com.zabora.subscription.modelo.dto.ReporteIngresosMensualDTO;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportesServicio {

    private final PagoRepositorio pagoRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;

    /**
     * Generar reporte de ingresos mensuales
     * @param cantidadMeses Cantidad de meses hacia atrás a incluir (default: 12)
     */
    public List<ReporteIngresosMensualDTO> generarReporteIngresosMensuales(Integer cantidadMeses) {
        log.info("Generando reporte de ingresos para los últimos {} meses", cantidadMeses);

        // Obtener todos los pagos completados
        var pagosCompletados = pagoRepositorio.findByEstado(EstadoPago.COMPLETADO);

        // Filtrar por rango de fecha
        LocalDateTime fechaInicio = LocalDateTime.now().minusMonths(cantidadMeses);
        
        var pagosFiltrados = pagosCompletados.stream()
                .filter(p -> p.getFechaPago() != null && p.getFechaPago().isAfter(fechaInicio))
                .collect(Collectors.toList());

        log.info("Total de pagos completados en rango: {}", pagosFiltrados.size());

        // Agrupar por mes
        Map<String, List<com.zabora.subscription.modelo.entidad.Pago>> pagosPorMes = pagosFiltrados.stream()
                .collect(Collectors.groupingBy(pago -> {
                    YearMonth yearMonth = YearMonth.from(pago.getFechaPago());
                    return yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                }));

        // Construir DTOs
        List<ReporteIngresosMensualDTO> reportes = new ArrayList<>();

        for (Map.Entry<String, List<com.zabora.subscription.modelo.entidad.Pago>> entry : pagosPorMes.entrySet()) {
            String mes = entry.getKey();
            List<com.zabora.subscription.modelo.entidad.Pago> pagos = entry.getValue();

            // Total de pagos
            long totalPagos = pagos.size();

            // Ingresos totales
            BigDecimal ingresosTotales = pagos.stream()
                    .map(p -> p.getMonto())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Ingreso promedio
            BigDecimal ingresoPromedio = totalPagos > 0 
                ? ingresosTotales.divide(BigDecimal.valueOf(totalPagos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            // Pagos por método
            Map<String, Long> pagosPorMetodo = pagos.stream()
                    .collect(Collectors.groupingBy(
                        p -> p.getMetodoPago(),
                        Collectors.counting()
                    ));

            // Contar nuevas suscripciones y cancelaciones en el mes
            YearMonth ym = YearMonth.parse(mes, DateTimeFormatter.ofPattern("yyyy-MM"));
            LocalDateTime inicioMes = ym.atDay(1).atStartOfDay();
            LocalDateTime finMes = ym.atEndOfMonth().atTime(23, 59, 59);

            long nuevasSuscripciones = suscripcionRepositorio
                    .findByFechaCreacionBetween(inicioMes, finMes)
                    .stream()
                    .filter(s -> "premium".equalsIgnoreCase(s.getPlan().getNombre()))
                    .count();

            long cancelaciones = suscripcionRepositorio
                    .findByFechaCreacionBetween(inicioMes, finMes)
                    .stream()
                    .filter(s -> s.getFechaCancelacion() != null 
                              && s.getFechaCancelacion().isAfter(inicioMes) 
                              && s.getFechaCancelacion().isBefore(finMes))
                    .count();

            ReporteIngresosMensualDTO reporte = ReporteIngresosMensualDTO.builder()
                    .mes(mes)
                    .totalPagos(totalPagos)
                    .ingresosTotales(ingresosTotales)
                    .ingresoPromedio(ingresoPromedio)
                    .pagosPorMetodo(pagosPorMetodo)
                    .nuevasSuscripciones(nuevasSuscripciones)
                    .cancelaciones(cancelaciones)
                    .build();

            reportes.add(reporte);
        }

        // Ordenar por mes descendente (más reciente primero)
        reportes.sort((r1, r2) -> r2.getMes().compareTo(r1.getMes()));

        log.info("Reporte generado con {} meses de datos", reportes.size());
        return reportes;
    }

    /**
     * Obtener resumen general de suscripciones
     */
    public Map<String, Object> obtenerResumenSuscripciones() {
        Map<String, Object> resumen = new HashMap<>();

        // Suscripciones activas
        long suscripcionesActivas = suscripcionRepositorio
                .findAll()
                .stream()
                .filter(s -> s.getEstado() == EstadoSuscripcion.ACTIVA)
                .count();

        // Suscripciones premium activas
        long premiumActivas = suscripcionRepositorio
                .findAll()
                .stream()
                .filter(s -> s.getEstado() == EstadoSuscripcion.ACTIVA 
                          && "premium".equalsIgnoreCase(s.getPlan().getNombre()))
                .count();

        // Suscripciones gratuitas
        long gratuitasActivas = suscripcionesActivas - premiumActivas;

        // Tasa de conversión (premium / total)
        double tasaConversion = suscripcionesActivas > 0 
            ? (premiumActivas * 100.0) / suscripcionesActivas 
            : 0.0;

        // Próximas a expirar (dentro de 7 días)
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime en7Dias = ahora.plusDays(7);
        
        long proximasExpirar = suscripcionRepositorio
                .findExpiringSubscriptions(ahora, en7Dias)
                .size();

        resumen.put("total_activas", suscripcionesActivas);
        resumen.put("premium_activas", premiumActivas);
        resumen.put("gratuitas_activas", gratuitasActivas);
        resumen.put("tasa_conversion_porcentaje", Math.round(tasaConversion * 100.0) / 100.0);
        resumen.put("proximas_expirar_7dias", proximasExpirar);
        resumen.put("fecha_consulta", ahora);

        return resumen;
    }
}