-- src/main/resources/data.sql
-- Inicializar planes de suscripción
INSERT INTO planes_suscripcion (nombre, descripcion, precio, moneda, 
    limite_condiciones_medicas, limite_alergias, limite_preferencias_alimentarias,
    ingredientes_por_busqueda, limite_recetas_favoritas, activo) 
VALUES 
('gratuito', 'Plan gratuito con características básicas', 0.00, 'COP', 
 2, 2, 1, 7, 4, true),
('premium', 'Plan premium con todas las características', 29900.00, 'COP', 
 3, 4, 1, 20, NULL, true);