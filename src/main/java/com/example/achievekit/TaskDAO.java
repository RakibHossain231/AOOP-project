package com.example.achievekit;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    /** Insert a new Task and return generated ID */
    public int insert(Task t) throws SQLException {
        String sql = "INSERT INTO Tasks (UserID, Title, Description, Priority, Status, DueDate) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, t.getUserId());
            ps.setString(2, t.getTitle());
            ps.setString(3, t.getDescription());
            ps.setString(4, t.getPriority());
            ps.setString(5, t.getStatus());
            ps.setDate(6, t.getDueDate() != null ? Date.valueOf(t.getDueDate()) : null);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    t.setTaskId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /** Update a task's status (used by PomodoroController) */
    public void updateStatus(int taskId, String status) throws SQLException {
        String sql = "UPDATE Tasks SET Status = ?, UpdatedAt = CURRENT_TIMESTAMP WHERE TaskID = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    /** Update title and date from Task Manager */
    public void updateTitleAndDate(int taskId, String title, LocalDate dueDate) throws SQLException {
        String sql = "UPDATE Tasks SET Title = ?, DueDate = ?, UpdatedAt = CURRENT_TIMESTAMP WHERE TaskID = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setDate(2, dueDate != null ? Date.valueOf(dueDate) : null);
            ps.setInt(3, taskId);
            ps.executeUpdate();
        }
    }

    /** Delete a task */
    public void delete(int taskId) throws SQLException {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Tasks WHERE TaskID = ?")) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        }
    }

    /** List all tasks for a user */
    public List<Task> listByUser(int userId) throws SQLException {
        String sql = """
                SELECT TaskID, UserID, Title, Description, Priority, Status, DueDate
                FROM Tasks
                WHERE UserID = ?
                ORDER BY 
                    CASE 
                        WHEN Status = 'pending' THEN 1
                        WHEN Status = 'in-progress' THEN 2
                        WHEN Status = 'completed' THEN 3
                        ELSE 4
                    END,
                    DueDate IS NULL, DueDate ASC, TaskID DESC
                """;
        List<Task> list = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Task(
                            rs.getInt("TaskID"),
                            rs.getInt("UserID"),
                            rs.getString("Title"),
                            rs.getString("Description"),
                            rs.getString("Priority"),
                            rs.getString("Status"),
                            rs.getDate("DueDate") != null
                                    ? rs.getDate("DueDate").toLocalDate()
                                    : null
                    ));
                }
            }
        }
        return list;
    }

    /** Get a single task by its ID (optional helper) */
    public Task findById(int taskId) throws SQLException {
        String sql = "SELECT * FROM Tasks WHERE TaskID = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Task(
                            rs.getInt("TaskID"),
                            rs.getInt("UserID"),
                            rs.getString("Title"),
                            rs.getString("Description"),
                            rs.getString("Priority"),
                            rs.getString("Status"),
                            rs.getDate("DueDate") != null
                                    ? rs.getDate("DueDate").toLocalDate()
                                    : null
                    );
                }
            }
        }
        return null;
    }

    /** Count how many auto-generated Dummy Tasks exist for a user.
     *  Used by PomodoroController to name new dummy tasks sequentially.
     */
    public int countDummyForUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Tasks WHERE UserID = ? AND Title LIKE 'Dummy Task %'";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

}
