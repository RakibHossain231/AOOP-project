package com.example.achievekit;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class todoController implements Initializable {

    // For Loading page indicator
    @FXML private Button dashboardBtn, pomodoroBtn, tasksBtn, expensesBtn, chatBtn;
    @FXML private Button analyticsBtn, settingsBtn, tipsBtn;

    @FXML private TextField newTaskField;
    @FXML private DatePicker newTaskDate;
    @FXML private ListView<Task> todayTaskList;
    @FXML private ListView<Task> upcomingTaskList;
    @FXML private ListView<Task> overdueTaskList;

    private final TaskDAO taskDAO = new TaskDAO();

    // --- constants for auto-height ---
    private static final double CELL_HEIGHT = 56; // তোমার সেল ডিজাইন অনুযায়ী চাইলে 48/52 করতে পারো

    // -------------------- Page navigation --------------------
    public void showPomodoro() throws IOException { HelloApplication.loadPage("pomodoroPage.fxml"); }
    public void showTasks() { /* already here */ }
    public void showExpenses() throws IOException { HelloApplication.loadPage("expense.fxml"); }

    HomeController a = new HomeController();
    public void showChat() { a.getOpenCourseChatPicker(); }
    public void showAnalytics() { System.out.println("Show analytics"); }
    public void showSettings() { System.out.println("Show settings"); }
    public void showTips() { System.out.println("Show tips"); }
    public void showDashboard() throws IOException { HelloApplication.loadPage("Homepage.fxml"); }
    public void logout() { System.out.println("Logout clicked"); }

    // -------------------- Init --------------------
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Nav.highlight( tasksBtn, pomodoroBtn, dashboardBtn, dashboardBtn,  expensesBtn, chatBtn, analyticsBtn, settingsBtn, tipsBtn);
        if (!SessionManager.isLoggedIn()) {
            System.out.println("No logged-in user; tasks will not load.");
            return;
        }

        // fixed cell height so we can calculate prefHeight properly
        todayTaskList.setFixedCellSize(CELL_HEIGHT);
        upcomingTaskList.setFixedCellSize(CELL_HEIGHT);
        overdueTaskList.setFixedCellSize(CELL_HEIGHT);

        // ensure ListView respects prefHeight (what we compute)
        todayTaskList.setMaxHeight(Region.USE_PREF_SIZE);
        upcomingTaskList.setMaxHeight(Region.USE_PREF_SIZE);
        overdueTaskList.setMaxHeight(Region.USE_PREF_SIZE);

        // Cell factories
        setupCellFactory(todayTaskList);
        setupCellFactory(upcomingTaskList);
        setupCellFactory(overdueTaskList);

        // Auto-height bindings (initial visible rows)
        bindAutoHeight(todayTaskList, 3);
        bindAutoHeight(upcomingTaskList, 2);
        bindAutoHeight(overdueTaskList, 2);

        // Load data
        refreshLists();
    }

    // -------------------- UI handlers --------------------
    @FXML
    public void addTask() {
        try {
            if (!SessionManager.isLoggedIn()) return;
            int userId = SessionManager.getCurrentUser().getUserId();

            String title = newTaskField.getText() != null ? newTaskField.getText().trim() : "";
            LocalDate due = newTaskDate.getValue();

            if (title.isEmpty()) {
                showToast("Please enter a task title.");
                return;
            }
            Task t = new Task(userId, title, due);
            taskDAO.insert(t);

            newTaskField.clear();
            newTaskDate.setValue(null);
            refreshLists();
        } catch (Exception ex) {
            ex.printStackTrace();
            showToast("Failed to add task.");
        }
    }

    @FXML public void showCompletedTasks() { filterByStatus("completed"); }
    @FXML public void showPendingTasks() { filterByStatus("pending"); }

    // -------------------- Data loading & grouping --------------------
    private void refreshLists() {
        try {
            int userId = SessionManager.getCurrentUser().getUserId();
            List<Task> all = taskDAO.listByUser(userId);

            LocalDate today = LocalDate.now();

            List<Task> todayTasks = all.stream()
                    .filter(t -> t.getDueDate() != null && t.getDueDate().isEqual(today))
                    .sorted(Comparator.comparing(Task::getTitle))
                    .toList();

            List<Task> upcoming = all.stream()
                    .filter(t -> t.getDueDate() != null && t.getDueDate().isAfter(today) && !t.isCompleted())
                    .sorted(Comparator.comparing(Task::getDueDate).thenComparing(Task::getTitle))
                    .toList();

            List<Task> overdue = all.stream()
                    .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today) && !t.isCompleted())
                    .sorted(Comparator.comparing(Task::getDueDate).thenComparing(Task::getTitle))
                    .toList();

            todayTaskList.getItems().setAll(todayTasks);
            upcomingTaskList.getItems().setAll(upcoming);
            overdueTaskList.getItems().setAll(overdue);

            // force resize after new data
            forceResize(todayTaskList, 3);
            forceResize(upcomingTaskList, 2);
            forceResize(overdueTaskList, 2);

        } catch (Exception ex) {
            ex.printStackTrace();
            showToast("Failed to load tasks.");
        }
    }

    private void filterByStatus(String status) {
        try {
            int userId = SessionManager.getCurrentUser().getUserId();
            List<Task> all = taskDAO.listByUser(userId);
            List<Task> filtered = all.stream()
                    .filter(t -> status.equalsIgnoreCase(t.getStatus()))
                    .collect(Collectors.toList());

            todayTaskList.getItems().setAll(filtered);
            upcomingTaskList.getItems().clear();
            overdueTaskList.getItems().clear();

            forceResize(todayTaskList, 3);
            forceResize(upcomingTaskList, 2);
            forceResize(overdueTaskList, 2);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // -------------------- Rendering each row --------------------
    private void setupCellFactory(ListView<Task> listView) {
        listView.setCellFactory(lv -> new ListCell<Task>() {
            @Override
            protected void updateItem(Task t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                CheckBox cb = new CheckBox(t.getTitle());
                cb.setSelected(t.isCompleted());
                cb.getStyleClass().add("check-box");

                Label dueLabel = new Label(t.getDueDate() != null ? "Due: " + t.getDueDate() : "No due");
                Button del = new Button("❌");
                del.getStyleClass().add("task-delete-btn");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox root = new HBox(10, cb, spacer, dueLabel, del);
                root.getStyleClass().add("task-item");

                // Toggle completed/pending
                cb.setOnAction(e -> {
                    try {
                        String newStatus = cb.isSelected() ? "completed" : "pending";
                        taskDAO.updateStatus(t.getTaskId(), newStatus);
                        t.setStatus(newStatus);
                        refreshLists(); // regroup & auto-resize
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showToast("Failed to update status.");
                        cb.setSelected(!cb.isSelected()); // rollback UI
                    }
                });

                // Delete
                del.setOnAction(e -> {
                    try {
                        taskDAO.delete(t.getTaskId());
                        getListView().getItems().remove(t);
                        // resize this list after deletion
                        if (getListView() == todayTaskList) forceResize(todayTaskList, 3);
                        else if (getListView() == upcomingTaskList) forceResize(upcomingTaskList, 2);
                        else if (getListView() == overdueTaskList) forceResize(overdueTaskList, 2);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showToast("Failed to delete.");
                    }
                });

                setGraphic(root);
                setText(null);
            }
        });
    }

    // -------------------- Auto height helpers --------------------
    private void bindAutoHeight(ListView<?> listView, int minRows) {
        Runnable resize = () -> forceResize(listView, minRows);

        // initial sizing after skin is ready
        Platform.runLater(resize);

        // whenever items change, resize
        listView.getItems().addListener((javafx.collections.ListChangeListener<Object>) c -> resize.run());
    }

    private void forceResize(ListView<?> listView, int minRows) {
        int size = listView.getItems() == null ? 0 : listView.getItems().size();
        int rows = Math.max(minRows, size);
        double pref = rows * listView.getFixedCellSize() + 2; // small padding allowance
        listView.setPrefHeight(pref);
        listView.setMaxHeight(pref); // FXML-এ USE_PREF_SIZE দিলে এটাও মানবে
    }

    // -------------------- Helpers --------------------
    private void showToast(String msg) {
        System.out.println(msg);
    }

    @FXML private void showNotifications() { System.out.println("Notifications clicked (not implemented yet)"); }
    @FXML private void showSearch() { System.out.println("Search clicked (not implemented yet)"); }

}
