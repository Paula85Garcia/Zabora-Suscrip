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
    
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre; // "gratuito", "premium"
    
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
    
    @Column(name = "precio", precision = 10, scale = 2)
    private BigDecimal precio;
    
    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda = "COP";
    
    @Column(name = "limite_condiciones_medicas")
    private Integer limiteCondicionesMedicas;
    
    @Column(name = "limite_alergias")
    private Integer limiteAlergias;
    
    @Column(name = "limite_preferencias_alimentarias")
    private Integer limitePreferenciasAlimentarias;
    
    @Column(name = "ingredientes_por_busqueda")
    private Integer ingredientesPorBusqueda;
    
    @Column(name = "limite_recetas_favoritas")
    private Integer limiteRecetasFavoritas;
    
    @Column(name = "activo")
    private Boolean activo = true;
    
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
    
    // Constructor para datos quemados
    public PlanSuscripcion() {}
    
    public PlanSuscripcion(Long id, String nombre, String descripcion, BigDecimal precio, 
                          Integer limiteCondicionesMedicas, Integer limiteAlergias, 
                          Integer limitePreferenciasAlimentarias, Integer ingredientesPorBusqueda, 
                          Integer limiteRecetasFavoritas) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.limiteCondicionesMedicas = limiteCondicionesMedicas;
        this.limiteAlergias = limiteAlergias;
        this.limitePreferenciasAlimentarias = limitePreferenciasAlimentarias;
        this.ingredientesPorBusqueda = ingredientesPorBusqueda;
        this.limiteRecetasFavoritas = limiteRecetasFavoritas;
    }
}