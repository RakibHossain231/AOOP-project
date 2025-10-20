package com.example.achievekit;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseDAO {

    public List<Expense> findAllByUser(int userId) {
        String sql = """
            SELECT ExpenseID, UserID, Category, Type, Amount, Description, Notes, ExpenseDate, CreatedAt
            FROM Expenses
            WHERE UserID = ?
            ORDER BY ExpenseDate DESC, ExpenseID DESC
        """;
        List<Expense> list = new ArrayList<>();
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rowToExpense(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int insert(Expense ex) throws SQLException {
        String sql = """
            INSERT INTO Expenses (UserID, Category, Type, Amount, Description, Notes, ExpenseDate)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ex.getUserId());
            ps.setString(2, ex.getCategory());
            ps.setString(3, ex.getType());
            ps.setBigDecimal(4, ex.getAmount());
            ps.setString(5, ex.getDescription());
            ps.setString(6, ex.getNotes());
            ps.setDate(7, Date.valueOf(ex.getExpenseDate()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return 0;
    }

    public boolean update(Expense ex) throws SQLException {
        String sql = """
            UPDATE Expenses
            SET Category=?, Type=?, Amount=?, Description=?, Notes=?, ExpenseDate=?
            WHERE ExpenseID=? AND UserID=?
        """;
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ex.getCategory());
            ps.setString(2, ex.getType());
            ps.setBigDecimal(3, ex.getAmount());
            ps.setString(4, ex.getDescription());
            ps.setString(5, ex.getNotes());
            ps.setDate(6, Date.valueOf(ex.getExpenseDate()));
            ps.setInt(7, ex.getExpenseId());
            ps.setInt(8, ex.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int expenseId, int userId) throws SQLException {
        String sql = "DELETE FROM Expenses WHERE ExpenseID=? AND UserID=?";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, expenseId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public Map<LocalDate, BigDecimal> last7DaysTotals(int userId) {
        String sql = """
            SELECT ExpenseDate AS d, SUM(Amount) AS total
            FROM Expenses
            WHERE UserID=? AND Type='expense' AND ExpenseDate >= (CURRENT_DATE - INTERVAL 6 DAY)
            GROUP BY ExpenseDate
        """;
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate d = rs.getDate("d").toLocalDate();
                    BigDecimal t = rs.getBigDecimal("total");
                    map.put(d, t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    private Expense rowToExpense(ResultSet rs) throws SQLException {
        Expense e = new Expense();
        e.setExpenseId(rs.getInt("ExpenseID"));
        e.setUserId(rs.getInt("UserID"));
        e.setCategory(rs.getString("Category"));
        e.setType(rs.getString("Type"));
        e.setAmount(rs.getBigDecimal("Amount"));
        e.setDescription(rs.getString("Description"));
        e.setNotes(rs.getString("Notes"));

        Date d = rs.getDate("ExpenseDate");
        if (d != null) e.setExpenseDate(d.toLocalDate());

        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) e.setCreatedAt(ts.toLocalDateTime());
        return e;
    }
}
