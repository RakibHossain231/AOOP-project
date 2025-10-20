package com.example.achievekit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    // ✅ MariaDB URL
    private static final String URL =
            "jdbc:mariadb://localhost:3306/oopdatabase"; // আমার ডাটাবেস এর নাম oopdatabse

    // ⬇️ নিজের ক্রেডেনশিয়াল বসাবো
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static {
        // কিছু সিস্টেমে ড্রাইভার লোড করা দরকার হতে পারে
        try { Class.forName("org.mariadb.jdbc.Driver"); } catch (ClassNotFoundException ignored) {}
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
