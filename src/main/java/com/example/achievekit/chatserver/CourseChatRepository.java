package com.example.achievekit.chatserver;

import com.example.achievekit.DB;
import com.example.achievekit.chat.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseChatRepository {

    /** username -> users.userID (exact column name userID) */
    public Integer findUserIDByUsername(String username) throws Exception {
        String sql = "SELECT `userID` FROM users WHERE username=?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        }
    }

    /** ensure user is member of the course (coursemembers) */
    public void ensureMember(int courseId, int userID) throws Exception {
        String sql = """
            INSERT INTO coursemembers(CourseID, UserID, Role)
            VALUES (?,?, 'student')
            ON DUPLICATE KEY UPDATE JoinedAt=JoinedAt
        """;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, userID);
            ps.executeUpdate();
        }
    }

    /** save message into coursemessages */
    public void saveMessage(int courseId, int senderUserID, String body, long ts) throws Exception {
        String sql = "INSERT INTO coursemessages(CourseID, SenderID, MessageType, Content, SentAt) VALUES (?,?, 'text', ?, FROM_UNIXTIME(?/1000))";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, senderUserID);
            ps.setString(3, body);
            ps.setLong(4, ts);
            ps.executeUpdate();
        }
    }

    /** recent N messages (old -> new) */
    public List<Message> recentMessages(int courseId, int limit) throws Exception {
        String sql = """
            SELECT u.username AS sender, cm.Content AS body,
                   UNIX_TIMESTAMP(cm.SentAt)*1000 AS ts
            FROM coursemessages cm
            JOIN users u ON u.`userID` = cm.SenderID
            WHERE cm.CourseID = ?
            ORDER BY cm.SentAt DESC
            LIMIT ?
        """;
        List<Message> out = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(Message.history("course:"+courseId, rs.getString("sender"),
                            rs.getString("body"), rs.getLong("ts")));
                }
            }
        }
        out.sort((a,b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        return out;
    }
}
