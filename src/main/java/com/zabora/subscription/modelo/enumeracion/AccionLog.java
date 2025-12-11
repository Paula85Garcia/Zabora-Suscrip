package com.zabora.subscription.modelo.enumeracion;

/**
 * Enumeración que define las posibles acciones que pueden registrarse
 * en el log de suscripciones o pagos.
 * 
 * Cada valor representa un tipo de evento importante en el sistema.
 */
public enum AccionLog {
    CREACION,//Registro de la creación de un recurso (ej. una suscripción o método de pago).
    ACTIVACION,//Activación de una suscripción o servicio.
    CANCELACION,//Cancelación de una suscripción o servicio.
    RENOVACION,//Renovación automática(opcional segun desarrollo) o manual de una suscripción
    PAGO_EXITOSO,//Pago exitoso realizado por un usuario.
    PAGO_FALLIDO,//Pago fallido o rechazado.
    CAMBIO_PLAN,//Cambio de plan de suscripción por parte del usuario.
    REEMBOLSO,//Reembolso realizado al usuario opcional segun desarrollo
    CAMBIO_ESTADO//Cambio de estado de un recurso (ej. de activo a inactivo).
}