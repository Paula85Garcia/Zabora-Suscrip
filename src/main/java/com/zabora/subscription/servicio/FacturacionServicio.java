package com.zabora.subscription.servicio;

import com.zabora.subscription.excepcion.RecursoNoEncontradoException;
import com.zabora.subscription.modelo.entidad.Factura;
import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.enumeracion.EstadoFactura;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.repositorio.FacturaRepository;
import com.zabora.subscription.repositorio.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturacionServicio {
    
    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * EXPLICACIÓN:
     * Es como cuando compras algo en Amazon y te dan una factura.
     * Esta es tu prueba de que pagaste.
     * 
     * 1. Verifica que el pago existe y está completado
     * 2. Calcula el IVA (19% en Colombia)
     * 3. Genera un número de factura único
     * 4. Guarda la factura en la base de datos
     */
    @Transactional
    public Factura generarFactura(String pagoId) {
        log.info("📄 Generando factura para pago: {}", pagoId);
        
        // 1. Verificar que el pago existe y está completado
        Pago pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado"));
        
        if (pago.getEstado() != EstadoPago.COMPLETADO) {
            throw new IllegalStateException("Solo se pueden facturar pagos completados");
        }
        
        // 2. Verificar si ya existe factura para este pago
        facturaRepository.findByPagoId(pagoId).ifPresent(f -> {
            throw new IllegalStateException("Ya existe una factura para este pago");
        });
        
        // 3. Obtener consecutivo de factura usando el procedimiento almacenado
        Long consecutivo = obtenerSiguienteConsecutivo();
        
        // 4. Calcular montos (IVA 19% incluido en el total)
        BigDecimal total = pago.getMonto();
        BigDecimal iva = total.multiply(new BigDecimal("0.19"))
            .divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.subtract(iva);
        
        // 5. Crear factura
        Factura factura = new Factura();
        factura.setPago(pago);
        factura.setUsuarioId(pago.getUsuarioId());
        factura.setPrefijo("FZ");
        factura.setConsecutivo(consecutivo);
        factura.setFechaEmision(LocalDate.now());
        factura.setFechaVencimiento(LocalDate.now().plusDays(30));
        factura.setSubtotal(subtotal);
        factura.setIva(iva);
        factura.setTotal(total);
        factura.setEstado(EstadoFactura.EMITIDA);
        
        // 6. Generar CUFE (Código Único de Factura Electrónica) - simulado
        factura.setCufe(generarCUFE(factura));
        
        // 7. URLs de documentos (en producción, estos serían PDFs/XMLs reales)
        //Esta opcion aun esta en consideracion segun tiempo de desarrollo
        factura.setPdfUrl("https://zabora.com/facturas/" + consecutivo + ".pdf");
        factura.setXmlUrl("https://zabora.com/facturas/" + consecutivo + ".xml");
        
        // 8. Guardar factura
        facturaRepository.save(factura);
        
        log.info("Factura generada: FZ-{}", consecutivo);
        
        return factura;
    }
    
    /**
     * Obtener factura por ID de pago
     */
    @Transactional(readOnly = true)
    public Factura obtenerFacturaPorPago(String pagoId) {
        return facturaRepository.findByPagoId(pagoId)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe factura para este pago"));
    }
    
    /**
     * Obtener facturas de un usuario
     */
    @Transactional(readOnly = true)
    public List<Factura> obtenerFacturasUsuario(String usuarioId) {
        return facturaRepository.findByUsuarioIdOrderByFechaEmisionDesc(usuarioId);
    }
    
    /**
     * Obtener factura por número
     */
    @Transactional(readOnly = true)
    public Factura obtenerFacturaPorNumero(String numeroFactura) {
        return facturaRepository.findByNumeroFactura(numeroFactura)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Factura no encontrada: " + numeroFactura));
    }
    
    /**
     * Anular factura (solo en primeras 24 horas)
     */
    @Transactional
    public Factura anularFactura(String facturaId, String motivo) {
        log.info("🚫 Anulando factura: {}", facturaId);
        
        Factura factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));
        
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new IllegalStateException("La factura ya está anulada");
        }
        
        // Verificar que no hayan pasado más de 24 horas
        if (factura.getFechaEmision().isBefore(LocalDate.now().minusDays(1))) {
            throw new IllegalStateException("Solo se pueden anular facturas de menos de 24 horas");
        }
        
        factura.setEstado(EstadoFactura.ANULADA);
        
        // En producción, aquí se notificaría a la DIAN
        Map<String, Object> respuestaDian = Map.of(
            "estado", "anulada",
            "motivo", motivo,
            "fecha_anulacion", LocalDate.now().toString()
        );
        
        factura.setRespuestaDian(respuestaDian.toString());
        
        facturaRepository.save(factura);
        
        log.info("✅ Factura anulada: {}", facturaId);
        
        return factura;
    }
    
    // ========== MÉTODOS AUXILIARES ==========
    
    /**
     * Obtener siguiente consecutivo de factura usando la tabla secuencia_facturas
     */
    private Long obtenerSiguienteConsecutivo() {
        jdbcTemplate.update("UPDATE secuencia_facturas SET consecutivo = consecutivo + 1 WHERE id = 1");
        
        Long consecutivo = jdbcTemplate.queryForObject(
            "SELECT consecutivo FROM secuencia_facturas WHERE id = 1",
            Long.class
        );
        
        return consecutivo;
    }
    
    /**
     * Generar CUFE (Código Único de Factura Electrónica)
     * En producción, esto seguiría el estándar de la DIAN
     */
    private String generarCUFE(Factura factura) {
        String base = String.format("%s-%d-%s-%s",
            factura.getPrefijo(),
            factura.getConsecutivo(),
            factura.getUsuarioId(),
            factura.getFechaEmision()
        );
        
        // En producción, esto sería un hash SHA-384 según normas DIAN
        return "CUFE-" + java.util.UUID.nameUUIDFromBytes(base.getBytes()).toString();
    }
}
