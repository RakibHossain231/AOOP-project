package com.example.achievekit;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    public static String hash(String raw) {
        return BCrypt.hashpw(raw, BCrypt.gensalt(10));
    }
    public static boolean matches(String raw, String hashed) {
        if (raw == null || hashed == null || hashed.isBlank()) return false;
        return BCrypt.checkpw(raw, hashed);
    }
}
