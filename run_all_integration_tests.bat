@echo off
echo ========================================
echo EJECUTANDO PRUEBAS DE INTEGRACION COMPLETAS
echo ========================================
echo.

REM Compilar el proyecto
echo [1/5] Compilando proyecto...
call mvn clean compile -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: No se pudo compilar el proyecto
    pause
    exit /b 1
)

REM Ejecutar todas las pruebas de integración
echo [2/5] Ejecutando pruebas de integración...
call mvn test -Dtest="*IntegrationTest" -q
if %ERRORLEVEL% neq 0 (
    echo ADVERTENCIA: Algunas pruebas pueden haber fallado
)

echo [3/5] Generando reporte de pruebas...
call mvn surefire-report:report -q

echo [4/5] Mostrando resumen de resultados...
echo.

REM Buscar y mostrar los reportes generados
if exist "target\site\surefire-report.html" (
    echo Reporte HTML generado: target\site\surefire-report.html
    start target\site\surefire-report.html
)

if exist "target\surefire-reports" (
    echo Reportes XML generados en: target\surefire-reports\
    dir target\surefire-reports\*.xml
)

echo.
echo [5/5] Pruebas completadas!
echo ========================================
echo RESUMEN DE CASOS DE PRUEBA CP032-CP039
echo ========================================
echo.
echo CP032 - Seleccion de Metodo de Pago
echo CP033 - Flujo Pago Tarjeta
echo CP034 - Flujo Pago PSE  
echo CP035 - Creacion de Suscripcion
echo CP036 - Cancelacion de Suscripcion
echo CP037 - Verificacion de Estado
echo CP038 - Procesamiento de Pago (Bricks)
echo CP039 - Reportes Administrativos
echo WebhookSimulationTest - Simulacion Webhook
echo.
echo Revise la consola para los reportes detallados de cada caso de prueba.
echo Los reportes incluyen: codigo HTTP, datos clave, y veredicto final.
echo.
pause
