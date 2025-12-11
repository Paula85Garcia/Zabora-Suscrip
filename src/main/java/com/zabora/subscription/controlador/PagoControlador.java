package com.zabora.subscription.controlador;

import com.zabora.subscription.modelo.dto.RespuestaPagoDTO;
import com.zabora.subscription.modelo.dto.SolicitudPagoDTO;
import com.zabora.subscription.servicio.PagoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para la gestión de pagos.
 * Permite procesar pagos, gestionar métodos de pago del usuario y realizar pagos de prueba.
 */
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Endpoints para gestión de pagos")
public class PagoControlador {

    private final PagoServicio pagoServicio;

    /**
     * Procesa un pago manual para una suscripción.
     *
     * @param usuarioId ID del usuario que realiza el pago (header X-Usuario-Id)
     * @param solicitud Datos del pago a procesar
     * @return Respuesta con el estado del pago, monto, comprobante y detalles
     */
    @PostMapping("/procesar")
    @Operation(summary = "Procesar pago manual")
    public ResponseEntity<RespuestaPagoDTO> procesarPago(
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @Valid @RequestBody SolicitudPagoDTO solicitud) {
        RespuestaPagoDTO resultado = pagoServicio.procesarPago(solicitud);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Obtiene los métodos de pago asociados a un usuario.
     *
     * @param usuarioId ID del usuario (header X-Usuario-Id)
     * @return Lista de métodos de pago activos y detalles básicos
     */
    @GetMapping("/metodos")
    @Operation(summary = "Obtener métodos de pago del usuario")
    public ResponseEntity<List<Map<String, Object>>> obtenerMetodosPago(
            @RequestHeader("X-Usuario-Id") String usuarioId) {
        List<Map<String, Object>> metodos = pagoServicio.obtenerMetodosPago(usuarioId);
        return ResponseEntity.ok(metodos);
    }

    /**
     * Agrega un nuevo método de pago para un usuario.
     *
     * @param usuarioId ID del usuario (header X-Usuario-Id)
     * @param datosMetodo Información del método de pago a agregar
     * @return Resultado de la operación con detalles del método agregado
     */
    @PostMapping("/metodos")
    @Operation(summary = "Agregar método de pago")
    public ResponseEntity<Map<String, Object>> agregarMetodoPago(
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @RequestBody Map<String, Object> datosMetodo) {
        Map<String, Object> resultado = pagoServicio.agregarMetodoPago(usuarioId, datosMetodo);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Elimina un método de pago asociado a un usuario.
     *
     * @param usuarioId ID del usuario (header X-Usuario-Id)
     * @param idMetodo ID del método de pago a eliminar
     * @return Resultado de la operación
     */
    @DeleteMapping("/metodos/{idMetodo}")
    @Operation(summary = "Eliminar método de pago")
    public ResponseEntity<Map<String, Object>> eliminarMetodoPago(
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @PathVariable String idMetodo) {
        Map<String, Object> resultado = pagoServicio.eliminarMetodoPago(usuarioId, idMetodo);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Procesa un pago de prueba para simular diferentes escenarios.
     * Útil para testing con tokens de tarjeta o pagos PSE simulados.
     *
     * @param solicitudPrueba Mapa con información de la prueba (id_suscripcion, monto, tipo_pago, token_prueba)
     * @return Respuesta simulada del pago
     */
    @PostMapping("/pago-prueba")
    @Operation(summary = "Procesar pago de prueba con diferentes escenarios")
    public ResponseEntity<RespuestaPagoDTO> procesarPagoPrueba(
            @RequestBody Map<String, Object> solicitudPrueba) {

        SolicitudPagoDTO solicitud = new SolicitudPagoDTO();
        solicitud.setIdSuscripcion((String) solicitudPrueba.get("id_suscripcion"));
        solicitud.setMonto(new java.math.BigDecimal(solicitudPrueba.get("monto").toString()));
        solicitud.setTipoPago((String) solicitudPrueba.get("tipo_pago"));
        solicitud.setTokenTarjetaPrueba((String) solicitudPrueba.get("token_prueba"));

        RespuestaPagoDTO resultado = pagoServicio.procesarPago(solicitud);
        return ResponseEntity.ok(resultado);
    }
}
