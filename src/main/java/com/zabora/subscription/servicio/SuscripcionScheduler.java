package com.zabora.subscription.servicio;

import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FEAT-1 & FEAT-2: Jobs programados para procesar expiraciones y cancelaciones diferidas.
 *
 * - procesarExpiraciones:            Cada hora, busca suscripciones ACTIVAS cuyo fin_periodo_actual < ahora.
 * - procesarCancelacionesDiferidas:  Cada hora, busca suscripciones ACTIVAS con cancelar_al_final_periodo=true
 *                                    cuyo periodo ya termino.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuscripcionScheduler {

    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;
    private final AuthServicio authServicio;

    /**
     * FEAT-1: Procesa suscripciones expiradas cada hora.
     * Busca suscripciones ACTIVAS cuyo fin_periodo_actual ya paso y las marca como EXPIRADA.
     */
    @Scheduled(fixedRate = 3600000) // cada hora
    @Transactional
    public void procesarExpiraciones() {
        LocalDateTime ahora = LocalDateTime.now();
        log.info("[Scheduler] Procesando expiraciones — fecha actual: {}", ahora);

        List<UsuarioSuscripcion> expiradas = suscripcionRepositorio
            .findByEstadoAndFinPeriodoActualBefore(EstadoSuscripcion.ACTIVA, ahora);

        if (expiradas.isEmpty()) {
            log.info("[Scheduler] No hay suscripciones por expirar");
            return;
        }

        log.info("[Scheduler] {} suscripciones por expirar", expiradas.size());

        int procesadas = 0;
        for (UsuarioSuscripcion suscripcion : expiradas) {
            try {
                // Solo expirar si NO tiene cancelacion diferida (esas se procesan aparte)
                if (Boolean.TRUE.equals(suscripcion.getCancelarAlFinalPeriodo())) {
                    continue;
                }

                suscripcion.setEstado(EstadoSuscripcion.EXPIRADA);
                suscripcion.setFechaActualizacion(ahora);
                suscripcionRepositorio.save(suscripcion);

                // Revertir rol a GRATUITO
                authServicio.revertirAGratuitoSilencioso(suscripcion.getUsuarioId());

                procesadas++;
                log.info("[Scheduler] Suscripcion {} expirada — usuario: {}",
                    suscripcion.getId(), suscripcion.getUsuarioId());

            } catch (Exception e) {
                log.error("[Scheduler] Error expirando suscripcion {}: {}",
                    suscripcion.getId(), e.getMessage());
            }
        }

        log.info("[Scheduler] Expiraciones completadas: {}/{}", procesadas, expiradas.size());
    }

    /**
     * FEAT-2: Procesa cancelaciones diferidas cada hora.
     * Busca suscripciones ACTIVAS con cancelar_al_final_periodo=true cuyo periodo ya termino.
     */
    @Scheduled(fixedRate = 3600000) // cada hora
    @Transactional
    public void procesarCancelacionesDiferidas() {
        LocalDateTime ahora = LocalDateTime.now();
        log.info("[Scheduler] Procesando cancelaciones diferidas — fecha actual: {}", ahora);

        List<UsuarioSuscripcion> pendientes = suscripcionRepositorio
            .findByCancelarAlFinalPeriodoTrueAndEstadoAndFinPeriodoActualBefore(
                EstadoSuscripcion.ACTIVA, ahora);

        if (pendientes.isEmpty()) {
            log.info("[Scheduler] No hay cancelaciones diferidas pendientes");
            return;
        }

        log.info("[Scheduler] {} cancelaciones diferidas por procesar", pendientes.size());

        int procesadas = 0;
        for (UsuarioSuscripcion suscripcion : pendientes) {
            try {
                suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
                suscripcion.setFechaCancelacion(ahora);
                suscripcion.setFechaActualizacion(ahora);
                suscripcionRepositorio.save(suscripcion);

                // Revertir rol a GRATUITO
                authServicio.revertirAGratuitoSilencioso(suscripcion.getUsuarioId());

                procesadas++;
                log.info("[Scheduler] Suscripcion {} cancelada (diferida) — usuario: {}",
                    suscripcion.getId(), suscripcion.getUsuarioId());

            } catch (Exception e) {
                log.error("[Scheduler] Error cancelando suscripcion diferida {}: {}",
                    suscripcion.getId(), e.getMessage());
            }
        }

        log.info("[Scheduler] Cancelaciones diferidas completadas: {}/{}", procesadas, pendientes.size());
    }
}
