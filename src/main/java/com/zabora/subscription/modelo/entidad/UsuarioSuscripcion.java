package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * Entidad que representa la suscripción de un usuario a un plan específico.
 * Contiene información sobre el estado de la suscripción, periodos de vigencia y referencias a Stripe.
 */
@Entity
@Table(name = "suscripciones_usuarios")
@Data
public class UsuarioSuscripcion {
    
	//Identificador único de la suscripción del usuario (UUID).
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    //Identificador del usuario que posee esta suscripción.
    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;
    
    //Plan de suscripción al que está suscrito el usuario.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanSuscripcion plan;
    
    //Estado actual de la suscripción (ACTIVA, CANCELADA, EXPIRADA, PENDIENTE_PAGO).
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoSuscripcion estado = EstadoSuscripcion.PENDIENTE_PAGO;
    
    //Fecha de inicio del periodo de suscripción actual.
    @Column(name = "inicio_periodo_actual")
    private LocalDateTime inicioPeriodoActual;
    
    //Fecha de fin del periodo de suscripción actual.
    @Column(name = "fin_periodo_actual")
    private LocalDateTime finPeriodoActual;
    
    //Indica si la suscripción debe cancelarse automáticamente al final del periodo actual.
    @Column(name = "cancelar_al_final_periodo")
    private Boolean cancelarAlFinalPeriodo = false;
    
    //Fecha en que se solicitó la cancelación de la suscripción, si aplica.
    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;
    
    //ID del cliente en Stripe, si se utiliza Stripe para el pago.
    @Column(name = "id_cliente_stripe")
    private String idClienteStripe;
    
    //ID de la suscripción en Stripe, si se utiliza Stripe para el pago.
    @Column(name = "id_suscripcion_stripe")
    private String idSuscripcionStripe;
    
    //Fecha y hora de creación del registro de la suscripción.
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    //Fecha y hora de la última actualización del registro de la suscripción.
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
}