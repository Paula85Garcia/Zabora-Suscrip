package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "suscripciones_usuarios")
@Data
public class UsuarioSuscripcion {
    
    @Id
    @Column(name = "id", length = 100)
    private String id;
    
    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanSuscripcion plan;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoSuscripcion estado = EstadoSuscripcion.PENDIENTE_PAGO;
    
    @Column(name = "inicio_periodo_actual")
    private LocalDateTime inicioPeriodoActual;
    
    @Column(name = "fin_periodo_actual")
    private LocalDateTime finPeriodoActual;
    
    @Column(name = "cancelar_al_final_periodo")
    private Boolean cancelarAlFinalPeriodo = false;
    
    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;
    
    @Column(name = "id_cliente_stripe", length = 255)
    private String idClienteStripe;
    
    @Column(name = "id_suscripcion_stripe", length = 255)
    private String idSuscripcionStripe;
    
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
    
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
    
    // Constructor para inicializacion
    public UsuarioSuscripcion() {
        this.id = java.util.UUID.randomUUID().toString();
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }
}