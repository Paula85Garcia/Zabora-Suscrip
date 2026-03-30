package com.zabora.subscription.data;

/**
 * ThreadLocal que almacena la informacion del usuario autenticado
 * durante el ciclo de vida de la request HTTP.
 *
 * Poblado por UserContextFilter con los headers X-User-Id, X-User-Email, X-User-Role
 * que inyecta el API Gateway tras validar el JWT.
 */
public class UserContext {

    private static final ThreadLocal<UserData> currentUser = new ThreadLocal<>();

    public static void set(UserData userData) {
        currentUser.set(userData);
    }

    public static UserData get() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
