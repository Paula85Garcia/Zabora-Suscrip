package com.zabora.subscription.servicio;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.zabora.subscription.excepcion.PagoException;
import com.zabora.subscription.excepcion.RecursoNoEncontradoException;
import com.zabora.subscription.modelo.dto.RespuestaPagoDTO;
import com.zabora.subscription.modelo.dto.SolicitudPagoDTO;
import com.zabora.subscription.modelo.entidad.LogSuscripcion;
import com.zabora.subscription.modelo.entidad.MetodoPago;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.AccionLog;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.TipoCuentaBanco;
import com.zabora.subscription.modelo.enumeracion.TipoMetodoPago;
import com.zabora.subscription.repositorio.LogSuscripcionRepository;
import com.zabora.subscription.repositorio.MetodoPagoRepository;
import com.zabora.subscription.repositorio.PagoRepository;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoServicio {

    private final PagoRepository pagoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioSuscripcionRepository suscripcionRepository;
    private final LogSuscripcionRepository logRepository;
    private final SuscripcionServicio suscripcionServicio;

    /**
     * EXPLICACIÓN:
     * Es como pagar tu suscripción de Spotify con tu tarjeta.
     * 1. Verificas que la suscripción existe
     * 2. Procesas el pago (simulado o real con Stripe)
     * 3. Si el pago es exitoso, activas la suscripción
     * 4. Guardas el recibo (comprobante)
     */
    @Transactional
    public RespuestaPagoDTO procesarPago(SolicitudPagoDTO solicitud) {
        log.info("Procesando pago para suscripción: {}", solicitud.getIdSuscripcion());

        // 1. Verificar que la suscripción existe
        UsuarioSuscripcion suscripcion = suscripcionRepository.findById(solicitud.getIdSuscripcion())
                .orElseThrow(() -> new RecursoNoEncontradoException("Suscripción no encontrada"));

        // 2. Crear registro de pago
        Pago pago = new Pago();
        pago.setId(UUID.randomUUID().toString());
        pago.setSuscripcion(suscripcion);
        pago.setUsuarioId(suscripcion.getUsuarioId());
        pago.setMonto(solicitud.getMonto());
        pago.setMoneda("COP");
        pago.setMetodoPago(TipoMetodoPago.valueOf(solicitud.getTipoPago().toUpperCase()));
        pago.setEstado(EstadoPago.PENDIENTE);

        try {
            RespuestaPagoDTO respuesta;

            // 3. Procesar según el modo (prueba o producción)
            if (solicitud.getTokenTarjetaPrueba() != null) {
                // Modo de prueba con diferentes escenarios
                respuesta = procesarPagoPrueba(pago, solicitud);
            } else {
                // Modo simulado para desarrollo (sin Stripe real)
                respuesta = procesarPagoSimulado(pago, solicitud);
            }

            // 4. Si el pago fue exitoso, activar suscripción
            if (respuesta.getExito() && "COMPLETADO".equals(respuesta.getEstado())) {
                pago.setEstado(EstadoPago.COMPLETADO);
                pago.setFechaPago(LocalDateTime.now());

                // Activar suscripción premium
                suscripcionServicio.activarSuscripcionPremium(
                        suscripcion.getId(),
                        "stripe_sub_" + System.currentTimeMillis()
                );

                // Registrar log de pago exitoso
                registrarLogPago(suscripcion.getId(), suscripcion.getUsuarioId(),
                        AccionLog.PAGO_EXITOSO, "Pago completado exitosamente");

            } else {
                pago.setEstado(EstadoPago.FALLIDO);

                // Registrar log de pago fallido
                registrarLogPago(suscripcion.getId(), suscripcion.getUsuarioId(),
                        AccionLog.PAGO_FALLIDO, "Pago rechazado: " + respuesta.getMensaje());
            }

            // 5. Guardar registro de pago
            pagoRepository.save(pago);

            // 6. Actualizar respuesta con ID de pago
            respuesta.setIdPago(pago.getId());

            log.info("Pago procesado: {} - Estado: {}", pago.getId(), pago.getEstado());

            return respuesta;

        } catch (Exception e) {
            log.error("Error procesando pago: {}", e.getMessage(), e);

            pago.setEstado(EstadoPago.FALLIDO);
            pagoRepository.save(pago);

            throw new PagoException("Error al procesar el pago: " + e.getMessage());
        }
    }

    /**
     * Procesar pago en modo PRUEBA
     * Simula diferentes escenarios según el token
     */
    private RespuestaPagoDTO procesarPagoPrueba(Pago pago, SolicitudPagoDTO solicitud) {
        log.info("Procesando pago de prueba con token: {}", solicitud.getTokenTarjetaPrueba());

        String token = solicitud.getTokenTarjetaPrueba();
        boolean exito;
        String estado;
        String mensaje;

        // Simular diferentes escenarios
        if (token.contains("fail") || token.contains("decline")) {
            exito = false;
            estado = "FALLIDO";
            mensaje = "Pago rechazado - Tarjeta declinada";

        } else if (token.contains("insufficient")) {
            exito = false;
            estado = "FALLIDO";
            mensaje = "Pago rechazado - Fondos insuficientes";

        } else if (token.contains("expired")) {
            exito = false;
            estado = "FALLIDO";
            mensaje = "Pago rechazado - Tarjeta expirada";

        } else if (token.contains("3ds") || token.contains("authentication")) {
            exito = false;
            estado = "REQUIERE_AUTENTICACION";
            mensaje = "Requiere autenticación 3D Secure";

        } else {
            // Token válido - pago exitoso
            exito = true;
            estado = "COMPLETADO";
            mensaje = "Pago procesado exitosamente";
        }

        String intentId = "pi_test_" + System.currentTimeMillis();
        pago.setIdIntentoPagoStripe(intentId);

        return RespuestaPagoDTO.builder()
                .exito(exito)
                .mensaje(mensaje)
                .idPago(pago.getId())
                .estado(estado)
                .monto(solicitud.getMonto())
                .moneda("COP")
                .fechaPago(exito ? LocalDateTime.now() : null)
                .urlComprobante(exito ? "https://receipt.stripe.com/test/" + intentId : null)
                .requiereConfirmacion(estado.equals("REQUIERE_AUTENTICACION"))
                .build();
    }

    /**
     * Procesar pago SIMULADO para desarrollo
     * No conecta con Stripe real
     */
    private RespuestaPagoDTO procesarPagoSimulado(Pago pago, SolicitudPagoDTO solicitud) {
        log.info("Procesando pago simulado para desarrollo");

        String intentId = "pi_sim_" + System.currentTimeMillis();
        pago.setIdIntentoPagoStripe(intentId);

        boolean requiereConfirmacion = solicitud.getTipoPago().equals("PSE");
        String estado = requiereConfirmacion ? "PENDIENTE" : "COMPLETADO";
        String mensaje = requiereConfirmacion
                ? "Pago PSE creado, requiere confirmación en el banco"
                : "Pago con tarjeta procesado exitosamente";

        Map<String, Object> detalles = new HashMap<>();
        detalles.put("id_intent", intentId);
        detalles.put("tipo_pago", solicitud.getTipoPago());
        detalles.put("simulado", true);

        return RespuestaPagoDTO.builder()
                .exito(true)
                .mensaje(mensaje)
                .idPago(pago.getId())
                .estado(estado)
                .monto(solicitud.getMonto())
                .moneda("COP")
                .fechaPago(LocalDateTime.now())
                .urlComprobante("https://zabora.com/receipt/" + intentId)
                .requiereConfirmacion(requiereConfirmacion)
                .detalles(detalles)
                .build();
    }

    /**
     * Obtener métodos de pago del usuario
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerMetodosPago(String usuarioId) {
        List<MetodoPago> metodos = metodoPagoRepository.findByUsuarioIdAndActivoTrue(usuarioId);
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (MetodoPago metodo : metodos) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", metodo.getId());
            item.put("tipo", metodo.getTipo().name());
            item.put("predeterminado", metodo.getPredeterminado());

            if (metodo.getTipo() == TipoMetodoPago.TARJETA_CREDITO) {
                item.put("ultimos_cuatro", metodo.getUltimosCuatro());
                item.put("marca", metodo.getMarca());
                item.put("expira_mes", metodo.getExpiraMes());
                item.put("expira_anio", metodo.getExpiraAnio());
            } else {
                item.put("banco", metodo.getBanco());
                item.put("tipo_cuenta", metodo.getTipoCuenta());
            }

            resultado.add(item);
        }

        return resultado;
    }

    /**
     * Agregar método de pago
     */
    @Transactional
    public Map<String, Object> agregarMetodoPago(String usuarioId, Map<String, Object> datosMetodo) {
        log.info("Agregando método de pago para usuario: {}", usuarioId);

        MetodoPago metodo = new MetodoPago();
        metodo.setId(UUID.randomUUID().toString());
        metodo.setUsuarioId(usuarioId);
        metodo.setTipo(TipoMetodoPago.valueOf((String) datosMetodo.get("tipo")));
        metodo.setIdMetodoPagoStripe("pm_mock_" + System.currentTimeMillis());

        if (metodo.getTipo() == TipoMetodoPago.TARJETA_CREDITO) {
            metodo.setUltimosCuatro((String) datosMetodo.get("ultimos_cuatro"));
            metodo.setMarca((String) datosMetodo.get("marca"));
            metodo.setExpiraMes((Integer) datosMetodo.get("expira_mes"));
            metodo.setExpiraAnio((Integer) datosMetodo.get("expira_anio"));
        } else {
            metodo.setBanco((String) datosMetodo.get("banco"));
            metodo.setTipoCuenta(TipoCuentaBanco.valueOf((String) datosMetodo.get("tipo_cuenta")));
        }

        // Si es el primer método, hacerlo predeterminado
        List<MetodoPago> metodosExistentes = metodoPagoRepository.findByUsuarioIdAndActivoTrue(usuarioId);
        if (metodosExistentes.isEmpty()) {
            metodo.setPredeterminado(true);
        }

        metodoPagoRepository.save(metodo);

        log.info("Método de pago agregado: {}", metodo.getId());

        return Map.of(
                "exito", true,
                "mensaje", "Método de pago agregado exitosamente",
                "id_metodo", metodo.getId()
        );
    }

    /**
     * Eliminar método de pago
     */
    @Transactional
    public Map<String, Object> eliminarMetodoPago(String usuarioId, String idMetodo) {
        log.info("Eliminando método de pago: {}", idMetodo);

        MetodoPago metodo = metodoPagoRepository.findById(idMetodo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Método de pago no encontrado"));

        if (!metodo.getUsuarioId().equals(usuarioId)) {
            throw new PagoException("No tienes permiso para eliminar este método de pago");
        }

        metodo.setActivo(false);
        metodoPagoRepository.save(metodo);

        return Map.of(
                "exito", true,
                "mensaje", "Método de pago eliminado exitosamente"
        );
    }

    /**
     * Obtener historial de pagos
     */
    @Transactional(readOnly = true)
    public List<Pago> obtenerHistorialPagos(String usuarioId) {
        return pagoRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    // ========== MÉTODOS AUXILIARES ==========

    private void registrarLogPago(String suscripcionId, String usuarioId,
                                  AccionLog accion, String descripcion) {
        LogSuscripcion log = new LogSuscripcion();
        log.setSuscripcionId(suscripcionId);
        log.setUsuarioId(usuarioId);
        log.setAccion(accion);
        log.setDescripcion(descripcion);
        log.setRealizadoPor("sistema");

        logRepository.save(log);
    }
}
