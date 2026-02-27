package com.zabora.subscription.servicio;

import com.zabora.subscription.modelo.dto.RespuestaSuscripcionDTO;
import com.zabora.subscription.modelo.dto.RespuestaVerificacionDTO;
import com.zabora.subscription.modelo.entidad.PlanSuscripcion;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.PlanSuscripcionRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Servicio principal de suscripciones
 * Aquí manejamos todo lo relacionado con crear, activar, cancelar y expirar suscripciones
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class SuscripcionServicioReal {
    
    private final PlanSuscripcionRepositorio planRepository;
    private final UsuarioSuscripcionRepositorio suscripcionRepository;
    
    /**
     * Suscribe a un usuario a un plan
     * Si el plan es gratis, se activa al instante
     * Si es premium, primero hay que pagar
     */

    @Transactional
    public RespuestaSuscripcionDTO suscribirse(String usuarioId, SolicitudSuscripcionDTO solicitud) {
        log.info("User {} subscribing to plan {}", usuarioId, solicitud.getNombrePlan());
        
        // Find plan
        PlanSuscripcion plan = planRepository.findByNombreIgnoreCase(solicitud.getNombrePlan())
            .orElseThrow(() -> new RuntimeException("Plan not found: " + solicitud.getNombrePlan()));
        
     // Revisamos si el usuario ya tiene una suscripción activa
        Optional<UsuarioSuscripcion> existingSubscription = 
            suscripcionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA);
        
        if (existingSubscription.isPresent()) {
            throw new RuntimeException("User already has an active subscription");
        }
        
     // Creamos la nueva suscripción
        UsuarioSuscripcion nuevaSuscripcion = new UsuarioSuscripcion();
        String suscripcionId = "sub_" + UUID.randomUUID().toString();
        nuevaSuscripcion.setId(suscripcionId);
        nuevaSuscripcion.setUsuarioId(usuarioId);
        nuevaSuscripcion.setPlan(plan);
        nuevaSuscripcion.setFechaCreacion(LocalDateTime.now());
        
     // Si es plan gratuito, lo activamos de inmediato
        if ("gratuito".equalsIgnoreCase(solicitud.getNombrePlan())) {
            nuevaSuscripcion.setEstado(EstadoSuscripcion.ACTIVA);
            nuevaSuscripcion.setInicioPeriodoActual(LocalDateTime.now());
         // El plan gratuito no tiene fecha de vencimiento
            nuevaSuscripcion.setFinPeriodoActual(null);
            
            suscripcionRepository.save(nuevaSuscripcion);
            
            return RespuestaSuscripcionDTO.builder()
                .exito(true)
                .mensaje("Free subscription activated successfully")
                .idSuscripcion(suscripcionId)
                .plan(plan.getNombre())
                .estado(EstadoSuscripcion.ACTIVA.name())
                .fechaInicio(nuevaSuscripcion.getInicioPeriodoActual())
                .fechaExpiracion(null)
//                .limites(obtenerLimitesPlan(plan))
                .requierePago(false)
                .build();
        }
        
     // Si es premium, lo dejamos pendiente hasta que pague

        nuevaSuscripcion.setEstado(EstadoSuscripcion.PENDIENTE_PAGO);
        suscripcionRepository.save(nuevaSuscripcion);
        
     // Armamos la info necesaria para el intento de pago
        Map<String, Object> paymentIntent = new HashMap<>();
        paymentIntent.put("monto", plan.getPrecio());
        paymentIntent.put("moneda", plan.getMoneda());
        paymentIntent.put("estado", "REQUIERE_METODO_PAGO");
        
        return RespuestaSuscripcionDTO.builder()
            .exito(true)
            .mensaje("Premium subscription created. Payment required.")
            .idSuscripcion(suscripcionId)
            .plan(plan.getNombre())
            .estado(EstadoSuscripcion.PENDIENTE_PAGO.name())
//            .limites(obtenerLimitesPlan(plan))
            .requierePago(true)
            .intentoPago(paymentIntent)
            .build();
    }
    
    /**
     * Cancela una suscripción
     * Puede ser inmediata o al final del período actual
     */

    @Transactional
    public RespuestaSuscripcionDTO cancelarSuscripcion(
            String usuarioId, 
            String idSuscripcion,
            Boolean inmediata) {
        
        log.info("User {} cancelling subscription {} (immediate: {})", 
                usuarioId, idSuscripcion, inmediata);
        
        UsuarioSuscripcion suscripcion = suscripcionRepository.findById(idSuscripcion)
            .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        if (!suscripcion.getUsuarioId().equals(usuarioId)) {
            throw new RuntimeException("Subscription does not belong to user");
        }
        
        if (suscripcion.getEstado() == EstadoSuscripcion.CANCELADA) {
            throw new RuntimeException("Subscription is already cancelled");
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fechaEfectoCancelacion;
        Long horasHastaCancelacion = null;
        Long diasRestantes = null;
        
        if (Boolean.TRUE.equals(inmediata)) {
        	// Cancelación inmediata: se corta el acceso ya mismo
            suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
            suscripcion.setFechaCancelacion(now);
            suscripcion.setFinPeriodoActual(now);
            suscripcion.setCancelarAlFinalPeriodo(false);
            fechaEfectoCancelacion = now;
            
            log.warn("IMMEDIATE cancellation - Premium access terminated");
            
        } else {
        	// Cancelación al final del período: sigue activo hasta que termine

            suscripcion.setCancelarAlFinalPeriodo(true);
            suscripcion.setFechaCancelacion(now);
            fechaEfectoCancelacion = suscripcion.getFinPeriodoActual();
            
            if (fechaEfectoCancelacion != null) {
                horasHastaCancelacion = ChronoUnit.HOURS.between(now, fechaEfectoCancelacion);
                diasRestantes = ChronoUnit.DAYS.between(now, fechaEfectoCancelacion);
            }
            
            log.info("Cancellation scheduled for: {}", fechaEfectoCancelacion);
        }
        
        suscripcion.setFechaActualizacion(now);
        suscripcionRepository.save(suscripcion);
        
        boolean elegibleReembolso = esElegibleParaReembolso(suscripcion.getInicioPeriodoActual());
        
        return RespuestaSuscripcionDTO.builder()
            .exito(true)
            .mensaje(inmediata ? 
                "Subscription cancelled immediately" : 
                "Subscription will be cancelled at end of period")
            .idSuscripcion(suscripcion.getId())
            .plan(suscripcion.getPlan().getNombre())
            .estado(suscripcion.getEstado().name())
            .fechaInicio(suscripcion.getInicioPeriodoActual())
            .fechaExpiracion(suscripcion.getFinPeriodoActual())
            .diasRestantes(diasRestantes)
            .horasRestantes(horasHastaCancelacion)
            .cancelarAlFinalPeriodo(suscripcion.getCancelarAlFinalPeriodo())
            .fechaEfectoCancelacion(fechaEfectoCancelacion)
            .horasHastaCancelacion(horasHastaCancelacion)
            .elegibleReembolso(elegibleReembolso)
            .fechaCancelacion(suscripcion.getFechaCancelacion())
            .build();
    }
    
    /**
     * Verifica si el usuario tiene una suscripción premium válida
     * Lo usan otros microservicios para saber si darle acceso o no
     */

    public RespuestaVerificacionDTO verificarSuscripcion(String usuarioId) {
        Optional<UsuarioSuscripcion> suscripcionOpt = 
            suscripcionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA);
        
        if (suscripcionOpt.isEmpty()) {
        	// Devolver los límites del plan gratuito
            PlanSuscripcion planGratuito = planRepository.findByNombreIgnoreCase("gratuito")
                .orElseThrow(() -> new RuntimeException("Free plan not configured"));
            
            return RespuestaVerificacionDTO.builder()
                .valida(false)
                .plan("gratuito")
                .estado("NO_SUBSCRIPTION")
//                .limites(obtenerLimitesPlan(planGratuito))
                .build();
        }
        
        UsuarioSuscripcion suscripcion = suscripcionOpt.get();
        boolean esPremium = "premium".equalsIgnoreCase(suscripcion.getPlan().getNombre());
        
        return RespuestaVerificacionDTO.builder()
            .valida(esPremium)
            .plan(suscripcion.getPlan().getNombre())
            .estado(suscripcion.getEstado().name())
            .fechaExpiracion(suscripcion.getFinPeriodoActual())
//            .limites(obtenerLimitesPlan(suscripcion.getPlan()))
            .build();
    }
    
    /**
    * Obtener el estado de suscripción completo para un usuario
     */
    public Map<String, Object> obtenerEstadoSuscripcion(String usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();
        
        Optional<UsuarioSuscripcion> suscripcionOpt = 
            suscripcionRepository.findTopByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
        
        if (suscripcionOpt.isPresent()) {
            UsuarioSuscripcion suscripcion = suscripcionOpt.get();
            LocalDateTime now = LocalDateTime.now();
            Long diasRestantes = null;
            Long horasRestantes = null;
            Long horasHastaCancelacion = null;
            
            if (suscripcion.getFinPeriodoActual() != null) {
                diasRestantes = ChronoUnit.DAYS.between(now, suscripcion.getFinPeriodoActual());
                horasRestantes = ChronoUnit.HOURS.between(now, suscripcion.getFinPeriodoActual());
                
                if (Boolean.TRUE.equals(suscripcion.getCancelarAlFinalPeriodo())) {
                    horasHastaCancelacion = horasRestantes;
                }
            }
            
            respuesta.put("usuario_id", usuarioId);
            respuesta.put("id_suscripcion", suscripcion.getId());
            respuesta.put("plan", suscripcion.getPlan().getNombre());
            respuesta.put("estado", suscripcion.getEstado().name());
            respuesta.put("fecha_inicio", suscripcion.getInicioPeriodoActual());
            respuesta.put("fecha_expiracion", suscripcion.getFinPeriodoActual());
            respuesta.put("dias_restantes", diasRestantes);
            respuesta.put("horas_restantes", horasRestantes);
            respuesta.put("cancelar_al_final_periodo", suscripcion.getCancelarAlFinalPeriodo());
            respuesta.put("horas_hasta_cancelacion", horasHastaCancelacion);
//            respuesta.put("limites", obtenerLimitesPlan(suscripcion.getPlan()));
            respuesta.put("es_premium", 
                "premium".equalsIgnoreCase(suscripcion.getPlan().getNombre()) && 
                suscripcion.getEstado() == EstadoSuscripcion.ACTIVA);
            
        } else {
        	// No se encontró suscripción - devolver el plan gratuito
            PlanSuscripcion planGratuito = planRepository.findByNombreIgnoreCase("gratuito")
                .orElseThrow(() -> new RuntimeException("Free plan not configured"));
            
            respuesta.put("usuario_id", usuarioId);
            respuesta.put("plan", "gratuito");
//            respuesta.put("limites", obtenerLimitesPlan(planGratuito));
            respuesta.put("es_premium", false);
        }
        
        return respuesta;
    }
    
    /**
     * Obtén todos los planes disponibles
     */
    public List<PlanSuscripcion> obtenerPlanes() {
        return planRepository.findByActivoTrue();
    }
    
    /**
     * Activar suscripción tras pago exitoso
     * * Llamado del servicio de pago
*/
     
    @Transactional
    public void activarSuscripcion(String userId) {
        Optional<UsuarioSuscripcion> suscripcionOpt = 
            suscripcionRepository.findByUsuarioIdAndEstado(userId, EstadoSuscripcion.PENDIENTE_PAGO);
        
        if (suscripcionOpt.isEmpty()) {
            log.error("No pending subscription found for user: {}", userId);
            return;
        }
        
        UsuarioSuscripcion suscripcion = suscripcionOpt.get();
        LocalDateTime now = LocalDateTime.now();
        
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setInicioPeriodoActual(now);
        suscripcion.setFinPeriodoActual(now.plusDays(30)); // 30 day period
        suscripcion.setFechaActualizacion(now);
        
        suscripcionRepository.save(suscripcion);
        
        log.info("Subscription activated for user: {}", userId);
    }
    
    /**
     * Procesar cancelaciones programadas
     */
    @Transactional
    public void procesarCancelacionesProgramadas() {
        LocalDateTime now = LocalDateTime.now();
        List<UsuarioSuscripcion> toCancelList = 
            suscripcionRepository.findSubscriptionsToCancel(now);
        
        for (UsuarioSuscripcion suscripcion : toCancelList) {
            suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
            suscripcion.setFinPeriodoActual(now);
            suscripcionRepository.save(suscripcion);
            
            log.info("Subscription cancelled (scheduled): {}", suscripcion.getId());
        }
    }
    
   
    /**
     * Procesar suscripciones vencidas
     */
     
    @Transactional
    public void procesarSuscripcionesExpiradas() {
        LocalDateTime now = LocalDateTime.now();
        List<UsuarioSuscripcion> expiredList = 
            suscripcionRepository.findExpiredSubscriptions(now);
        
        for (UsuarioSuscripcion suscripcion : expiredList) {
            suscripcion.setEstado(EstadoSuscripcion.EXPIRADA);
            suscripcionRepository.save(suscripcion);
            
            log.info("Subscription expired: {}", suscripcion.getId());
        }
    }
    
    
//    
    /**
     * Verificar si la suscripción es elegible para reembolso
     * Dentro de las 24 horas posteriores a su creación
     */
    private boolean esElegibleParaReembolso(LocalDateTime fechaCreacion) {
        if (fechaCreacion == null) return false;
        return LocalDateTime.now().minusHours(24).isBefore(fechaCreacion);
    }
}