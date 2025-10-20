package com.example.achievekit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Expense {
    private int expenseId;
    private int userId;
    private String category;        // e.g., Food, Transport, Study
    private String type;            // "income" | "expense"
    private BigDecimal amount;
    private String description;     // UI: Name
    private String notes;           // NEW
    private LocalDate expenseDate;
    private LocalDateTime createdAt;

    public Expense() {}

    public Expense(int expenseId, int userId, String category, String type,
                   BigDecimal amount, String description, String notes,
                   LocalDate expenseDate, LocalDateTime createdAt) {
        this.expenseId = expenseId;
        this.userId = userId;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.notes = notes;
        this.expenseDate = expenseDate;
        this.createdAt = createdAt;
    }

    // --- getters/setters ---
    public int getExpenseId() { return expenseId; }
    public void setExpenseId(int expenseId) { this.expenseId = expenseId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // --- UI helper (TableColumn mapping) ---
    public String getNameForTable() { return description; }
    public String getNotesForTable() { return notes == null ? "" : notes; }
}
