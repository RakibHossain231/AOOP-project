package com.example.achievekit;

public class AuthService {
    private final UserDao userDao = new UserDao();

    /** সফল হলে লগইন করা User রিটার্ন করে, নাহলে null */
    public User authenticate(String loginIdentifier, String rawPassword) throws Exception {
        User u = userDao.findByLogin(loginIdentifier);
        if (u == null) return null;
        if (u.getStatus() == null || !u.getStatus().equalsIgnoreCase("active")) return null;
        return PasswordUtil.matches(rawPassword, u.getPasswordHash()) ? u : null;
    }
}
