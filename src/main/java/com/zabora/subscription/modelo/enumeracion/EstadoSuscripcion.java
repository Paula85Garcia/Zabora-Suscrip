package com.zabora.subscription.modelo.enumeracion;


/**
 * Enumeración que representa los posibles estados de una suscripción en el sistema.
 * 
 * Cada valor indica en qué etapa o situación se encuentra la suscripción.
 */

public enum EstadoSuscripcion {
    ACTIVA,//Suscripción activa y vigente.
    CANCELADA,//Suscripción que fue cancelada por el usuario o el sistema.
    EXPIRADA,//Suscripción que llegó a su fecha de vencimiento y ya no está vigente
    PENDIENTE_PAGO//Suscripción pendiente de pago; aún no está activa hasta que se complete el pago.
}
