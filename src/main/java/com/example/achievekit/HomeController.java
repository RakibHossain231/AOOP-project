package com.example.achievekit;

import com.example.achievekit.chat.ChatWindows;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.*;
import java.text.NumberFormat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Locale;

public class HomeController {

    // ======== Sidebar/nav ========
    @FXML private Button dashboardBtn, pomodoroBtn, tasksBtn, expensesBtn, chatBtn;
    @FXML private Button analyticsBtn, settingsBtn, tipsBtn;

    // ======== Dashboard stats / cards ========
    @FXML private Label pomodoroCount, taskCount, expenseAmount, messageCount;
    @FXML private VBox activityList, chatPreview;

    // ======== Sidebar / profile ========
    @FXML private Label sidebarName;
    @FXML private Label sidebarSubtext;
    @FXML private VBox profileBox;
    @FXML private Button logoutBtn;

    // Auto refresh
    private Timeline autoRefresh;

    @FXML
    public void initialize() {
        Nav.highlight(dashboardBtn, dashboardBtn, pomodoroBtn, tasksBtn, expensesBtn, chatBtn, analyticsBtn, settingsBtn, tipsBtn);

        User u = SessionManager.getCurrentUser();
        if (u != null) {
            String display = (u.getFullName() != null && !u.getFullName().isBlank())
                    ? u.getFullName() : u.getUsername();
            if (sidebarName != null) sidebarName.setText(display);
            if (sidebarSubtext != null) sidebarSubtext.setText("Member");
        }
        if (profileBox != null) profileBox.setOnMouseClicked(e -> openProfile());

        setActiveNav(dashboardBtn);

        // প্রথম লোড + অটো-রিফ্রেশ সেটআপ
        refreshDashboard();
        setupAutoRefresh();
    }

    // ======== Refresh everything on dashboard ========
    private void refreshDashboard() {
        loadDashboardStats();
        loadRecentActivity();
        loadChatPreview();
    }

    private void setupAutoRefresh() {
        // Window focus/Shown হলে রিফ্রেশ
        if (profileBox != null) {
            profileBox.sceneProperty().addListener((obs, oldSc, sc) -> {
                if (sc != null) {
                    sc.windowProperty().addListener((o2, oldWin, win) -> {
                        if (win != null) {
                            win.focusedProperty().addListener((o3, was, is) -> { if (is) refreshDashboard(); });
                            win.setOnShown(e -> refreshDashboard());
                        }
                    });
                }
            });
        }
        // প্রতি 60 সেকেন্ডে রিফ্রেশ (চাইলে কমেন্ট করে দাও/সময় বদলাও)
        if (autoRefresh != null) autoRefresh.stop();
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(60), e -> refreshDashboard()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    // =================== DASHBOARD STATS (exact schema) ===================
    private void loadDashboardStats() {
        User u = SessionManager.getCurrentUser();
        if (u == null) {
            safeSet(pomodoroCount, "0");
            safeSet(taskCount, "0/0");
            safeSet(expenseAmount, "0.00$");
            safeSet(messageCount, "0");
            return;
        }
        int userId = u.getUserId();

        try (Connection con = DB.getConnection()) {

            // 1) Pomodoros Today
            int pomoToday = scalarInt(con,
                    "SELECT COUNT(*) FROM pomodorosessions WHERE UserID=? AND DATE(StartTime)=CURDATE()", userId);
            safeSet(pomodoroCount, String.valueOf(pomoToday));

            // 2) Tasks (done/total)
            int totalTasks = scalarInt(con, "SELECT COUNT(*) FROM tasks WHERE UserID=?", userId);
            int doneTasks  = scalarInt(con, "SELECT COUNT(*) FROM tasks WHERE UserID=? AND Status='completed'", userId);
            safeSet(taskCount, doneTasks + "/" + totalTasks);

            // 3) Daily Spending today
            double spendToday = scalarDouble(con,
                    "SELECT COALESCE(SUM(Amount),0) FROM expenses " +
                            "WHERE UserID=? AND Type='expense' AND ExpenseDate=CURDATE()", userId);
            safeSet(expenseAmount, formatMoneyAfter(spendToday)); // e.g., 45.20$

            // 4) Messages today
            int msgsToday = scalarInt(con,
                    "SELECT COUNT(*) FROM coursemessages m " +
                            "JOIN coursemembers cm ON cm.CourseID = m.CourseID " +
                            "WHERE cm.UserID=? AND DATE(m.SentAt)=CURDATE()", userId);
            safeSet(messageCount, String.valueOf(msgsToday));

        } catch (Exception ex) {
            ex.printStackTrace();
            safeSet(pomodoroCount, "0");
            safeSet(taskCount, "0/0");
            safeSet(expenseAmount, "0.00$");
            safeSet(messageCount, "0");
            new Alert(Alert.AlertType.WARNING,
                    "Couldn't load dashboard stats from database.\n\nError: " + ex.getMessage())
                    .showAndWait();
        }
    }

    // =================== RECENT ACTIVITY (exact columns) ===================
    private void loadRecentActivity() {
        if (activityList == null) return;
        activityList.getChildren().clear();

        User u = SessionManager.getCurrentUser();
        if (u == null) return;
        int userId = u.getUserId();

        List<ActivityItem> items = new ArrayList<>();

        try (Connection con = DB.getConnection()) {

            // Pomodoro: latest 1 (StartTime)
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT StartTime AS ts FROM pomodorosessions " +
                            "WHERE UserID=? ORDER BY StartTime DESC LIMIT 1")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        LocalDateTime ts = toLdt(rs.getTimestamp("ts"));
                        items.add(new ActivityItem("🍅", "Completed Pomodoro session", ts));
                    }
                }
            }

            // Task (completed): latest 1 (UpdatedAt)
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT Title, UpdatedAt AS ts FROM tasks " +
                            "WHERE UserID=? AND Status='completed' " +
                            "ORDER BY UpdatedAt DESC LIMIT 1")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String title = nvl(rs.getString("Title"), "a task");
                        LocalDateTime ts = toLdt(rs.getTimestamp("ts"));
                        items.add(new ActivityItem("✅", "Completed '" + title + "' ", ts));
                    }
                }
            }

            // Expense: latest 1 (CreatedAt)
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT Description, Amount, CreatedAt AS ts FROM expenses " +
                            "WHERE UserID=? AND Type='expense' " +
                            "ORDER BY CreatedAt DESC LIMIT 1")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String desc = nvl(rs.getString("Description"), "Expense");
                        double amt  = rs.getDouble("Amount");
                        LocalDateTime ts = toLdt(rs.getTimestamp("ts"));
                        items.add(new ActivityItem("💰", "Added expense: '" + desc + " - " + formatMoneyBefore(amt) +"'", ts));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Mix করে time-desc, শুধু 3টা দেখাও
        items.sort(Comparator.comparing((ActivityItem a) -> a.time).reversed());
        int limit = Math.min(3, items.size());
        for (int i = 0; i < limit; i++) {
            activityList.getChildren().add(renderActivity(items.get(i)));
        }
    }

    private static class ActivityItem {
        final String icon, text; final LocalDateTime time;
        ActivityItem(String icon, String text, LocalDateTime time) { this.icon=icon; this.text=text; this.time=time; }
    }

    private Node renderActivity(ActivityItem it) {
        HBox row = new HBox();
        row.getStyleClass().add("activity-item");
        row.setSpacing(10);

        Label ic = new Label(it.icon);
        ic.getStyleClass().add("activity-icon");

        VBox texts = new VBox();
        texts.setFillWidth(true);
        Label t1 = new Label(it.text);
        t1.getStyleClass().add("activity-text");
        Label t2 = new Label(timeAgo(it.time));
        t2.getStyleClass().add("activity-time");
        texts.getChildren().addAll(t1, t2);
        HBox.setHgrow(texts, Priority.ALWAYS);

        row.getChildren().addAll(ic, texts);
        return row;
    }

    // =================== STUDY CHAT PREVIEW ===================
    // =================== STUDY CHAT PREVIEW (latest course → last 3 messages) ===================
    private void loadChatPreview() {
        if (chatPreview == null) return;
        chatPreview.getChildren().clear();

        User u = SessionManager.getCurrentUser();
        if (u == null) return;
        int userId = u.getUserId();

        Integer latestCourseId = null;
        String latestCourseName = null;

        // 1) ইউজারের কোর্সগুলোর মধ্যে যেটায় সর্বশেষ মেসেজ আছে সেটা খুঁজে বের করি
        String pickCourseSql =
                "SELECT m.CourseID, c.CourseName, MAX(m.SentAt) AS lastAt " +
                        "FROM coursemessages m " +
                        "JOIN coursemembers cm ON cm.CourseID = m.CourseID " +
                        "JOIN courses c ON c.CourseID = m.CourseID " +
                        "WHERE cm.UserID=? " +
                        "GROUP BY m.CourseID, c.CourseName " +
                        "ORDER BY lastAt DESC " +
                        "LIMIT 1";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(pickCourseSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    latestCourseId = rs.getInt("CourseID");
                    latestCourseName = nvl(rs.getString("CourseName"), "Course #" + latestCourseId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (latestCourseId == null) {
            Label empty = new Label("No recent messages from your courses.");
            empty.getStyleClass().add("chat-time");
            chatPreview.getChildren().add(empty);
            return;
        }

        // 2) ওই কোর্সের শেষ 3টা মেসেজ (JOIN ঠিক করা হয়েছে: SenderID)
        String msgsSql =
                "SELECT m.Content, m.SentAt, " +
                        "       COALESCE(us.FullName, us.Username, CONCAT('User #', m.SenderID)) AS sender " +
                        "FROM coursemessages m " +
                        "LEFT JOIN users us ON us.UserID = m.SenderID " +
                        "WHERE m.CourseID=? " +
                        "ORDER BY m.SentAt DESC " +
                        "LIMIT 3";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(msgsSql)) {
            ps.setInt(1, latestCourseId);

            // Header: কোন কোর্স থেকে দেখাচ্ছি
            Label header = new Label("Latest in: " + latestCourseName);
            header.getStyleClass().add("chat-time");
            chatPreview.getChildren().add(header);

            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    String sender = nvl(rs.getString("sender"), "Unknown");
                    String content = nvl(rs.getString("Content"), "");
                    LocalDateTime sent = toLdt(rs.getTimestamp("SentAt"));
                    chatPreview.getChildren().add(renderChatRow(sender, content, sent));
                }
                if (!any) {
                    Label empty = new Label("No messages yet in this course.");
                    empty.getStyleClass().add("chat-time");
                    chatPreview.getChildren().add(empty);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Node renderChatRow(String senderName, String content, LocalDateTime when) {
        HBox row = new HBox();
        row.getStyleClass().add("chat-message");
        row.setSpacing(10);

        Label avatar = new Label(initials(senderName));
        avatar.getStyleClass().add("chat-avatar");

        VBox texts = new VBox();
        texts.setFillWidth(true);
        Label t1 = new Label(senderName + ": " + content);
        t1.getStyleClass().add("chat-text");
        Label t2 = new Label(timeAgo(when));
        t2.getStyleClass().add("chat-time");

        texts.getChildren().addAll(t1, t2);
        HBox.setHgrow(texts, Priority.ALWAYS);

        row.getChildren().addAll(avatar, texts);
        return row;
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "U";
        String[] parts = name.trim().split("\\s+");
        String a = parts[0].substring(0,1).toUpperCase(Locale.ROOT);
        String b = parts.length>1 ? parts[1].substring(0,1).toUpperCase(Locale.ROOT) : "";
        return (a + b).trim();
    }

    // =================== Helpers ===================
    private void safeSet(Label l, String txt) { if (l != null) l.setText(txt); }

    // If you want "$45.20" instead, switch to formatMoneyBefore()
    private String formatMoneyAfter(double v) { return String.format(Locale.US, "%.2f$", v); }
    private String formatMoneyBefore(double v) { return NumberFormat.getCurrencyInstance(Locale.US).format(v); }

    private int scalarInt(Connection con, String sql, Object... params) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }
    private double scalarDouble(Connection con, String sql, Object... params) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0.0; }
        }
    }
    private void bind(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }

    private static String nvl(String s, String def){ return (s==null || s.isBlank()) ? def : s; }
    private static LocalDateTime toLdt(Timestamp ts){
        return ts == null ? null : LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
    }
    private static String timeAgo(LocalDateTime when){
        if (when == null) return "";
        java.time.Duration d = java.time.Duration.between(when, LocalDateTime.now());
        long sec = Math.max(1, d.getSeconds());
        if (sec < 60) return sec + " sec ago";
        long min = sec / 60;
        if (min < 60) return min + " min ago";
        long hr = min / 60;
        if (hr < 24) return hr + " hour" + (hr > 1 ? "s" : "") + " ago";
        long day = hr / 24;
        return day + " day" + (day > 1 ? "s" : "") + " ago";
    }


    // ======== Active nav helper ========
    private void setActiveNav(Button active) {
        Button[] all = new Button[]{ dashboardBtn, pomodoroBtn, tasksBtn, expensesBtn,
                chatBtn, analyticsBtn, settingsBtn, tipsBtn };
        for (Button b : all) { if (b != null) b.getStyleClass().remove("nav-item-active"); }
        if (active != null && !active.getStyleClass().contains("nav-item-active")) {
            active.getStyleClass().add("nav-item-active");
        }
    }

    // ======== Profile dialog ========
    private void openProfile() {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;
        String info = String.format(
                "Full Name : %s%nUsername  : %s%nEmail     : %s%nJoined    : %s%nStatus    : %s",
                ns(u.getFullName()), ns(u.getUsername()), ns(u.getEmail()),
                String.valueOf(u.getJoinDate()), ns(u.getStatus())
        );
        new Alert(Alert.AlertType.INFORMATION, info).showAndWait();
    }
    private String ns(String s){ return (s==null || s.isBlank()) ? "-" : s; }

    // ======== Sidebar navigation handlers ========
    public void showDashboard() {
        // Dashboard বাটনে চাপলে সাথে সাথে রিফ্রেশ
        refreshDashboard();
        setActiveNav(dashboardBtn);
    }
    public void showPomodoro()  { navigateTo("pomodoroPage.fxml"); }
    public void showTasks()     { navigateTo("todo.fxml"); }
    public void showExpenses()  { navigateTo("expense.fxml"); }
    public void showAnalytics() { System.out.println("Showing Analytics"); }
    public void showTips()      { System.out.println("Showing Tips"); }
    public void showSettings()  { System.out.println("Showing settings"); }
    public void showChat()      { openCourseChatPicker(); }

    // Cards / header actions
    public void manageTasks()   { navigateTo("todo.fxml"); }
    public void startPomodoro() { navigateTo("pomodoroPage.fxml"); }
    public void addExpense()    { navigateTo("expense.fxml"); }
    public void showNotifications(){ System.out.println("Showing notifications"); }
    public void showSearch(){ System.out.println("Showing search"); }
    public void joinChat(){ openCourseChatPicker(); }

    // ======== Logout ========
    public void showlogout() { performLogout(); }
    public void logout(){ performLogout(); }

    private void performLogout() {
        try {
            SessionManager.clear();
            try { HelloApplication.loadPage("login.fxml"); return; } catch (Exception ignore) {}

            Stage stage = (Stage) (logoutBtn != null ? logoutBtn.getScene().getWindow()
                    : (sidebarName != null ? sidebarName.getScene().getWindow() : null));
            if (stage != null) {
                Parent root;
                try { root = FXMLLoader.load(getClass().getResource("/com/example/achievekit/login.fxml")); }
                catch (Exception e2) { root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml")); }
                stage.setScene(new Scene(root));
                stage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Logout failed: " + e.getMessage()).showAndWait();
        }
    }

    // ======== Navigation (fallback-friendly) ========
    private void navigateTo(String fxmlName) {
        try {
            try { HelloApplication.loadPage(fxmlName); return; } catch (Exception ignore) {}

            Stage stage = getAnyStageSafe();
            if (stage == null) throw new IllegalStateException("Stage not found to navigate.");

            Parent root = FXMLLoader.load(getClass().getResource("/com/example/achievekit/" + fxmlName));
            stage.setScene(new Scene(root));
            stage.setTitle("AchieveKIT • " + prettyTitleFromFile(fxmlName));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load: " + fxmlName + "\n" + ex.getMessage()).showAndWait();
        }
    }

    private Stage getAnyStageSafe() {
        try {
            if (logoutBtn != null && logoutBtn.getScene() != null) return (Stage) logoutBtn.getScene().getWindow();
            if (sidebarName != null && sidebarName.getScene() != null) return (Stage) sidebarName.getScene().getWindow();
        } catch (Exception ignored) {}
        return null;
    }

    private String prettyTitleFromFile(String fxml) {
        String base = fxml.endsWith(".fxml") ? fxml.substring(0, fxml.length() - 5) : fxml;
        switch (base.toLowerCase()) {
            case "expense": return "Expense";
            case "todo": return "Tasks";
            case "pomodoropage": return "Pomodoro";
            case "studychat": return "Study Chat";
            case "login": return "Login";
            default: return Character.toUpperCase(base.charAt(0)) + base.substring(1);
        }
    }

    /* =========================
       Study Chat: Course Picker
       ========================= */
    private List<CourseRow> fetchAllCourses() {
        List<CourseRow> list = new ArrayList<>();
        String sql = "SELECT CourseID, CourseName, CourseCode, Semester FROM courses ORDER BY CourseName LIMIT 200";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new CourseRow(
                        rs.getInt("CourseID"),
                        rs.getString("CourseName"),
                        rs.getString("CourseCode"),
                        rs.getInt("Semester")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load courses:\n" + e.getMessage()).showAndWait();
        }
        return list;
    }

    private void openCourseChatPicker() {
        List<CourseRow> courses = fetchAllCourses();
        if (courses.isEmpty()) {
            TextInputDialog td = new TextInputDialog();
            td.setTitle("Open Study Chat");
            td.setHeaderText("No courses found in database.");
            td.setContentText("Enter a CourseID to open chat:");
            td.showAndWait().ifPresent(txt -> {
                try { int cid = Integer.parseInt(txt.trim()); ChatWindows.openCourseChat(cid, "Course #" + cid); }
                catch (NumberFormatException e) { new Alert(Alert.AlertType.WARNING, "Invalid CourseID.").showAndWait(); }
            });
            return;
        }

        Stage d = new Stage();
        d.initOwner(getAnyStageSafe());
        d.initModality(javafx.stage.Modality.WINDOW_MODAL);
        d.setTitle("Open Study Chat (Select one or more courses)");

        Label title = new Label("Select courses to open chat:");
        title.setStyle("-fx-text-fill: #fbbf24;           /* gold/yellow text */\n" +
                "    -fx-font-size: 16px;              /* slightly larger than normal text */\n" +
                "    -fx-font-weight: bold;            /* make it bold */\n" +
                "    -fx-padding: 0 0 10 0;            /* add some space below */\n" +
                "    -fx-alignment: center-left;       /* align text to left */\n" +
                "    -fx-font-family: \"Segoe UI\", Arial, sans-serif; /* modern font */");

        ListView<CourseRow> lv = new ListView<>();
        lv.setItems(javafx.collections.FXCollections.observableArrayList(courses));
        lv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lv.setPrefSize(560, 380);
        lv.getStyleClass().add("ak-list");
        lv.setStyle("-fx-background-color: #1f2937;           /* dark gray background */\n" +
                "    -fx-control-inner-background: #111827;   /* inner area of ListView */\n" +
                "    -fx-border-color: rgba(255,193,7,0.3);   /* subtle gold border */\n" +
                "    -fx-border-radius: 8;\n" +
                "    -fx-background-radius: 8;");
        // the function below applies to cells / list rows
//        lv.setCellFactory(listView -> new ListCell<CourseRow>() {
//            @Override
//            protected void updateItem(CourseRow item, boolean empty) {
//                super.updateItem(item, empty);
//                if (empty || item == null) {
//                    setText(null);
//                    setStyle(""); // reset style
//                } else {
//                    setText(item.displayName());
//                    setStyle(
//                            "-fx-text-fill: #d1d5db;" +                  // light gray text
//                                    "-fx-font-size: 13px;" +
//                                    "-fx-padding: 8 12;" +
//                                    "-fx-background-color: transparent;"
//                    );
//
//                    // hover effect
//                    hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
//                        if (isNowHovered) setStyle("-fx-background-color: rgba(255,193,7,0.1); -fx-text-fill: #d1d5db; -fx-font-size: 13px; -fx-padding: 8 12;");
//                        else setStyle("-fx-background-color: transparent; -fx-text-fill: #d1d5db; -fx-font-size: 13px; -fx-padding: 8 12;");
//                    });
//
//                    // selected effect
//                    selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
//                        if (isNowSelected) setStyle("-fx-background-color: rgba(219,165,15,1); -fx-text-fill: #08fcf0; -fx-font-size: 13px; -fx-padding: 8 12;");
//                    });
//                }
//            }
//        });
        lv.setCellFactory(listView -> new ListCell<CourseRow>() {
            @Override
            protected void updateItem(CourseRow item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle(""); // reset style
                } else {
                    setText(item.displayName());

                    // Determine base style
                    String baseStyle = "-fx-font-size: 13px; -fx-padding: 8 12;";

                    if (isSelected()) {
                        // selected cell style
                        setStyle(baseStyle +
                                "-fx-background-color: rgba(219,165,15,1);" + // gold background
                                "-fx-text-fill: #000000;");                  // black text
                    } else if (isHover()) {
                        // hover style
                        setStyle(baseStyle +
                                "-fx-background-color: rgba(255,193,7,0.1);" + // subtle hover gold
                                "-fx-text-fill: #d1d5db;");                   // light gray text
                    } else {
                        // normal style
                        setStyle(baseStyle +
                                "-fx-background-color: transparent;" +
                                "-fx-text-fill: #d1d5db;");                  // light gray text
                    }

                    // Optional: ensure hover repaint triggers style update
                    hoverProperty().addListener((obs, oldVal, newVal) -> updateItem(item, empty));
                }
            }
        });


        Button openBtn = new Button("Open Chat");
        Button cancelBtn = new Button("Cancel");
        openBtn.setStyle("-fx-background-color: #fbbf24;   /* gold */\n" +
                "    -fx-text-fill: #111827;          /* dark text */\n" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-font-size: 11px;\n" +
                "    -fx-background-radius: 8;\n" +
                "    -fx-padding: 8 20;\n" +
                "    -fx-border-color: transparent;\n" +
                "    -fx-cursor: hand;");
        cancelBtn.setStyle("-fx-background-color: transparent;        /* transparent background */\n" +
                "    -fx-text-fill: #fbbf24;                  /* gold text */\n" +
                "    -fx-border-color: #fbbf24;               /* gold border */\n" +
                "    -fx-border-width: 2;\n" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-font-size: 14px;\n" +
                "    -fx-background-radius: 8;\n" +
                "    -fx-border-radius: 8;\n" +
                "    -fx-padding: 8 20;\n" +
                "    -fx-cursor: hand;");

        openBtn.setDefaultButton(true);

        openBtn.setOnAction(e -> {
            var sel = lv.getSelectionModel().getSelectedItems();
            if (sel == null || sel.isEmpty()) return;
            sel.forEach(c -> ChatWindows.openCourseChat(c.courseId, c.displayName()));
            d.close();
        });
        cancelBtn.setOnAction(e -> d.close());

        HBox actions = new HBox(10, openBtn, cancelBtn);
        actions.setPadding(new Insets(10));
        actions.setStyle("-fx-alignment: center-right;");

        VBox root = new VBox(10, title, lv, actions);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("dialog-root");

        Scene sc = new Scene(root);
        try {
            var base = getClass().getResource("/com/example/achievekit/chat.css");
            var pick = getClass().getResource("/com/example/achievekit/picker.css");
            if (base != null) sc.getStylesheets().add(base.toExternalForm());
            if (pick != null) sc.getStylesheets().add(pick.toExternalForm());
        } catch (Exception ex) { ex.printStackTrace(); }

        openBtn.getStyleClass().setAll("ak-btn", "primary-btn");
        cancelBtn.getStyleClass().setAll("ak-btn", "ghost-btn");

        d.setScene(sc);
        d.centerOnScreen();
        d.show();
    }

    public void getOpenCourseChatPicker() { openCourseChatPicker(); }

    private static class CourseRow {
        final int courseId; final String courseName; final String code; final int semester;
        CourseRow(int id, String name, String code, int sem){
            this.courseId=id; this.courseName=name; this.code=code; this.semester=sem;
        }
        String displayName() {
            String tag = (code!=null && !code.isBlank()? code : "Course");
            return courseName + " (" + tag + ")";
        }
        @Override public String toString() {
            String tag = (code!=null && !code.isBlank()? code : "Course");
            return "• " + courseName + "  —  " + tag + "  [Sem " + semester + "]";
        }
    }
}
