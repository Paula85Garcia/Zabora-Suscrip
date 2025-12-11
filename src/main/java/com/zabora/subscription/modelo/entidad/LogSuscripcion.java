package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.AccionLog;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * Entidad que representa un registro de log relacionado con una suscripción.
 * 
 * Permite auditar acciones importantes realizadas sobre suscripciones y sus pagos.
 */
@Entity
@Table(name = "logs_suscripciones")
@Data
public class LogSuscripcion {
    // Identificador único del log (autoincremental).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    
    //ID de la suscripción asociada al log.
    @Column(name = "suscripcion_id", nullable = false, length = 36)
    private String suscripcionId;
    
    
    //ID del usuario al que pertenece la suscripción.
    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;
    
    
    //Acción realizada sobre la suscripción (CREACION, ACTIVACION, CANCELACION, etc.).
    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false)
    private AccionLog accion;
    
    
    // Estado anterior de la suscripción antes de la acción.
    @Column(name = "estado_anterior", length = 50)
    private String estadoAnterior;
    
    
    //Estado nuevo de la suscripción después de la acción.
    @Column(name = "estado_nuevo", length = 50)
    private String estadoNuevo;
    
    
    //Descripción adicional de la acción realizada.
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
    
    
    //ID del usuario que realizó la acción (puede ser el sistema o un usuario).
    @Column(name = "realizado_por", nullable = false, length = 36)
    private String realizadoPor;
    
    
    //Dirección IP desde la cual se realizó la acción.
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    
    //Información del user agent del navegador o sistema que realizó la acción.
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    
    //Fecha y hora de creación del registro de log.
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    
    /**
     * Acción ejecutada antes de insertar el registro en la base de datos.
     * Se asegura de actualizar la fecha de creación.
     */
    @PrePersist
    public void prePersist() {
        fechaCreacion = LocalDateTime.now();
    }
}
