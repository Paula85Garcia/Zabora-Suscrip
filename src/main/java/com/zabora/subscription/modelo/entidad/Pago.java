package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa un pago realizado por un usuario
 */
@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "suscripcion_id", length = 100, nullable = false)
    private String suscripcionId;

    @Column(name = "usuario_id", length = 100, nullable = false)
    private String usuarioId;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda = "COP";

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;  // "TARJETA_CREDITO" o "PSE"

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(name = "id_intento_pago", unique = true, length = 255)
    private String idIntentoPago;  // Preference ID de MercadoPago

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

    @Column(name = "metadatos", columnDefinition = "LONGTEXT")
    private String metadatos;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}