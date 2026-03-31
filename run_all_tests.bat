@echo off
echo ========================================
echo EJECUTANDO TODAS LAS PRUEBAS CP032-CP039
echo ========================================
echo.

echo [1/8] CP032 - Selección Método Pago
mvn test -Dtest=CP032_SeleccionMetodoPagoTest
echo.
echo ========================================
echo.

echo [2/8] CP033 - Conexión PSE
mvn test -Dtest=CP033_ConexionPseTest
echo.
echo ========================================
echo.

echo [3/8] CP034 - Conexión Tarjeta
mvn test -Dtest=CP034_ConexionTarjetaTest
echo.
echo ========================================
echo.

echo [4/8] CP035 - Recolectar Información
mvn test -Dtest=CP035_RecolectarInformacionTest
echo.
echo ========================================
echo.

echo [5/8] CP036 - Cancelar Suscripción
mvn test -Dtest=CP036_CancelarSuscripcionTest
echo.
echo ========================================
echo.

echo [6/8] CP037 - Verificar Estado
mvn test -Dtest=CP037_VerificarEstadoTest
echo.
echo ========================================
echo.

echo [7/8] CP038 - Finalizar Pago
mvn test -Dtest=CP038_FinalizarPagoExitosoTest
echo.
echo ========================================
echo.

echo [8/8] CP039 - Reportar Ingresos
mvn test -Dtest=CP039_ReportarIngresosTest
echo.
echo ========================================
echo TODAS LAS PRUEBAS COMPLETADAS
echo ========================================
pause
