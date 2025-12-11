package com.zabora.subscription.modelo.enumeracion;
/**
 * Enumeración que representa los posibles estados de un pago en el sistema.
 * 
 * Cada valor indica en qué etapa se encuentra el proceso de pago.
 */
public enum EstadoPago {
    PENDIENTE,//Pago que aún no se ha completado, está pendiente de ejecución o confirmación.
    COMPLETADO,//Pago realizado correctamente y confirmado.
    FALLIDO,//Pago que no se pudo procesar correctamente o fue rechazado.
    REEMBOLSADO,//pago que ha sido devuelto al usuario como reembolso.Opcional segun desarrollo
    CANCELADO//Pago que fue cancelado antes de completarse o procesarse.
}