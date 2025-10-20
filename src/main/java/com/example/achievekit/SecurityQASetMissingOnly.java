

/*Only one time we can run this, karon ager user der Q/A nai tai*/
/*
package com.example.achievekit;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class SecurityQASetMissingOnly {
    public static void main(String[] args) throws Exception {
        final String question = "Where Do you live?";
        final String answerHash = PasswordUtil.hash("dhaka");

        String sql = """
            UPDATE users
               SET SecurityQuestion=?, SecurityAnswerHash=?
             WHERE SecurityQuestion IS NULL OR SecurityQuestion=''
                OR SecurityAnswerHash IS NULL OR SecurityAnswerHash=''
        """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, question);
            ps.setString(2, answerHash);
            int updated = ps.executeUpdate();
            System.out.println("Updated rows (NULL/empty only): " + updated);
        }
        System.out.println("DONE. Refresh phpMyAdmin to verify.");
    }
}
*/