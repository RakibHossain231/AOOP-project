package com.example.achievekit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class UserDao {

    public boolean existsByEmail(String email) throws Exception {
        String sql = "SELECT 1 FROM users WHERE `Email` = ? LIMIT 1";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean existsByUsername(String username) throws Exception {
        String sql = "SELECT 1 FROM users WHERE `Username` = ? LIMIT 1";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int insert(User u) throws Exception {
        String sql = "INSERT INTO users (`Username`,`Email`,`PasswordHash`,`FullName`,`Status`,`SecurityQuestion`,`SecurityAnswerHash`) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getFullName());
            ps.setString(5, u.getStatus());
            ps.setString(6, u.getSecurityQuestion());
            ps.setString(7, u.getSecurityAnswerHash());

            int updated = ps.executeUpdate();
            if (updated != 1) throw new RuntimeException("Insert failed");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    u.setUserId(id);
                    return id;
                }
            }
        }
        throw new RuntimeException("No generated key returned");
    }

    /** Username বা Email – যেকোনোটা দিয়ে ইউজার আনবে (login/forgot flow এ কাজে লাগে) */
    public User findByLogin(String login) throws Exception {
        String sql = "SELECT `UserID`,`Username`,`Email`,`PasswordHash`,`FullName`,`JoinDate`,`Status` " +
                "FROM users WHERE `Username` = ? OR `Email` = ? LIMIT 1";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
                return null;
            }
        }
    }

    /** userId দিয়ে পাসওয়ার্ড হ্যাশ আপডেট (existing usage) */
    public void updatePasswordHashByUserId(int userId, String newHash) throws Exception {
        String sql = "UPDATE users SET `PasswordHash` = ? WHERE `UserID` = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            if (ps.executeUpdate() != 1) throw new RuntimeException("Password update failed");
        }
    }

    /** Forgot password: username দিয়ে পাসওয়ার্ড আপডেট */
    public int updatePasswordByUsername(String username, String newHash) throws Exception {
        String sql = "UPDATE users SET `PasswordHash` = ? WHERE `Username` = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, username);
            return ps.executeUpdate();
        }
    }

    /** Security Question lookup */
    public String findSecurityQuestionByUsername(String username) throws Exception {
        String sql = "SELECT `SecurityQuestion` FROM users WHERE `Username`=? LIMIT 1";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Security Answer hash lookup */
    public String findSecurityAnswerHashByUsername(String username) throws Exception {
        String sql = "SELECT `SecurityAnswerHash` FROM users WHERE `Username`=? LIMIT 1";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // ---------- Mapper ----------
    private User mapUser(ResultSet rs) throws Exception {
        int id = rs.getInt("UserID");
        String username = rs.getString("Username");
        String email = rs.getString("Email");
        String passwordHash = rs.getString("PasswordHash");
        String fullName = rs.getString("FullName");
        Timestamp joinDate = rs.getTimestamp("JoinDate");
        String status = rs.getString("Status");
        return new User(id, username, email, passwordHash, fullName, joinDate, status);
    }
}
