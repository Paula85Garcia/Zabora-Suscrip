package com.zabora.subscription.modelo.entidad;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Entidad que representa un plan de suscripción.
 * 
 * Define características, límites y precio de cada plan disponible para los usuarios.
 */
@Entity
@Table(name = "planes_suscripcion")
@Data
public class PlanSuscripcion {
    //Identificador único del plan (autoincremental).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    
    //Nombre del plan, por ejemplo: "gratuito", "premium".
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre; // "gratuito", "premium"
    
    
    //Descripción del plan.
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
    
    
    //Precio del plan en la moneda definida.
    @Column(name = "precio", precision = 10, scale = 2)
    private BigDecimal precio;
    
    
    //Moneda del plan, por defecto COP (pesos colombianos).
    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda = "COP";
    
    
    //Límite de condiciones médicas que el usuario puede registrar en este plan.
    @Column(name = "limite_condiciones_medicas")
    private Integer limiteCondicionesMedicas;
    
    
    //Límite de alergias que el usuario puede registrar en este plan.
    @Column(name = "limite_alergias")
    private Integer limiteAlergias;
    
    
    //Límite de preferencias alimentarias que el usuario puede registrar.
    @Column(name = "limite_preferencias_alimentarias")
    private Integer limitePreferenciasAlimentarias;
    
    
    //Cantidad de ingredientes que el usuario puede buscar por consulta.
    @Column(name = "ingredientes_por_busqueda")
    private Integer ingredientesPorBusqueda;
    
    
    //Límite de recetas que el usuario puede marcar como favoritas.
    @Column(name = "limite_recetas_favoritas")
    private Integer limiteRecetasFavoritas;
    
    //Indica si el plan está activo.
    @Column(name = "activo")
    private Boolean activo = true;
    
    
    //Fecha y hora de creación del plan.
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    //Fecha y hora de la última actualización del plan.
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
    
    // ===== CONSTRUCTORES =====
    //Constructor vacío para JPA y datos quemados.
    public PlanSuscripcion() {}
    /**
     * Constructor completo para inicializar un plan con todos sus atributos.
     */
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