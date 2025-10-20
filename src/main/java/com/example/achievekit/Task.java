package com.example.achievekit;

import java.time.LocalDate;

public class Task {
    private int taskId;
    private int userId;
    private String title;
    private String description;
    private String priority;   // low, medium, high
    private String status;     // pending, in-progress, completed
    private LocalDate dueDate;

    public Task(int taskId, int userId, String title, String description,
                String priority, String status, LocalDate dueDate) {
        this.taskId = taskId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
    }
    public Task(int userId, String title, LocalDate dueDate) {
        this(0, userId, title, null, "medium", "pending", dueDate);
    }

    public Task() {

    }



    public int getTaskId() { return taskId; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }

    public void setTaskId(int taskId) { this.taskId = taskId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setStatus(String status) { this.status = status; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return title != null ? title : "Untitled Task";
    }



}
