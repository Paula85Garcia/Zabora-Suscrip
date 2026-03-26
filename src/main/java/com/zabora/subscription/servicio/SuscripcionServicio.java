package com.zabora.subscription.servicio;

import com.zabora.subscription.data.UserContext;
import com.zabora.subscription.data.UserData;
import com.zabora.subscription.modelo.dto.RespuestaSuscripcionDTO;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import com.zabora.subscription.modelo.entidad.PlanSuscripcion;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.PlanSuscripcionRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuscripcionServicio {

    private static final Logger log = LoggerFactory.getLogger(SuscripcionServicio.class);

    private final PlanSuscripcionRepositorio planRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;

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

        boolean tieneActiva = suscripcionRepositorio
            .findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA)
            .isPresent();

        if (tieneActiva) {
            throw new IllegalStateException("El usuario ya tiene una suscripcion activa");
        }

        UsuarioSuscripcion suscripcion = new UsuarioSuscripcion();
        suscripcion.setUsuarioId(usuarioId);
        suscripcion.setPlan(plan);

        boolean esPlanGratuito = "gratuito".equalsIgnoreCase(plan.getNombre());

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
            .requierePago(!esPlanGratuito)
            .build();
    }

    public RespuestaSuscripcionDTO obtenerEstado(Integer usuarioId) {
        log.info("Consultando estado - Usuario: {}", usuarioId);

        var suscripcionOpt = suscripcionRepositorio.findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA);

        if (suscripcionOpt.isEmpty()) {
            List<UsuarioSuscripcion> historial = suscripcionRepositorio
                .findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
            if (!historial.isEmpty()) {
                return construirRespuestaEstado(historial.get(0));
            }
            return RespuestaSuscripcionDTO.builder()
                .exito(true)
                .mensaje("Sin suscripcion activa")
                .plan("gratuito")
                .estado(EstadoSuscripcion.SIN_SUSCRIPCION.name())
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
        } else {
            suscripcion.setCancelarAlFinalPeriodo(true);
        }

        suscripcionRepositorio.save(suscripcion);

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
            .fechaInicio(suscripcion.getInicioPeriodoActual())
            .fechaExpiracion(suscripcion.getFinPeriodoActual())
            .diasRestantes(diasRestantes)
            .horasRestantes(horasRestantes)
            .cancelarAlFinalPeriodo(suscripcion.getCancelarAlFinalPeriodo())
            .fechaEfectoCancelacion(
                Boolean.TRUE.equals(suscripcion.getCancelarAlFinalPeriodo())
                    ? suscripcion.getFinPeriodoActual() : null)
            .requierePago(suscripcion.getEstado() == EstadoSuscripcion.PENDIENTE_PAGO)
            .build();
    }
}