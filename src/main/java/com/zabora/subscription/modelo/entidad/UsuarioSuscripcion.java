package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FIX BUG-5: Cambiado @OneToMany(mappedBy="suscripcionId") a @JoinColumn.
 *            mappedBy requiere un campo @ManyToOne en Pago, pero suscripcionId
 *            es un String simple. Con @JoinColumn JPA maneja la FK directamente.
 *
 * FIX BUG-7: Eliminados todos los getters/setters manuales — @Data los genera.
 *
 * FIX: Inicializacion movida a @PrePersist para ser idiomatico con JPA.
 */
@Entity
@Table(name = "suscripciones_usuarios")
@Data
public class UsuarioSuscripcion {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private Integer usuarioId;

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

    @Column(name = "id_cliente", length = 255)
    private String idCliente;

    @Column(name = "id_suscripcion", length = 255)
    private String idSuscripcion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // FIX BUG-5: Usar @JoinColumn en vez de mappedBy="suscripcionId"
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id")
    private List<Pago> pagos = new ArrayList<>();

    public UsuarioSuscripcion() {
        // No-arg constructor requerido por JPA
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.fechaActualizacion == null) {
            this.fechaActualizacion = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
