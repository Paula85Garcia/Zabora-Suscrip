package com.zabora.subscription.modelo.enumeracion;
/**
 * Enumeración que representa los posibles roles que un usuario puede tener en el sistema.
 * 
 * Cada rol define el nivel de permisos y acceso a funcionalidades específicas.
 */
public enum RolUsuario {
    USUARIO,//Usuario normal con acceso limitado a sus propios datos y acciones.
    ADMINISTRADOR,//Administrador con permisos para gestionar usuarios, suscripciones y configuraciones.
    SISTEMA//Rol del sistema usado para operaciones internas automáticas.
}