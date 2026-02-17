package com.zabora.subscription.data;

public class UserContext {

    private static final ThreadLocal<UserData> context = new ThreadLocal<>();

    public static void set(UserData user) {
        context.set(user);
    }

    public static UserData get() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }
}
