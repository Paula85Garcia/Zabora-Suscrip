package com.zabora.subscription.repositorio;

import com.zabora.subscription.modelo.entidad.Pago;
import com.zabora.subscription.modelo.enumeracion.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepositorio extends JpaRepository<Pago, String> {
    
    List<Pago> findByUsuarioIdOrderByFechaCreacionDesc(String usuarioId);
    
    Optional<Pago> findByIdIntentoPago(String idIntentoPago);
    
    Optional<Pago> findBySuscripcionIdAndEstado(String suscripcionId, EstadoPago estado);
    
    boolean existsBySuscripcionIdAndEstado(String suscripcionId, EstadoPago estado);
    
    List<Pago> findByEstado(EstadoPago estado);
    
    List<Pago> findByUsuarioIdAndEstado(String usuarioId, EstadoPago estado);
    
    long countByUsuarioIdAndEstado(String usuarioId, EstadoPago estado);
}