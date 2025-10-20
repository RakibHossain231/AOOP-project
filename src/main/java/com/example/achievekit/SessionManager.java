package com.example.achievekit;

public final class SessionManager {

    private static User currentUser;

    // private constructor to prevent instantiation
    private SessionManager() {}

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }

    // ✅ Added logout method
    public static void logout() {
        currentUser = null; // clear the stored user
    }
}
