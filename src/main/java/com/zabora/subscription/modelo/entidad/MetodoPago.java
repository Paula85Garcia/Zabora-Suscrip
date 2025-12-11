package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.TipoMetodoPago;
import com.zabora.subscription.modelo.enumeracion.TipoCuentaBanco;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * Entidad que representa un método de pago de un usuario.
 * 
 * Puede ser una tarjeta de crédito o un método PSE (banco en línea). 
 * Contiene información específica según el tipo de método y datos de control.
 */
@Entity
@Table(name = "metodos_pago")
@Data
public class MetodoPago {
    //Identificador único del método de pago (UUID)
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    //ID del usuario al que pertenece este método de pago.
    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;
    
    //Tipo de método de pago (TARJETA_CREDITO o PSE).
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMetodoPago tipo;
    
 // ===== DATOS DE TARJETA =====
    //Últimos 4 dígitos de la tarjeta.
    @Column(name = "ultimos_cuatro", length = 4)
    private String ultimosCuatro;
    
    
   // Marca de la tarjeta (Visa, Mastercard, etc.).
    @Column(name = "marca", length = 50)
    private String marca;
    
    
    //Mes de expiración de la tarjeta.
    @Column(name = "expira_mes")
    private Integer expiraMes;
    
    
    //Año de expiración de la tarjeta.
    @Column(name = "expira_anio")
    private Integer expiraAnio;
    
    // ===== DATOS PSE =====
    //Banco asociado al método PSE.
    @Column(name = "banco", length = 100)
    private String banco;
    
    //Tipo de cuenta bancaria (AHORROS o CORRIENTE) para PSE.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cuenta")
    private TipoCuentaBanco tipoCuenta;
    
    
    //Referencia asociada al pago PSE.
    @Column(name = "referencia_pse", length = 100)
    private String referenciaPse;
    
    
    //ID del método de pago en Stripe (único y obligatorio).
    @Column(name = "id_metodo_pago_stripe", nullable = false, unique = true)
    private String idMetodoPagoStripe;
    
    //Indica si este método de pago es el predeterminado del usuario.
    @Column(name = "predeterminado")
    private Boolean predeterminado = false;
    
    
    //Indica si el método de pago está activo.
    @Column(name = "activo")
    private Boolean activo = true;
    
    
    //Fecha y hora de creación del registro.
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    
    //Fecha y hora de última actualización del registro.
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
    
    
    /**
     * Acción ejecutada antes de insertar el registro en la base de datos.
     * Se asegura de asignar un ID si no existe y actualizar las fechas.
     */
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    /**
     * Acción ejecutada antes de actualizar el registro en la base de datos.
     * Actualiza la fecha de actualización.
     */
    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}