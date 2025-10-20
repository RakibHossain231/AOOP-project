package com.example.achievekit.chat;

import com.example.achievekit.HelloApplication;
import com.example.achievekit.Nav;
import com.example.achievekit.SessionManager;
import com.example.achievekit.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CourseChatController {

    // For Loading page indicator
    @FXML private Button dashboardBtn, pomodoroBtn, tasksBtn, expensesBtn, chatBtn;
    @FXML private Button analyticsBtn, settingsBtn, tipsBtn;

    @FXML private Label titleLabel, statusLabel, courseNameLabel, userLabel;
    @FXML private Button connectBtn, disconnectBtn, sendBtn;
    @FXML private TextField messageField;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane scrollPane;

    private ChatClient client;
    private String username;
    private int courseId;
    private String courseName;

    private boolean soundEnabled = true;   // চাইলে F9 দিয়ে টগল করো
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** window ওপেন করার পর caller এইটা কল করবে */
    public void initCourse(int courseId, String courseName) {
        this.courseId = courseId;
        this.courseName = courseName;
        if (courseNameLabel != null) courseNameLabel.setText(courseName + " (ID: " + courseId + ")");
        if (titleLabel != null) titleLabel.setText("💬 " + courseName + " • Chat");
    }

    /* ---------- Navigation (sidebar) ---------- */
    public void showDashboard() throws IOException { HelloApplication.loadPage("Homepage.fxml"); }
    public void showPomodoro() throws IOException { HelloApplication.loadPage("pomodoroPage.fxml"); }
    public void showExpenses() throws IOException { HelloApplication.loadPage("expense.fxml"); }
    public void showChat() { /* already here */ }
    public void showAnalytics() { System.out.println("Showing Analytics"); }
    public void showTips() { System.out.println("Showing Tips"); }
    public void showTasks() throws IOException { HelloApplication.loadPage("todo.fxml"); }
    public void logout() { System.out.println("Logout requested"); }
    public void openExpensesSettings() { System.out.println("Open settings"); }
    public void showExpensesHelp() { System.out.println("Help"); }
    public void showSettings(){ System.out.println("Settings"); }

    @FXML
    public void initialize() {

        Nav.highlight( chatBtn, dashboardBtn, dashboardBtn, pomodoroBtn, tasksBtn, expensesBtn,  analyticsBtn, settingsBtn, tipsBtn);

        User u = SessionManager.getCurrentUser();
        username = (u != null && u.getUsername()!=null && !u.getUsername().isBlank())
                ? u.getUsername() : "Guest" + (System.currentTimeMillis()%1000);
        if (userLabel != null) userLabel.setText("User: " + username);

        // হেডার স্টাইল
        if (courseNameLabel != null) courseNameLabel.getStyleClass().add("course-head");
        if (userLabel != null) {
            userLabel.getStyleClass().clear();
            userLabel.getStyleClass().add("badge");
        }

        messageField.textProperty().addListener((obs, o, v) ->
                sendBtn.setDisable(v==null || v.isBlank() || client==null || !client.isConnected()));

        // নতুন মেসেজ এলে অটো-স্ক্রল
        scrollPane.vvalueProperty().bind(messagesBox.heightProperty());

        // (ঐচ্ছিক) F9 = সাউন্ড টগল
        Platform.runLater(() -> {
            var sc = messagesBox.getScene();
            if (sc != null) {
                sc.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
                    if (ev.getCode() == javafx.scene.input.KeyCode.F9) {
                        soundEnabled = !soundEnabled;
                        System.out.println("Chat sound: " + (soundEnabled ? "ON" : "OFF"));
                    }
                });
            }
        });
    }

    @FXML
    public void onConnect() {
        if (client!=null && client.isConnected()) return;
        if (courseId<=0) {
            new Alert(Alert.AlertType.ERROR, "Course not set. Call initCourse(courseId, courseName).").show();
            return;
        }
        String room = "course:" + courseId;
        client = new ChatClient("127.0.0.1", 5050, room, username, this::onIncoming);
        try {
            client.connect();
            setStatus(true);
            addSystem("Connected to " + courseName + " (#"+courseId+")");
        } catch (Exception e) {
            setStatus(false);
            addSystem("Connect failed: " + e.getMessage());
        }
    }

    @FXML
    public void onDisconnect() {
        if (client!=null) {
            client.close();
            addSystem("Disconnected.");
        }
        setStatus(false);
    }

    @FXML
    public void onSend() {
        String text = messageField.getText();
        if (text==null || text.isBlank() || client==null || !client.isConnected()) return;

        long now = System.currentTimeMillis();
        Message msg = Message.chat(client.getRoom(), username, text, now);
        client.send(msg);
        messageField.clear();

        // optimistic UI
        addBubble(msg, true);
    }

    /** সার্ভার থেকে ইভেন্ট আসে */
    private void onIncoming(Message msg) {
        switch (msg.getType()) {
            case SYSTEM -> addSystem(msg.getBody());

            case HISTORY -> {
                boolean mine = username.equalsIgnoreCase(nullToEmpty(msg.getSender()));
                addBubble(msg, mine);
                if (!mine) playNewMsgSound(msg.getSender());
            }

            case CHAT -> {
                boolean mine = username.equalsIgnoreCase(nullToEmpty(msg.getSender()));
                if (!mine) {
                    addBubble(msg, false);
                    playNewMsgSound(msg.getSender()); // 🔔 সাউন্ড
                }
                // নিজের echo এলে কিছু করিনি
            }
            default -> {}
        }
    }

    private void addSystem(String s) {
        Platform.runLater(() -> {
            Label lbl = new Label("• " + s);
            lbl.getStyleClass().add("meta");
            messagesBox.getChildren().add(lbl);
        });
    }

    private void addBubble(Message m, boolean mine) {
        Platform.runLater(() -> {
            HBox row = new HBox();
            row.getStyleClass().add("msg-row");
            row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubble = new VBox();
            bubble.getStyleClass().addAll("bubble", mine ? "bubble-me" : "bubble-you");

            Label body = new Label(m.getBody());
            body.getStyleClass().add("body");
            body.setWrapText(true);
            body.setMaxWidth(560);

            String who = mine ? "You" : nullToEmpty(m.getSender());
            String timeStr = formatTime(m.getTimestamp());
            Label small = new Label(who + " • " + timeStr);
            small.getStyleClass().add("meta");

            bubble.getChildren().addAll(body, small);
            bubble.setPadding(new Insets(8, 12, 8, 12));
            row.getChildren().add(bubble);
            messagesBox.getChildren().add(row);
        });
    }

    private void setStatus(boolean connected) {
        Platform.runLater(() -> {
            statusLabel.setText(connected ? "Connected" : "Disconnected");
            connectBtn.setDisable(connected);
            disconnectBtn.setDisable(!connected);
            sendBtn.setDisable(!connected || messageField.getText()==null || messageField.getText().isBlank());
        });
    }

    /** HomeController থেকে উইন্ডো বন্ধের সময় safe shutdown */
    public void shutdown() {
        if (client != null) client.close();
    }

    /* ---------- Sound helpers ---------- */
    private void playNewMsgSound(String sender) {
        if (!soundEnabled) return;
        if (sender != null && sender.equalsIgnoreCase(username)) return; // নিজের মেসেজে সাউন্ড নয়

        // শুধু উইন্ডো unfocused হলে সাউন্ড দিতে চাইলে নীচের 3 লাইন অন করো:
        // var sc = messagesBox.getScene();
        // if (sc != null && sc.getWindow() != null && sc.getWindow().isFocused()) return;

        com.example.achievekit.util.SoundFX.incoming();
    }

    /* ---------- Utils ---------- */
    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String formatTime(long epochMillis) {
        try {
            LocalDateTime ldt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
            return TIME_FMT.format(ldt);
        } catch (Exception e) {
            return TIME_FMT.format(LocalDateTime.now());
        }
    }
}
