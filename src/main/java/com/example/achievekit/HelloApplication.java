package com.example.achievekit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

public class HelloApplication extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        Platform.setImplicitExit(true);     // window সব বন্ধ হলে app exit

        loadPage("login.fxml");             // প্রথম পেজ লোড
        stage.setTitle("AchieveKIT");

        // >>> ফুলস্ক্রিন অ্যাপ্লাই (লঞ্চের সময়েই)
        applyFullscreen(stage);

        // উইন্ডো বন্ধ করলে জোর করে শাটডাউন
        stage.setOnCloseRequest(evt -> {
            shutdown();
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    /** এক জায়গা থেকে সব পেজ লোড */
    public static void loadPage(String fxmlName) throws IOException {
        URL url = HelloApplication.class.getResource("/com/example/achievekit/" + fxmlName);
        if (url == null) throw new IllegalStateException("FXML not found: " + fxmlName);

        Parent root = FXMLLoader.load(url);

        // কোন স্টেজে Scene বসাবো তা বের করি
        Stage targetStage = primaryStage;
        if (targetStage == null) {
            Window w = Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
            if (w instanceof Stage s) {
                targetStage = s;
            } else {
                targetStage = new Stage();
                targetStage.show();
            }
        }

        targetStage.setScene(new Scene(root));

        // পেজ বদলালেই ফুলস্ক্রিন অ্যাপ্লাই (সব FXML-এ প্রযোজ্য)
        applyFullscreen(targetStage);

        // সাইডবার (নাম/অ্যাভাটার/লগআউট) অটো-বাইন্ড; না থাকলে ইগনোর
        try { SidebarBinder.bind(root); } catch (Throwable ignored) {}
    }

    /** সবসময় ফুলস্ক্রিন/ম্যাক্সিমাইজ ফোর্স করার হেল্পার */
    public static void applyFullscreen(Stage stage) {
        if (stage == null) return;

        javafx.geometry.Rectangle2D vb = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX(vb.getMinX());
        stage.setY(vb.getMinY());
        stage.setWidth(vb.getWidth());
        stage.setHeight(vb.getHeight());

        // ডেকোরেশনসহ বড় উইন্ডো
        stage.setResizable(true);
        stage.setMaximized(true);

        // ❌ ফুলস্ক্রিন পুরোপুরি অফ রাখছি যাতে উইন্ডো কন্ট্রোল দেখা যায়
        stage.setFullScreen(false);

    }


    @Override
    public void stop() {
        shutdown();
    }

    /** সব ক্লিনআপ এখানে */
    private static void shutdown() {
        // সেশন ক্লিয়ার
        try { SessionManager.clear(); } catch (Throwable ignored) {}

        // 👉 MySQL Connector থাকলে safe shutdown (Reflection; class না থাকলে নীরব)
        // MySQL 8.x: com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown()
        try {
            Class<?> clazz = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            Method m = clazz.getMethod("checkedShutdown");
            m.invoke(null);
        } catch (Throwable ignored) {}

        // MySQL 5.x: com.mysql.jdbc.AbandonedConnectionCleanupThread.shutdown()
        try {
            Class<?> clazz = Class.forName("com.mysql.jdbc.AbandonedConnectionCleanupThread");
            Method m = clazz.getMethod("shutdown");
            m.invoke(null);
        } catch (Throwable ignored) {}
    }

    public static void main(String[] args) { launch(); }
}
