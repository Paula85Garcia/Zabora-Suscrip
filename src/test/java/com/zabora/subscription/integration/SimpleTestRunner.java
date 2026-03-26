package com.zabora.subscription.integration;

/**
 * Ejecutor simple de pruebas sin JUnit Suite
 * Para ejecutar todas las pruebas usa este comando:
 * mvn test -Dtest=SimpleTestRunner
 */
public class SimpleTestRunner {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("EJECUTOR DE PRUEBAS - SUSCRIPCION SERVICE");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Para ejecutar todas las pruebas, usa estos comandos:");
        System.out.println();
        System.out.println("1. CP032 - Selección Método Pago:");
        System.out.println("   mvn test -Dtest=CP032_SeleccionMetodoPagoTest");
        System.out.println();
        System.out.println("2. CP033 - Conexión PSE:");
        System.out.println("   mvn test -Dtest=CP033_ConexionPseTest");
        System.out.println();
        System.out.println("3. CP034 - Conexión Tarjeta:");
        System.out.println("   mvn test -Dtest=CP034_ConexionTarjetaTest");
        System.out.println();
        System.out.println("4. CP035 - Recolectar Información:");
        System.out.println("   mvn test -Dtest=CP035_RecolectarInformacionTest");
        System.out.println();
        System.out.println("5. CP036 - Cancelar Suscripción:");
        System.out.println("   mvn test -Dtest=CP036_CancelarSuscripcionTest");
        System.out.println();
        System.out.println("6. CP037 - Verificar Estado:");
        System.out.println("   mvn test -Dtest=CP037_VerificarEstadoTest");
        System.out.println();
        System.out.println("7. CP038 - Finalizar Pago:");
        System.out.println("   mvn test -Dtest=CP038_FinalizarPagoExitosoTest");
        System.out.println();
        System.out.println("8. CP039 - Reportar Ingresos:");
        System.out.println("   mvn test -Dtest=CP039_ReportarIngresosTest");
        System.out.println();
        System.out.println("O ejecuta todo en un script batch:");
        System.out.println("run_all_tests.bat");
        System.out.println("========================================");
    }
}
