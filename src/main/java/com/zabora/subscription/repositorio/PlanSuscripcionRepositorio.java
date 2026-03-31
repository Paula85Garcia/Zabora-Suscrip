package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.PlanSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de la entidad PlanSuscripcion.
 *
 * FIX BUG-6: Cambiado de JpaRepository<PlanSuscripcion, Long> a Integer.
 *            PlanSuscripcion.id es Integer, no Long.
 */
@Repository
public interface PlanSuscripcionRepositorio extends JpaRepository<PlanSuscripcion, Integer> {

    Optional<PlanSuscripcion> findByNombreIgnoreCase(String nombre);

    List<PlanSuscripcion> findByActivoTrue();

    boolean existsByNombreIgnoreCase(String nombre);
}
