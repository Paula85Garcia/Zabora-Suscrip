package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.PlanSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PlanSuscripcionRepository extends JpaRepository<PlanSuscripcion, Long> {
    
    // Buscar plan por nombre (ej: "premium", "gratuito")
    Optional<PlanSuscripcion> findByNombre(String nombre);
    
    // Buscar solo planes activos
    List<PlanSuscripcion> findByActivoTrue();
    
    // Verificar si existe un plan con ese nombre
    boolean existsByNombre(String nombre);
}