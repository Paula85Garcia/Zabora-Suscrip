-- Datos de prueba para tests de integración

-- Insertar planes de suscripción
INSERT INTO planes_suscripcion (id, nombre, descripcion, precio, moneda, limite_condiciones_medicas, limite_alergias, limite_preferencias_alimentarias, ingredientes_por_busqueda, limite_recetas_favoritas, activo)
VALUES 
(1, 'gratuito', 'Plan gratuito con caracteristicas basicas', 0.00, 'COP', 2, 2, 2, 7, 4, TRUE),
(2, 'premium', 'Plan premium con todas las caracteristicas', 29900.00, 'COP', 3, 4, 4, 20, NULL, TRUE);
