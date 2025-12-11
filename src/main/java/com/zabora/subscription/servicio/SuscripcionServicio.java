package com.zabora.subscription.servicio;

import com.zabora.subscription.excepcion.RecursoNoEncontradoException;
import com.zabora.subscription.excepcion.SuscripcionException;
import com.zabora.subscription.modelo.dto.RespuestaSuscripcionDTO;
import com.zabora.subscription.modelo.dto.RespuestaVerificacionDTO;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import com.zabora.subscription.modelo.entidad.LogSuscripcion;
import com.zabora.subscription.modelo.entidad.PlanSuscripcion;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.AccionLog;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.repositorio.LogSuscripcionRepository;
import com.zabora.subscription.repositorio.PlanSuscripcionRepository;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuscripcionServicio {
    
    private final UsuarioSuscripcionRepository suscripcionRepository;
    private final PlanSuscripcionRepository planRepository;
    private final LogSuscripcionRepository logRepository;
    
    /**
     * EXPLICACIÓN :
     * Este método es cuando   quieres comprar una membresía.
     * 1. Verificas que el plan existe (premium o gratuito)
     * 2. Verificas que no tengas ya una membresía activa
     * 3. Si es gratuita, la tendras de inmediato
     * 4. Si es premium, te crea la suscripción pero tienes que pagar primero
     */
    @Transactional
    public RespuestaSuscripcionDTO suscribirse(String usuarioId, SolicitudSuscripcionDTO solicitud) {
        log.info("Usuario {} suscribiéndose al plan {}", usuarioId, solicitud.getNombrePlan());
        
        // 1. Buscar el plan en la base de datos
        PlanSuscripcion plan = planRepository.findByNombre(solicitud.getNombrePlan())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Plan no encontrado: " + solicitud.getNombrePlan()));
        
        // 2. Verificar si ya tiene suscripción activa
        Optional<UsuarioSuscripcion> suscripcionExistente = 
            suscripcionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA);
        
        if (suscripcionExistente.isPresent()) {
            throw new SuscripcionException("El usuario ya tiene una suscripción activa");
        }
        
        // 3. Crear nueva suscripción
        UsuarioSuscripcion nuevaSuscripcion = new UsuarioSuscripcion();
        nuevaSuscripcion.setId(UUID.randomUUID().toString());
        nuevaSuscripcion.setUsuarioId(usuarioId);
        nuevaSuscripcion.setPlan(plan);
        
        // 4. Si es plan gratuito, activar inmediatamente
        if ("gratuito".equalsIgnoreCase(solicitud.getNombrePlan())) {
            nuevaSuscripcion.setEstado(EstadoSuscripcion.ACTIVA);
            nuevaSuscripcion.setInicioPeriodoActual(LocalDateTime.now());
            // Plan gratuito no expira
            nuevaSuscripcion.setFinPeriodoActual(null);
            
            // Guardar en base de datos
            suscripcionRepository.save(nuevaSuscripcion);
            
            // Registrar log
            registrarLog(nuevaSuscripcion.getId(), usuarioId, AccionLog.CREACION, 
                null, EstadoSuscripcion.ACTIVA.name(), 
                "Suscripción gratuita creada", usuarioId);
            
            log.info(" Suscripción gratuita creada: {}", nuevaSuscripcion.getId());
            
            return RespuestaSuscripcionDTO.builder()
                .exito(true)
                .mensaje("Suscripción gratuita activada exitosamente")
                .idSuscripcion(nuevaSuscripcion.getId())
                .plan(plan.getNombre())
                .estado(EstadoSuscripcion.ACTIVA.name())
                .limites(obtenerLimitesPlan(plan))
                .requierePago(false)
                .build();
        }
        
        // 5. Si es premium, crear pendiente de pago
        nuevaSuscripcion.setEstado(EstadoSuscripcion.PENDIENTE_PAGO);
        suscripcionRepository.save(nuevaSuscripcion);
        
        // Registrar log
        registrarLog(nuevaSuscripcion.getId(), usuarioId, AccionLog.CREACION,
            null, EstadoSuscripcion.PENDIENTE_PAGO.name(),
            "Suscripción premium creada, pendiente de pago", usuarioId);
        
        log.info("💳 Suscripción premium creada, pendiente de pago: {}", nuevaSuscripcion.getId());
        
        // Simular PaymentIntent (en producción esto vendría de Stripe real)
        Map<String, Object> paymentIntent = new HashMap<>();
        paymentIntent.put("id", "pi_" + System.currentTimeMillis());
        paymentIntent.put("cliente_secreto", "secret_" + UUID.randomUUID());
        paymentIntent.put("monto", plan.getPrecio());
        paymentIntent.put("moneda", plan.getMoneda());
        paymentIntent.put("estado", "REQUIERE_METODO_PAGO");
        
        return RespuestaSuscripcionDTO.builder()
            .exito(true)
            .mensaje("Suscripción premium creada. Proceda con el pago.")
            .idSuscripcion(nuevaSuscripcion.getId())
            .plan(plan.getNombre())
            .estado(EstadoSuscripcion.PENDIENTE_PAGO.name())
            .limites(obtenerLimitesPlan(plan))
            .requierePago(true)
            .intentoPago(paymentIntent)
            .build();
    }
    
    /**
     * EXPLICACIÓN :
     * Es como cancelar tu suscripción de Netflix.
     * 1. Verificas que la suscripción existe
     * 2. La cancelas
     * 3. Si cancelaste en las primeras 24 horas, puedes pedir reembolso
     */
    @Transactional
    public RespuestaSuscripcionDTO cancelarSuscripcion(String usuarioId, String idSuscripcion) {
        log.info("Usuario {} cancelando suscripción {}", usuarioId, idSuscripcion);
        
        // 1. Buscar la suscripción
        UsuarioSuscripcion suscripcion = suscripcionRepository.findById(idSuscripcion)
            .orElseThrow(() -> new RecursoNoEncontradoException("Suscripción no encontrada"));
        
        // 2. Verificar que pertenece al usuario
        if (!suscripcion.getUsuarioId().equals(usuarioId)) {
            throw new SuscripcionException("No tienes permiso para cancelar esta suscripción");
        }
        
        // 3. Verificar que no esté ya cancelada
        if (suscripcion.getEstado() == EstadoSuscripcion.CANCELADA) {
            throw new SuscripcionException("La suscripción ya está cancelada");
        }
        
        // 4. Guardar estado anterior para el log
        EstadoSuscripcion estadoAnterior = suscripcion.getEstado();
        
        // 5. Cancelar suscripción
        suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
        suscripcion.setFechaCancelacion(LocalDateTime.now());
        suscripcion.setCancelarAlFinalPeriodo(true);
        
        suscripcionRepository.save(suscripcion);
        
        // 6. Verificar elegibilidad para reembolso (primeras 24 horas)
        boolean elegibleReembolso = esElegibleParaReembolso(suscripcion.getFechaCreacion());
        
        // 7. Registrar log
        String descripcionLog = elegibleReembolso 
            ? "Suscripción cancelada - Elegible para reembolso"
            : "Suscripción cancelada";
            
        registrarLog(idSuscripcion, usuarioId, AccionLog.CANCELACION,
            estadoAnterior.name(), EstadoSuscripcion.CANCELADA.name(),
            descripcionLog, usuarioId);
        
        if (elegibleReembolso) {
            registrarLog(idSuscripcion, usuarioId, AccionLog.REEMBOLSO,
                null, null, "Reembolso automático por cancelación en 24 horas", "sistema");
        }
        
        log.info("Suscripción cancelada exitosamente: {}", idSuscripcion);
        
        return RespuestaSuscripcionDTO.builder()
            .exito(true)
            .mensaje("Suscripción cancelada exitosamente")
            .idSuscripcion(suscripcion.getId())
            .plan(suscripcion.getPlan().getNombre())
            .estado(suscripcion.getEstado().name())
            .elegibleReembolso(elegibleReembolso)
            .fechaCancelacion(suscripcion.getFechaCancelacion())
            .build();
    }
    
    /**
     * EXPLICACIÓN:
     * Es como cuando otros servicios (Recipe Service) preguntan:
     * "¿Este usuario tiene premium activo?"
     * Este método responde rápido para que puedan tomar decisiones.
     */
    @Transactional(readOnly = true)
    public RespuestaVerificacionDTO verificarSuscripcion(String usuarioId) {
        log.debug("🔍 Verificando suscripción para usuario: {}", usuarioId);
        
        // Buscar suscripción activa
        Optional<UsuarioSuscripcion> suscripcionOpt = 
            suscripcionRepository.findByUsuarioIdAndEstado(usuarioId, EstadoSuscripcion.ACTIVA);
        
        if (suscripcionOpt.isEmpty()) {
            // No tiene suscripción activa - devolver límites gratuitos
            PlanSuscripcion planGratuito = planRepository.findByNombre("gratuito")
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan gratuito no encontrado"));
            
            return RespuestaVerificacionDTO.builder()
                .valida(false)
                .plan("gratuito")
                .estado("SIN_SUSCRIPCION")
                .limites(obtenerLimitesPlan(planGratuito))
                .build();
        }
        
        UsuarioSuscripcion suscripcion = suscripcionOpt.get();
        
        // Verificar si está expirada (solo para premium)
        if (suscripcion.getFinPeriodoActual() != null && 
            suscripcion.getFinPeriodoActual().isBefore(LocalDateTime.now())) {
            
            // Marcar como expirada
            suscripcion.setEstado(EstadoSuscripcion.EXPIRADA);
            suscripcionRepository.save(suscripcion);
            
            registrarLog(suscripcion.getId(), usuarioId, AccionLog.CAMBIO_ESTADO,
                EstadoSuscripcion.ACTIVA.name(), EstadoSuscripcion.EXPIRADA.name(),
                "Suscripción expirada automáticamente", "sistema");
            
            PlanSuscripcion planGratuito = planRepository.findByNombre("gratuito")
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan gratuito no encontrado"));
            
            return RespuestaVerificacionDTO.builder()
                .valida(false)
                .plan("gratuito")
                .estado("EXPIRADA")
                .limites(obtenerLimitesPlan(planGratuito))
                .build();
        }
        
        // Suscripción activa y válida
        boolean esPremium = "premium".equalsIgnoreCase(suscripcion.getPlan().getNombre());
        
        return RespuestaVerificacionDTO.builder()
            .valida(esPremium)
            .plan(suscripcion.getPlan().getNombre())
            .estado(suscripcion.getEstado().name())
            .fechaExpiracion(suscripcion.getFinPeriodoActual())
            .limites(obtenerLimitesPlan(suscripcion.getPlan()))
            .build();
    }
    
    /**
     * EXPLICACIÓN:
     * Muestra toda la información de la suscripción del usuario,
     * como si miraras tu perfil de Netflix.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadoSuscripcion(String usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();
        
        List<UsuarioSuscripcion> suscripciones = suscripcionRepository.findByUsuarioId(usuarioId);
        
        if (suscripciones.isEmpty()) {
            // Usuario sin suscripciones
            PlanSuscripcion planGratuito = planRepository.findByNombre("gratuito")
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan gratuito no encontrado"));
            
            respuesta.put("usuario_id", usuarioId);
            respuesta.put("tiene_suscripcion", false);
            respuesta.put("plan_actual", planGratuito);
            respuesta.put("limites", obtenerLimitesPlan(planGratuito));
            respuesta.put("es_premium", false);
            
            return respuesta;
        }
        
        // Obtener suscripción activa (si existe)
        Optional<UsuarioSuscripcion> suscripcionActiva = suscripciones.stream()
            .filter(s -> s.getEstado() == EstadoSuscripcion.ACTIVA)
            .findFirst();
        
        if (suscripcionActiva.isPresent()) {
            UsuarioSuscripcion suscripcion = suscripcionActiva.get();
            
            respuesta.put("usuario_id", usuarioId);
            respuesta.put("tiene_suscripcion", true);
            respuesta.put("suscripcion_id", suscripcion.getId());
            respuesta.put("plan_actual", suscripcion.getPlan());
            respuesta.put("estado", suscripcion.getEstado().name());
            respuesta.put("inicio_periodo", suscripcion.getInicioPeriodoActual());
            respuesta.put("fin_periodo", suscripcion.getFinPeriodoActual());
            respuesta.put("limites", obtenerLimitesPlan(suscripcion.getPlan()));
            respuesta.put("es_premium", "premium".equalsIgnoreCase(suscripcion.getPlan().getNombre()));
            
        } else {
            // Tiene suscripciones pero ninguna activa
            PlanSuscripcion planGratuito = planRepository.findByNombre("gratuito")
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan gratuito no encontrado"));
            
            respuesta.put("usuario_id", usuarioId);
            respuesta.put("tiene_suscripcion", false);
            respuesta.put("plan_actual", planGratuito);
            respuesta.put("limites", obtenerLimitesPlan(planGratuito));
            respuesta.put("es_premium", false);
            respuesta.put("historial_suscripciones", suscripciones);
        }
        
        return respuesta;
    }
    
    /**
     *Obtener todos los planes disponibles
     */
    @Transactional(readOnly = true)
    public List<PlanSuscripcion> obtenerPlanes() {
        return planRepository.findByActivoTrue();
    }
    
    /**
     * Activar suscripción premium después del pago
     * Este método es llamado por PagoServicio cuando el pago se completa
     */
    @Transactional
    public void activarSuscripcionPremium(String suscripcionId, String stripeSubscriptionId) {
        log.info("Activando suscripción premium: {}", suscripcionId);
        
        UsuarioSuscripcion suscripcion = suscripcionRepository.findById(suscripcionId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Suscripción no encontrada"));
        
        EstadoSuscripcion estadoAnterior = suscripcion.getEstado();
        
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setInicioPeriodoActual(LocalDateTime.now());
        suscripcion.setFinPeriodoActual(LocalDateTime.now().plusMonths(1)); // 30 días
        suscripcion.setIdSuscripcionStripe(stripeSubscriptionId);
        
        suscripcionRepository.save(suscripcion);
        
        registrarLog(suscripcionId, suscripcion.getUsuarioId(), AccionLog.ACTIVACION,
            estadoAnterior.name(), EstadoSuscripcion.ACTIVA.name(),
            "Suscripción premium activada tras pago exitoso", "sistema");
        
        log.info("Suscripción premium activada exitosamente: {}", suscripcionId);
    }
    
    // ========== MÉTODOS AUXILIARES ==========
    
    private Map<String, Object> obtenerLimitesPlan(PlanSuscripcion plan) {
        Map<String, Object> limites = new HashMap<>();
        limites.put("condiciones_medicas", plan.getLimiteCondicionesMedicas());
        limites.put("alergias", plan.getLimiteAlergias());
        limites.put("preferencias_alimentarias", plan.getLimitePreferenciasAlimentarias());
        limites.put("ingredientes_por_busqueda", plan.getIngredientesPorBusqueda());
        limites.put("recetas_favoritas", plan.getLimiteRecetasFavoritas());
        return limites;
    }
    
    private boolean esElegibleParaReembolso(LocalDateTime fechaCreacion) {
        return LocalDateTime.now().minusHours(24).isBefore(fechaCreacion);
    }
    
    private void registrarLog(String suscripcionId, String usuarioId, AccionLog accion,
                            String estadoAnterior, String estadoNuevo, 
                            String descripcion, String realizadoPor) {
        LogSuscripcion log = new LogSuscripcion();
        log.setSuscripcionId(suscripcionId);
        log.setUsuarioId(usuarioId);
        log.setAccion(accion);
        log.setEstadoAnterior(estadoAnterior);
        log.setEstadoNuevo(estadoNuevo);
        log.setDescripcion(descripcion);
        log.setRealizadoPor(realizadoPor);
        
        logRepository.save(log);
    }
}