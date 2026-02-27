package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.PlanSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de la entidad PlanSuscripcion
 */
@Repository
public interface PlanSuscripcionRepositorio extends JpaRepository<PlanSuscripcion, Long> {
    
    /**
     * Buscar un plan por nombre (ignorando mayúsculas y minúsculas)
     * Ejemplo: "premium", "gratuito"
     */
    Optional<PlanSuscripcion> findByNombreIgnoreCase(String nombre);
    
    /**
     * Buscar todos los planes activos
     * Utilizado para mostrar los planes disponibles a los usuarios
     */
    List<PlanSuscripcion> findByActivoTrue();
    
    /**
     * Verificar si existe un plan por nombre (ignorando mayúsculas y minúsculas)
     */
    boolean existsByNombreIgnoreCase(String nombre);
}
