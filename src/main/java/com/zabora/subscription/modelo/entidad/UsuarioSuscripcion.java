package com.zabora.subscription.modelo.entidad;

import com.zabora.subscription.modelo.enumeracion.EstadoSuscripcion;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    
    @OneToMany(mappedBy = "suscripcionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pago> pagos;
    
    // Constructor para inicializacion
    public UsuarioSuscripcion() {
        this.id = java.util.UUID.randomUUID().toString();
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.pagos = new ArrayList<>();
    }
    
    // Getters y Setters manuales
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    
    public PlanSuscripcion getPlan() { return plan; }
    public void setPlan(PlanSuscripcion plan) { this.plan = plan; }
    
    public EstadoSuscripcion getEstado() { return estado; }
    public void setEstado(EstadoSuscripcion estado) { this.estado = estado; }
    
    public LocalDateTime getInicioPeriodoActual() { return inicioPeriodoActual; }
    public void setInicioPeriodoActual(LocalDateTime inicioPeriodoActual) { this.inicioPeriodoActual = inicioPeriodoActual; }
    
    public LocalDateTime getFinPeriodoActual() { return finPeriodoActual; }
    public void setFinPeriodoActual(LocalDateTime finPeriodoActual) { this.finPeriodoActual = finPeriodoActual; }
    
    public Boolean getCancelarAlFinalPeriodo() { return cancelarAlFinalPeriodo; }
    public void setCancelarAlFinalPeriodo(Boolean cancelarAlFinalPeriodo) { this.cancelarAlFinalPeriodo = cancelarAlFinalPeriodo; }
    
    public LocalDateTime getFechaCancelacion() { return fechaCancelacion; }
    public void setFechaCancelacion(LocalDateTime fechaCancelacion) { this.fechaCancelacion = fechaCancelacion; }
    
    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }
    
    public String getIdSuscripcion() { return idSuscripcion; }
    public void setIdSuscripcion(String idSuscripcion) { this.idSuscripcion = idSuscripcion; }
    
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    
    public List<Pago> getPagos() { return pagos; }
    public void setPagos(List<Pago> pagos) { this.pagos = pagos; }
}