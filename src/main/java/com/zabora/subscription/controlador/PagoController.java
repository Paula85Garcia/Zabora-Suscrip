package com.zabora.subscription.controlador;

import com.zabora.subscription.modelo.dto.CrearPagoRequest;
import com.zabora.subscription.modelo.dto.CrearPagoResponse;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.entidad.UsuarioSuscripcion;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.repositorio.PagoRepositorio;
import com.zabora.subscription.repositorio.UsuarioSuscripcionRepositorio;
import com.zabora.subscription.servicio.MercadoPagoServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final MercadoPagoServicio mercadoPagoServicio;
    private final PagoRepositorio pagoRepositorio;
    private final UsuarioSuscripcionRepositorio suscripcionRepositorio;

    @PostMapping("/crear-preferencia")
    public ResponseEntity<CrearPagoResponse> crearPreferenciaPago(@Valid @RequestBody CrearPagoRequest request) {
        try {
            log.info("Creando preferencia de pago MercadoPago");
            log.info("Suscripcion ID: {}", request.getIdSuscripcion());
            log.info("Monto: {} COP", request.getMonto());

            UsuarioSuscripcion suscripcion = suscripcionRepositorio
                    .findById(request.getIdSuscripcion())
                    .orElseThrow(() -> new RuntimeException("Suscripcion no encontrada"));

            boolean existePagoPendiente = pagoRepositorio
                    .existsBySuscripcionIdAndEstado(request.getIdSuscripcion(), EstadoPago.PENDIENTE);

            if (existePagoPendiente) {
                throw new RuntimeException("Ya existe un pago pendiente para esta suscripcion");
            }

            CrearPagoResponse response = mercadoPagoServicio.crearPreferenciaPago(request);

            Pago pago = new Pago();
            pago.setId(UUID.randomUUID().toString());
            pago.setSuscripcionId(request.getIdSuscripcion());
            pago.setUsuarioId(suscripcion.getUsuarioId());
            pago.setMonto(request.getMonto());
            pago.setMoneda("COP");
            pago.setMetodoPago(mapearTipoPago(request.getTipoPago()));
            pago.setEstado(EstadoPago.PENDIENTE);
            pago.setIdIntentoPago(response.getPreferenceId());

            pagoRepositorio.save(pago);

            response.setSubscriptionId(request.getIdSuscripcion());
            response.setPaymentId(pago.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creando preferencia de pago: {}", e.getMessage(), e);
            throw new RuntimeException("Error creando preferencia de pago: " + e.getMessage());
        }
    }

    @GetMapping("/public-key")
    public ResponseEntity<String> obtenerPublicKey() {
        return ResponseEntity.ok(mercadoPagoServicio.getPublicKey());
    }

    private String mapearTipoPago(String tipoPago) {
        if ("tarjeta_credito".equalsIgnoreCase(tipoPago)) {
            return "TARJETA_CREDITO";
        } else if ("pse".equalsIgnoreCase(tipoPago)) {
            return "PSE";
        }
        return "TARJETA_CREDITO";
    }
}