package com.zabora.subscription.servicio;

import com.zabora.subscription.modelo.dto.RespuestaSuscripcionDTO;
import com.zabora.subscription.modelo.dto.RespuestaVerificacionDTO;
import com.zabora.subscription.modelo.dto.SolicitudSuscripcionDTO;
import com.zabora.subscription.modelo.entidad.PlanSuscripcion;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import com.zabora.subscription.modelo.enumeracion.TipoMetodoPago;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class SuscripcionServicio {
    
    // DATOS QUEMADOS PARA PRUEBAS
    private final List<PlanSuscripcion> planesMock;
    private final List<UsuarioSuscripcion> suscripcionesMock;
    private final Map<String, List<Map<String, Object>>> pagosMock;
    
    public SuscripcionServicio() {
        this.planesMock = new ArrayList<>();
        this.suscripcionesMock = new ArrayList<>();
        this.pagosMock = new HashMap<>();
        inicializarDatosMock();
    }
    
    private void inicializarDatosMock() {
        // Crear planes mock
        PlanSuscripcion planGratuito = new PlanSuscripcion(
            1L, "gratuito", "Plan gratuito con características básicas", 
            BigDecimal.ZERO, 2, 2, 1, 7, 4
        );
        
        PlanSuscripcion planPremium = new PlanSuscripcion(
            2L, "premium", "Plan premium con todas las características", 
            new BigDecimal("29900.00"), 3, 4, 1, 20, null
        );
        
        planesMock.add(planGratuito);
        planesMock.add(planPremium);
        
        // Crear suscripciones mock
        UsuarioSuscripcion suscripcionPremium = new UsuarioSuscripcion();
        suscripcionPremium.setId("sub_001");
        suscripcionPremium.setUsuarioId("usuario_001");
        suscripcionPremium.setPlan(planPremium);
        suscripcionPremium.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcionPremium.setInicioPeriodoActual(LocalDateTime.now().minusDays(10));
        suscripcionPremium.setFinPeriodoActual(LocalDateTime.now().plusDays(20));
        
        UsuarioSuscripcion suscripcionGratuita = new UsuarioSuscripcion();
        suscripcionGratuita.setId("sub_002");
        suscripcionGratuita.setUsuarioId("usuario_002");
        suscripcionGratuita.setPlan(planGratuito);
        suscripcionGratuita.setEstado(EstadoSuscripcion.ACTIVA);
        
        suscripcionesMock.add(suscripcionPremium);
        suscripcionesMock.add(suscripcionGratuita);
        
        // Crear pagos mock
        List<Map<String, Object>> pagosUsuario1 = new ArrayList<>();
        Map<String, Object> pago1 = new HashMap<>();
        pago1.put("id", "pago_001");
        pago1.put("idSuscripcion", "sub_001");
        pago1.put("monto", new BigDecimal("29900.00"));
        pago1.put("metodoPago", TipoMetodoPago.TARJETA_CREDITO);
        pago1.put("estado", "COMPLETADO");
        pago1.put("fecha", LocalDateTime.now().minusDays(10));
        pago1.put("urlComprobante", "https://receipt.stripe.com/test/123");
        pagosUsuario1.add(pago1);
        pagosMock.put("usuario_001", pagosUsuario1);
        
        log.info("✅ Datos mock inicializados: {} planes, {} suscripciones", 
                planesMock.size(), suscripcionesMock.size());
    }
    
    public RespuestaSuscripcionDTO suscribirse(String usuarioId, SolicitudSuscripcionDTO solicitud) {
        log.info("👤 Usuario {} suscribiéndose al plan {}", usuarioId, solicitud.getNombrePlan());
        
        // Buscar plan
        PlanSuscripcion plan = planesMock.stream()
            .filter(p -> p.getNombre().equalsIgnoreCase(solicitud.getNombrePlan()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + solicitud.getNombrePlan()));
        
        // Verificar si ya tiene suscripción activa
        Optional<UsuarioSuscripcion> suscripcionExistente = suscripcionesMock.stream()
            .filter(s -> s.getUsuarioId().equals(usuarioId) && 
                        s.getEstado() == EstadoSuscripcion.ACTIVA)
            .findFirst();
        
        if (suscripcionExistente.isPresent()) {
            throw new RuntimeException("El usuario ya tiene una suscripción activa");
        }
        
        // Crear nueva suscripción
        UsuarioSuscripcion nuevaSuscripcion = new UsuarioSuscripcion();
        String suscripcionId = "sub_" + System.currentTimeMillis();
        nuevaSuscripcion.setId(suscripcionId);
        nuevaSuscripcion.setUsuarioId(usuarioId);
        nuevaSuscripcion.setPlan(plan);
        
        // Si es gratuito, activar inmediatamente
        if ("gratuito".equalsIgnoreCase(solicitud.getNombrePlan())) {
            nuevaSuscripcion.setEstado(EstadoSuscripcion.ACTIVA);
            nuevaSuscripcion.setInicioPeriodoActual(LocalDateTime.now());
            suscripcionesMock.add(nuevaSuscripcion);
            
            return RespuestaSuscripcionDTO.builder()
                .exito(true)
                .mensaje("Suscripción gratuita activada exitosamente")
                .idSuscripcion(suscripcionId)
                .plan(plan.getNombre())
                .estado(EstadoSuscripcion.ACTIVA.name())
                .limites(obtenerLimitesPlan(plan))
                .requierePago(false)
                .build();
        }
        
        // Si es premium, crear pendiente de pago
        nuevaSuscripcion.setEstado(EstadoSuscripcion.PENDIENTE_PAGO);
        suscripcionesMock.add(nuevaSuscripcion);
        
        // Simular creación de PaymentIntent
        Map<String, Object> paymentIntent = new HashMap<>();
        String intentId = "pi_" + System.currentTimeMillis();
        paymentIntent.put("id", intentId);
        paymentIntent.put("cliente_secreto", "secret_" + System.currentTimeMillis());
        paymentIntent.put("monto", plan.getPrecio());
        paymentIntent.put("moneda", plan.getMoneda());
        paymentIntent.put("estado", "REQUIERE_METODO_PAGO");
        
        return RespuestaSuscripcionDTO.builder()
            .exito(true)
            .mensaje("Suscripción premium creada. Proceda con el pago.")
            .idSuscripcion(suscripcionId)
            .plan(plan.getNombre())
            .estado(EstadoSuscripcion.PENDIENTE_PAGO.name())
            .limites(obtenerLimitesPlan(plan))
            .requierePago(true)
            .intentoPago(paymentIntent)
            .build();
    }
    
    public RespuestaSuscripcionDTO cancelarSuscripcion(String usuarioId, String idSuscripcion) {
        log.info("❌ Usuario {} cancelando suscripción {}", usuarioId, idSuscripcion);
        
        UsuarioSuscripcion suscripcion = suscripcionesMock.stream()
            .filter(s -> s.getId().equals(idSuscripcion) && s.getUsuarioId().equals(usuarioId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Suscripción no encontrada"));
        
        if (suscripcion.getEstado() == EstadoSuscripcion.CANCELADA) {
            throw new RuntimeException("La suscripción ya está cancelada");
        }
        
        // Marcar como cancelada
        suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
        suscripcion.setFechaCancelacion(LocalDateTime.now());
        suscripcion.setFechaActualizacion(LocalDateTime.now());
        
        // Verificar si es elegible para reembolso (primeras 24 horas)
        boolean elegibleReembolso = esElegibleParaReembolso(suscripcion.getFechaCreacion());
        
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
    
    public RespuestaVerificacionDTO verificarSuscripcion(String usuarioId) {
        Optional<UsuarioSuscripcion> suscripcionOpt = suscripcionesMock.stream()
            .filter(s -> s.getUsuarioId().equals(usuarioId) && 
                        s.getEstado() == EstadoSuscripcion.ACTIVA)
            .findFirst();
        
        if (suscripcionOpt.isEmpty()) {
            // Usuario sin suscripción activa - devolver límites gratuitos
            PlanSuscripcion planGratuito = planesMock.get(0);
            return RespuestaVerificacionDTO.builder()
                .valida(false)
                .plan("gratuito")
                .estado("SIN_SUSCRIPCION")
                .limites(obtenerLimitesPlan(planGratuito))
                .build();
        }
        
        UsuarioSuscripcion suscripcion = suscripcionOpt.get();
        boolean esPremium = "premium".equalsIgnoreCase(suscripcion.getPlan().getNombre());
        
        return RespuestaVerificacionDTO.builder()
            .valida(esPremium)
            .plan(suscripcion.getPlan().getNombre())
            .estado(suscripcion.getEstado().name())
            .fechaExpiracion(suscripcion.getFinPeriodoActual())
            .limites(obtenerLimitesPlan(suscripcion.getPlan()))
            .build();
    }
    
    public Map<String, Object> obtenerEstadoSuscripcion(String usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();
        
        Optional<UsuarioSuscripcion> suscripcionOpt = suscripcionesMock.stream()
            .filter(s -> s.getUsuarioId().equals(usuarioId))
            .findFirst();
        
        if (suscripcionOpt.isPresent()) {
            UsuarioSuscripcion suscripcion = suscripcionOpt.get();
            
            respuesta.put("usuario_id", usuarioId);
            respuesta.put("suscripcion", suscripcion);
            respuesta.put("plan", suscripcion.getPlan());
            respuesta.put("limites", obtenerLimitesPlan(suscripcion.getPlan()));
            respuesta.put("es_premium", "premium".equalsIgnoreCase(suscripcion.getPlan().getNombre()) && 
                          suscripcion.getEstado() == EstadoSuscripcion.ACTIVA);
            
            // Agregar historial de pagos si existe
            if (pagosMock.containsKey(usuarioId)) {
                respuesta.put("historial_pagos", pagosMock.get(usuarioId));
            }
        } else {
            PlanSuscripcion planGratuito = planesMock.get(0);
            respuesta.put("usuario_id", usuarioId);
            respuesta.put("suscripcion", null);
            respuesta.put("plan", planGratuito);
            respuesta.put("limites", obtenerLimitesPlan(planGratuito));
            respuesta.put("es_premium", false);
        }
        
        return respuesta;
    }
    
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
    
    public Map<String, Object> obtenerDatosMock() {
        Map<String, Object> datos = new HashMap<>();
        
        datos.put("planes", planesMock);
        datos.put("suscripciones", suscripcionesMock);
        datos.put("pagos", pagosMock);
        
        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("total_usuarios", 2);
        estadisticas.put("premium_activos", 
            suscripcionesMock.stream()
                .filter(s -> "premium".equalsIgnoreCase(s.getPlan().getNombre()) && 
                           s.getEstado() == EstadoSuscripcion.ACTIVA)
                .count());
        estadisticas.put("ingresos_totales", new BigDecimal("29900.00"));
        
        datos.put("estadisticas", estadisticas);
        
        return datos;
    }
    
    public List<PlanSuscripcion> obtenerPlanes() {
        return new ArrayList<>(planesMock);
    }
}