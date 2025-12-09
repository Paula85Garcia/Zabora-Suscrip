package com.zabora.subscription.servicio;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.zabora.subscription.modelo.dto.RespuestaPagoDTO;
import com.zabora.subscription.modelo.dto.SolicitudPagoDTO;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.TipoMetodoPago;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class PagoServicio {
    
   
    private String stripeClaveSecreta;
    
    private final Map<String, List<Map<String, Object>>> metodosPagoMock;
    private final Map<String, List<Map<String, Object>>> historialPagosMock;
    
    public PagoServicio() {
        this.metodosPagoMock = new HashMap<>();
        this.historialPagosMock = new HashMap<>();
    }
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeClaveSecreta;
        inicializarDatosPagoMock();
    }
    
    private void inicializarDatosPagoMock() {
        // Métodos de pago mock para usuario_001
        List<Map<String, Object>> metodosUsuario1 = new ArrayList<>();
        
        Map<String, Object> tarjeta1 = new HashMap<>();
        tarjeta1.put("id", "pm_001");
        tarjeta1.put("tipo", "TARJETA_CREDITO");
        tarjeta1.put("ultimos_cuatro", "4242");
        tarjeta1.put("marca", "visa");
        tarjeta1.put("predeterminado", true);
        tarjeta1.put("expira_mes", 12);
        tarjeta1.put("expira_anio", 2025);
        metodosUsuario1.add(tarjeta1);
        
        Map<String, Object> pse1 = new HashMap<>();
        pse1.put("id", "pm_002");
        pse1.put("tipo", "PSE");
        pse1.put("banco", "Bancolombia");
        pse1.put("tipo_cuenta", "ahorros");
        pse1.put("predeterminado", false);
        metodosUsuario1.add(pse1);
        
        metodosPagoMock.put("usuario_001", metodosUsuario1);
        
        log.info("✅ Datos de pago mock inicializados");
    }
    
    public RespuestaPagoDTO procesarPago(SolicitudPagoDTO solicitud) {
        log.info("💳 Procesando pago para suscripción: {}", solicitud.getIdSuscripcion());
        
        try {
            RespuestaPagoDTO respuesta;
            
            if (solicitud.getTokenTarjetaPrueba() != null) {
                // Modo de prueba con token
                respuesta = procesarPagoPrueba(solicitud);
            } else {
                // Simulación de pago normal
                respuesta = procesarPagoSimulado(solicitud);
            }
            
            // Guardar en historial mock
            guardarPagoMock(solicitud, respuesta);
            
            return respuesta;
            
        } catch (Exception e) {
            log.error("❌ Error procesando pago: {}", e.getMessage());
            throw new RuntimeException("Error al procesar el pago: " + e.getMessage());
        }
    }
    
    private RespuestaPagoDTO procesarPagoPrueba(SolicitudPagoDTO solicitud) {
        log.info("🧪 Procesando pago de prueba con token: {}", solicitud.getTokenTarjetaPrueba());
        
        // Simular diferentes escenarios basados en el token
        String token = solicitud.getTokenTarjetaPrueba();
        boolean exito = true;
        String estado = "COMPLETADO";
        String mensaje = "Pago de prueba exitoso";
        
        if (token.contains("fail") || token.contains("chargeDeclined")) {
            exito = false;
            estado = "FALLIDO";
            mensaje = "Pago fallido - tarjeta declinada";
        } else if (token.contains("3ds")) {
            estado = "REQUIERE_AUTENTICACION";
            mensaje = "Requiere autenticación 3D Secure";
        } else if (token.contains("pending")) {
            estado = "PENDIENTE";
            mensaje = "Pago pendiente de confirmación";
        }
        
        String pagoId = "pago_test_" + System.currentTimeMillis();
        
        return RespuestaPagoDTO.builder()
            .exito(exito)
            .mensaje(mensaje)
            .idPago(pagoId)
            .estado(estado)
            .monto(solicitud.getMonto())
            .moneda("COP")
            .fechaPago(LocalDateTime.now())
            .urlComprobante("https://receipt.stripe.com/test/" + pagoId)
            .requiereConfirmacion(estado.equals("REQUIERE_AUTENTICACION"))
            .build();
    }
    
    private RespuestaPagoDTO procesarPagoSimulado(SolicitudPagoDTO solicitud) {
        log.info("Procesando pago simulado para: {}", solicitud.getIdSuscripcion());
        
        // Simular creación de PaymentIntent
        Map<String, Object> detallesPago = new HashMap<>();
        String pagoId = "pi_" + System.currentTimeMillis();
        
        detallesPago.put("id", pagoId);
        detallesPago.put("estado", "REQUIERE_ACCION");
        detallesPago.put("cliente_secreto", "secret_simulado_" + System.currentTimeMillis());
        detallesPago.put("monto", solicitud.getMonto());
        detallesPago.put("moneda", "COP");
        
        boolean requiereConfirmacion = solicitud.getTipoPago().equals("PSE");
        String estadoPago = requiereConfirmacion ? "PENDIENTE" : "COMPLETADO";
        String mensaje = requiereConfirmacion ? 
            "Pago PSE creado, requiere confirmación en el banco" : 
            "Pago con tarjeta procesado exitosamente";
        
        return RespuestaPagoDTO.builder()
            .exito(true)
            .mensaje(mensaje)
            .idPago(pagoId)
            .estado(estadoPago)
            .monto(solicitud.getMonto())
            .moneda("COP")
            .fechaPago(LocalDateTime.now())
            .urlComprobante("https://receipt.stripe.com/simulated/" + pagoId)
            .requiereConfirmacion(requiereConfirmacion)
            .detalles(detallesPago)
            .build();
    }
    
    private void guardarPagoMock(SolicitudPagoDTO solicitud, RespuestaPagoDTO respuestaPago) {
        // Obtener usuarioId de la suscripción (en realidad vendría de la base de datos)
        String usuarioId = "usuario_" + System.currentTimeMillis();
        
        Map<String, Object> pagoRegistro = new HashMap<>();
        pagoRegistro.put("id", respuestaPago.getIdPago());
        pagoRegistro.put("id_suscripcion", solicitud.getIdSuscripcion());
        pagoRegistro.put("monto", solicitud.getMonto());
        pagoRegistro.put("tipo_pago", solicitud.getTipoPago());
        pagoRegistro.put("estado", respuestaPago.getEstado());
        pagoRegistro.put("fecha", new Date());
        pagoRegistro.put("detalles", respuestaPago.getDetalles());
        
        historialPagosMock.computeIfAbsent(usuarioId, k -> new ArrayList<>())
            .add(pagoRegistro);
        
        log.info("Pago guardado en mock: {}", respuestaPago.getIdPago());
    }
    
    public List<Map<String, Object>> obtenerMetodosPago(String usuarioId) {
        return metodosPagoMock.getOrDefault(usuarioId, new ArrayList<>());
    }
    
    public Map<String, Object> agregarMetodoPago(String usuarioId, Map<String, Object> datosMetodo) {
        String tipo = (String) datosMetodo.get("tipo");
        
        Map<String, Object> nuevoMetodo = new HashMap<>();
        String metodoId = "pm_" + System.currentTimeMillis();
        nuevoMetodo.put("id", metodoId);
        nuevoMetodo.put("tipo", tipo);
        nuevoMetodo.put("fecha_creacion", new Date());
        
        if ("TARJETA_CREDITO".equals(tipo)) {
            nuevoMetodo.put("ultimos_cuatro", datosMetodo.get("ultimos_cuatro"));
            nuevoMetodo.put("marca", datosMetodo.get("marca"));
            nuevoMetodo.put("expira_mes", datosMetodo.get("expira_mes"));
            nuevoMetodo.put("expira_anio", datosMetodo.get("expira_anio"));
        } else if ("PSE".equals(tipo)) {
            nuevoMetodo.put("banco", datosMetodo.get("banco"));
            nuevoMetodo.put("tipo_cuenta", datosMetodo.get("tipo_cuenta"));
        }
        
        metodosPagoMock.computeIfAbsent(usuarioId, k -> new ArrayList<>())
            .add(nuevoMetodo);
        
        log.info("Método de pago agregado para usuario {}: {}", usuarioId, metodoId);
        
        return Map.of(
            "exito", true,
            "mensaje", "Método de pago agregado exitosamente",
            "metodo", nuevoMetodo
        );
    }
    
    public Map<String, Object> eliminarMetodoPago(String usuarioId, String idMetodo) {
        List<Map<String, Object>> metodos = metodosPagoMock.get(usuarioId);
        
        if (metodos != null) {
            boolean removido = metodos.removeIf(m -> idMetodo.equals(m.get("id")));
            
            if (removido) {
                log.info("🗑️ Método de pago eliminado: {}", idMetodo);
                return Map.of(
                    "exito", true,
                    "mensaje", "Método de pago eliminado exitosamente"
                );
            }
        }
        
        log.warn(" Método de pago no encontrado: {}", idMetodo);
        throw new RuntimeException("Método de pago no encontrado: " + idMetodo);
    }
    
    // Método para pruebas - obtener todos los datos mock de pagos
    public Map<String, Object> obtenerDatosPagoMock() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("metodos_pago", metodosPagoMock);
        datos.put("historial_pagos", historialPagosMock);
        
        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("total_metodos", 
            metodosPagoMock.values().stream()
                .mapToInt(List::size)
                .sum());
        estadisticas.put("total_pagos", 
            historialPagosMock.values().stream()
                .mapToInt(List::size)
                .sum());
        
        datos.put("estadisticas", estadisticas);
        return datos;
    }
}