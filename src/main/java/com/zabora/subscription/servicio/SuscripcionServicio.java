package com.zabora.subscription.servicio;

import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.data.UserData;
import com.zabora.subscription.modelo.dto.RespuestaSuscripcionDTO;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import com.zabora.subscription.modelo.entidad.PlanSuscripcion;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.PlanSuscripcionRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * FIX BUG-3:  Eliminada declaracion manual de Logger (duplicada con @Slf4j).
 * FIX BUG-9:  crearSuscripcion ahora tambien verifica PENDIENTE_PAGO para evitar duplicados.
 * FIX CAL-3:  cancelarSuscripcion ahora llama a authServicio.revertirAGratuito()
 *             cuando la cancelacion es inmediata.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuscripcionServicio {

    private final PlanSuscripcionRepositorio planRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;
    private final PagoRepositorio pagoRepositorio;
    private final AuthServicio authServicio;  // FIX CAL-3: inyectado para revertir rol
    private final ServicioNotificaciones servicioNotificaciones;

    @Transactional
    public RespuestaSuscripcionDTO crearSuscripcion(SolicitudSuscripcionDTO solicitud) {

        UserData usuario = UserContext.get();
        Integer usuarioId = (usuario != null) ? usuario.getUserId() : solicitud.getUsuarioId();

        if (usuarioId == null) {
            throw new IllegalStateException("No se pudo determinar el usuario. Verifica que el gateway envie X-User-Id.");
        }

        log.info("Creando suscripcion - Usuario: {}, Plan: {}", usuarioId, solicitud.getNombrePlan());

        PlanSuscripcion plan = planRepositorio.findByNombreIgnoreCase(solicitud.getNombrePlan())
            .orElseThrow(() -> new IllegalStateException(
                "Plan no encontrado: " + solicitud.getNombrePlan() + ". Disponibles: gratuito, premium"));

        Optional<UsuarioSuscripcion> activaOpt = suscripcionRepositorio
            .findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA);
        Optional<UsuarioSuscripcion> pendienteOpt = suscripcionRepositorio
            .findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.PENDIENTE_PAGO);

        boolean esPlanGratuito = "gratuito".equalsIgnoreCase(plan.getNombre());

        if (esPlanGratuito) {
            if (activaOpt.isPresent()) {
                log.info("Idempotente suscripcion gratuita — usuario {} ya tiene ACTIVA", usuarioId);
                return construirRespuestaEstado(activaOpt.get());
            }
            if (pendienteOpt.isPresent()) {
                UsuarioSuscripcion pend = pendienteOpt.get();
                pend.setEstado(EstadoSuscripcion.CANCELADA);
                pend.setFechaCancelacion(LocalDateTime.now());
                pend.setCancelarAlFinalPeriodo(false);
                suscripcionRepositorio.save(pend);
                log.info("Suscripcion pendiente {} cancelada al elegir plan gratuito — usuario {}", pend.getId(), usuarioId);
            }
        } else {
            if (activaOpt.isPresent()) {
                UsuarioSuscripcion s = activaOpt.get();
                String planActivo = s.getPlan().getNombre();
                if ("gratuito".equalsIgnoreCase(planActivo)) {
                    s.setPlan(plan);
                    s.setEstado(EstadoSuscripcion.PENDIENTE_PAGO);
                    s.setInicioPeriodoActual(null);
                    s.setFinPeriodoActual(null);
                    s.setCancelarAlFinalPeriodo(false);
                    suscripcionRepositorio.save(s);
                    log.info("Upgrade gratuito -> premium PENDIENTE_PAGO — suscripcion {}", s.getId());
                    return RespuestaSuscripcionDTO.builder()
                        .exito(true)
                        .mensaje("Suscripcion creada. Pendiente de pago.")
                        .idSuscripcion(s.getId())
                        .plan(plan.getNombre())
                        .estado(s.getEstado().name())
                        .precioPlan(plan.getPrecio())
                        .requierePago(true)
                        .build();
                }
                if ("premium".equalsIgnoreCase(planActivo)) {
                    log.info("Idempotente premium — usuario {} ya tiene premium ACTIVA", usuarioId);
                    return construirRespuestaEstado(s);
                }
                throw new IllegalStateException("El usuario ya tiene una suscripcion activa");
            }
            if (pendienteOpt.isPresent()) {
                UsuarioSuscripcion s = pendienteOpt.get();
                if ("premium".equalsIgnoreCase(plan.getNombre())) {
                    log.info("Idempotente premium pendiente — suscripcion {}", s.getId());
                    return construirRespuestaEstado(s);
                }
                throw new IllegalStateException("El usuario ya tiene una suscripcion pendiente de pago");
            }
        }

        UsuarioSuscripcion suscripcion = new UsuarioSuscripcion();
        suscripcion.setUsuarioId(usuarioId);
        suscripcion.setPlan(plan);

        if (esPlanGratuito) {
            LocalDateTime ahora = LocalDateTime.now();
            suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
            suscripcion.setInicioPeriodoActual(ahora);
            suscripcion.setFinPeriodoActual(ahora.plusYears(100));
            log.info("Plan gratuito activado directamente");
        } else {
            suscripcion.setEstado(EstadoSuscripcion.PENDIENTE_PAGO);
            log.info("Plan premium en PENDIENTE_PAGO");
        }

        suscripcion = suscripcionRepositorio.save(suscripcion);
        log.info("Suscripcion creada - ID: {}, Estado: {}", suscripcion.getId(), suscripcion.getEstado());

        return RespuestaSuscripcionDTO.builder()
            .exito(true)
            .mensaje(esPlanGratuito ? "Suscripcion gratuita activada" : "Suscripcion creada. Pendiente de pago.")
            .idSuscripcion(suscripcion.getId())
            .plan(plan.getNombre())
            .estado(suscripcion.getEstado().name())
            .precioPlan(plan.getPrecio())
            .requierePago(calcularRequierePago(suscripcion))
            .build();
    }

    public RespuestaSuscripcionDTO obtenerEstado(Integer usuarioId) {
        log.info("Consultando estado - Usuario: {}", usuarioId);

        var suscripcionOpt = suscripcionRepositorio.findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA);

        if (suscripcionOpt.isEmpty()) {
            List<UsuarioSuscripcion> historial = suscripcionRepositorio
                .findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
            if (!historial.isEmpty()) {
                UsuarioSuscripcion ultima = historial.get(0);
                EstadoSuscripcion ultEst = ultima.getEstado();
                if (ultEst == EstadoSuscripcion.CANCELADA || ultEst == EstadoSuscripcion.EXPIRADA) {
                    BigDecimal precioPremium = planRepositorio.findByNombreIgnoreCase("premium")
                        .map(PlanSuscripcion::getPrecio)
                        .orElse(null);
                    return RespuestaSuscripcionDTO.builder()
                        .exito(true)
                        .mensaje(ultEst == EstadoSuscripcion.CANCELADA
                            ? "Sin suscripcion activa"
                            : "Suscripcion expirada")
                        .plan("gratuito")
                        .estado(EstadoSuscripcion.SIN_SUSCRIPCION.name())
                        .precioPlan(precioPremium)
                        .requierePago(false)
                        .build();
                }
                return construirRespuestaEstado(ultima);
            }
            BigDecimal precioPremium = planRepositorio.findByNombreIgnoreCase("premium")
                .map(PlanSuscripcion::getPrecio)
                .orElse(null);
            return RespuestaSuscripcionDTO.builder()
                .exito(true)
                .mensaje("Sin suscripcion activa")
                .plan("gratuito")
                .estado(EstadoSuscripcion.SIN_SUSCRIPCION.name())
                .precioPlan(precioPremium)
                .requierePago(false)
                .build();
        }

        return construirRespuestaEstado(suscripcionOpt.get());
    }

    @Transactional
    public RespuestaSuscripcionDTO cancelarSuscripcion(String suscripcionId, boolean inmediata, Integer usuarioId) {
        log.info("Cancelando - ID: {}, Inmediata: {}, Usuario: {}", suscripcionId, inmediata, usuarioId);

        UsuarioSuscripcion suscripcion = suscripcionRepositorio.findById(suscripcionId)
            .orElseThrow(() -> new IllegalStateException("Suscripcion no encontrada: " + suscripcionId));

        if (!suscripcion.getUsuarioId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para cancelar esta suscripcion");
        }

        if (inmediata) {
            suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
            suscripcion.setFechaCancelacion(LocalDateTime.now());

            // FIX CAL-3: Revertir rol a GRATUITO en auth-service
            authServicio.revertirAGratuito(suscripcion.getUsuarioId());
        } else {
            suscripcion.setCancelarAlFinalPeriodo(true);
        }

        suscripcionRepositorio.save(suscripcion);

        try {
            servicioNotificaciones.notificarSuscripcionCancelada(
                usuarioId,
                suscripcionId,
                inmediata,
                "usuario");
        } catch (Exception e) {
            log.warn("No se pudo notificar admin cancelación: {}", e.getMessage());
        }

        return RespuestaSuscripcionDTO.builder()
            .exito(true)
            .mensaje(inmediata ? "Suscripcion cancelada inmediatamente"
                : "Suscripcion se cancelara al final del periodo")
            .idSuscripcion(suscripcionId)
            .estado(suscripcion.getEstado().name())
            .cancelarAlFinalPeriodo(suscripcion.getCancelarAlFinalPeriodo())
            .fechaEfectoCancelacion(inmediata ? LocalDateTime.now() : suscripcion.getFinPeriodoActual())
            .build();
    }

    private RespuestaSuscripcionDTO construirRespuestaEstado(UsuarioSuscripcion suscripcion) {
        LocalDateTime ahora = LocalDateTime.now();
        Long diasRestantes = null;
        Long horasRestantes = null;

        if (suscripcion.getFinPeriodoActual() != null) {
            diasRestantes = ChronoUnit.DAYS.between(ahora, suscripcion.getFinPeriodoActual());
            horasRestantes = ChronoUnit.HOURS.between(ahora, suscripcion.getFinPeriodoActual());
        }

        return RespuestaSuscripcionDTO.builder()
            .exito(true)
            .idSuscripcion(suscripcion.getId())
            .plan(suscripcion.getPlan().getNombre())
            .estado(suscripcion.getEstado().name())
            .precioPlan(suscripcion.getPlan().getPrecio())
            .fechaInicio(suscripcion.getInicioPeriodoActual())
            .fechaExpiracion(suscripcion.getFinPeriodoActual())
            .diasRestantes(diasRestantes)
            .horasRestantes(horasRestantes)
            .cancelarAlFinalPeriodo(suscripcion.getCancelarAlFinalPeriodo())
            .fechaEfectoCancelacion(
                Boolean.TRUE.equals(suscripcion.getCancelarAlFinalPeriodo())
                    ? suscripcion.getFinPeriodoActual() : null)
            .requierePago(calcularRequierePago(suscripcion))
            .build();
    }

    /**
     * Premium ACTIVA sin ningun pago COMPLETADO en BD se considera pendiente de cobro (checkout).
     */
    private boolean calcularRequierePago(UsuarioSuscripcion suscripcion) {
        if (suscripcion.getEstado() == EstadoSuscripcion.PENDIENTE_PAGO) {
            return true;
        }
        if (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA
                && suscripcion.getPlan() != null
                && "premium".equalsIgnoreCase(suscripcion.getPlan().getNombre())) {
            return !pagoRepositorio.existsBySuscripcionIdAndEstado(
                suscripcion.getId(), EstadoPago.COMPLETADO);
        }
        return false;
    }
}
