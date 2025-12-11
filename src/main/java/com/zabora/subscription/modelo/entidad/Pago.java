package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import com.zabora.subscription.modelo.enumeracion.TipoMetodoPago;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Entidad que representa un pago realizado por un usuario para una suscripción.
 * Contiene información sobre monto, método de pago, estado, y datos de comprobación.
 */
@Entity
@Table(name = "pagos")
@Data
public class Pago {
    //Identificador único del pago (UUID).
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    
    //Suscripción asociada a este pago.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private UsuarioSuscripcion suscripcion;
    
    
    //ID del usuario que realizó el pago.
    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;
    
    
    //Monto del pago.
    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
    
    //Moneda en la que se realizó el pago. Por defecto COP (pesos colombianos).
    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda = "COP";
    
    //Método de pago utilizado (TARJETA_CREDITO o PSE).
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private TipoMetodoPago metodoPago;
    
    //Estado del pago (PENDIENTE, COMPLETADO, FALLIDO, REEMBOLSADO, CANCELADO)
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPago estado = EstadoPago.PENDIENTE;
    
    //ID del intento de pago en Stripe, si aplica.
    @Column(name = "id_intento_pago_stripe")
    private String idIntentoPagoStripe;
    
    
    // Fecha y hora en que se realizó el pago.
    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;
    
    
    //URL del comprobante del pago (puede ser PDF o enlace a sistema externo).
    @Column(name = "url_comprobante")
    private String urlComprobante;
    
    
    //Metadatos adicionales del pago, almacenados en formato JSON.
    @Column(name = "metadatos", columnDefinition = "JSON")
    private String metadatos;
    
    //Fecha y hora de creación del registro de pago
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}