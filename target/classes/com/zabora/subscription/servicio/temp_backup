package com.zabora.subscription.servicio;

import com.zabora.subscription.modelo.entidad.Pago;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EmailService {
    
    @Value("${email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${email.from:noreply@zabora.com}")
    private String emailFrom;
    
    /**
     * Envía confirmación de pago exitoso
     */
    public void enviarConfirmacionPago(Pago pago) {
        if (!emailEnabled) {
            log.info("[SIMULADO] Confirmación de pago enviada para: {}", pago.getUsuarioId());
            return;
        }
        
        
        String mensaje = construirMensajeConfirmacion(pago);
        log.info("Email de confirmación: {}", mensaje);
    }
    
    /**
     * Envía factura del pago
     */
    public void enviarFacturaPago(Pago pago) {
        if (!emailEnabled) {
            log.info(" [SIMULADO] Factura enviada para: {}", pago.getUsuarioId());
            return;
        }
        
        // TODO: Implementar generación y envío de factura PDF
        String mensaje = construirFactura(pago);
        log.info(" Factura generada: {}", mensaje);
    }
    
    /**
     * Envía notificación de pago fallido
     */
    public void enviarNotificacionPagoFallido(Integer usuarioId, String motivo) {
        if (!emailEnabled) {
            log.info(" [SIMULADO] Notificación de fallo enviada para: {}", usuarioId);
            return;
        }
        
        log.info("Email de pago fallido: Usuario {}, Motivo: {}", usuarioId, motivo);
    }
    
    /**
     * Envía recordatorio de renovación próxima
     */
    public void enviarRecordatorioRenovacion(Integer usuarioId, int diasRestantes) {
        if (!emailEnabled) {
            log.info(" [SIMULADO] Recordatorio de renovación: {} - {} días", 
                    usuarioId, diasRestantes);
            return;
        }
        
        log.info("🔔 Recordatorio de renovación enviado a: {}", usuarioId);
    }
    
    private String construirMensajeConfirmacion(Pago pago) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        return String.format("""
            ¡Pago Exitoso!
            
            Tu suscripción Premium ha sido activada.
            
            Detalles del pago:
            - Monto: $%s COP
            - Fecha: %s
            - Método: %s
            - ID de transacción: %s
            
            Comprobante: %s
            
            ¡Gracias por confiar en Zabora!
            """,
            pago.getMonto(),
            pago.getFechaPago().format(formatter),
            pago.getMetodoPago(),
            pago.getId(),
            pago.getUrlComprobante() != null ? pago.getUrlComprobante() : "No disponible"
        );
    }
    
    private String construirFactura(Pago pago) {
        return String.format("""
            FACTURA DE VENTA
            
            Zabora S.A.S.
            NIT: 900.123.456-7
            
            Cliente: %s
            Fecha: %s
            
            Concepto: Suscripción Premium Mensual
            Valor: $%s COP
            
            Método de pago: %s
            """,
            pago.getUsuarioId(),
            pago.getFechaPago(),
            pago.getMonto(),
            pago.getMetodoPago()
        );
    }
}
