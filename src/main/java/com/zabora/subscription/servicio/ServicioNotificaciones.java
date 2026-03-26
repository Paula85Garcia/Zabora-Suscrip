package com.zabora.subscription.servicio;

import org.springframework.stereotype.Service;

@Service
public class ServicioNotificaciones {
    
    public void notificarPagoCompletado(Integer usuarioId, String suscripcionId) {
        System.out.println("Notificando pago completado - Usuario: " + usuarioId + ", Suscripción: " + suscripcionId);
    }
    
    public void notificarActualizacionSuscripcion(Integer usuarioId, String tipo) {
        System.out.println("Notificando actualización de suscripción - Usuario: " + usuarioId + ", Tipo: " + tipo);
    }
}
