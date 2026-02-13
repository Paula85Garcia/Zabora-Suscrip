package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.TipoMetodoPago;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Pago
 * Corregida para compatibilidad con MariaDB
 */
@Entity
@Table(name = "pagos")
@Data
public class Pago {
    
    @Id
    @Column(name = "id", length = 100)
    private String id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private UsuarioSuscripcion suscripcion;
    
    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;
    
    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
    
    @Column(name = "moneda", length = 3)
    private String moneda = "COP";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private TipoMetodoPago metodoPago;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPago estado = EstadoPago.PENDIENTE;
    
    @Column(name = "id_intento_pago_stripe", nullable = false, length = 255, unique = true)
    private String idIntentoPagoStripe;
    
    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;
    
    @Column(name = "url_comprobante", length = 500)
    private String urlComprobante;
    
    @Column(name = "codigo_autorizacion", length = 50)
    private String codigoAutorizacion;
    
    @Column(name = "estado_pse", length = 50)
    private String estadoPse;
    
    @Column(name = "referencia_pse", length = 100)
    private String referenciaPse;
    
    // CORRECCION: Usar LONGTEXT en lugar de JSON para MariaDB
    @Column(name = "metadatos", columnDefinition = "LONGTEXT")
    private String metadatos;
    
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
    
    // Constructor para inicializacion
    public Pago() {
        this.id = java.util.UUID.randomUUID().toString();
        this.fechaCreacion = LocalDateTime.now();
    }
}