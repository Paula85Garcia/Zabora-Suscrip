package com.zabora.subscription.excepcion;

public class PagoRechazadoException extends RuntimeException {

    private final String statusDetail;
    private final String mpPaymentId;

    public PagoRechazadoException(String statusDetail, String mpPaymentId) {
        super("Pago rechazado por MercadoPago. Detalle: " + statusDetail);
        this.statusDetail = statusDetail;
        this.mpPaymentId = mpPaymentId;
    }

    public String getStatusDetail() { return statusDetail; }
    public String getMpPaymentId()  { return mpPaymentId; }
}
