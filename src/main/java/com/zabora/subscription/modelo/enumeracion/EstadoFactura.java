package com.zabora.subscription.modelo.enumeracion;
/**
 * Enumeración que representa los posibles estados de una factura en el sistema.
 * 
 * Cada valor indica en qué etapa se encuentra la factura.
 */
public enum EstadoFactura {
    BORRADOR,//Factura creada pero todavía no finalizada ni enviada.
    EMITIDA,//Factura emitida oficialmente, lista para ser pagada.
    PAGADA,//Factura que ha sido pagada por el usuario.
    ANULADA//Factura anulada y que ya no tiene validez. Opcional segun desarrollo
}