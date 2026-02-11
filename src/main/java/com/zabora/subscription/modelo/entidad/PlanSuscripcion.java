package com.zabora.subscription.modelo.entidad;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "planes_suscripcion")
@Data
public class PlanSuscripcion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;
    
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
    
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;
    
    @Column(name = "moneda", length = 3)
    private String moneda = "COP";
    
    @Column(name = "limite_condiciones_medicas", nullable = false)
    private Integer limiteCondicionesMedicas = 0;
    
    @Column(name = "limite_alergias", nullable = false)
    private Integer limiteAlergias = 0;
    
    @Column(name = "limite_preferencias_alimentarias", nullable = false)
    private Integer limitePreferenciasAlimentarias = 0;
    
    @Column(name = "ingredientes_por_busqueda", nullable = false)
    private Integer ingredientesPorBusqueda = 0;
    
    @Column(name = "limite_recetas_favoritas")
    private Integer limiteRecetasFavoritas;
    
    @Column(name = "activo")
    private Boolean activo = true;
    
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
    
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
    
    // Constructor para inicializacion
    public PlanSuscripcion() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }
}