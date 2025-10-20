package com.example.achievekit;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public final class SidebarBinder {
    private SidebarBinder(){}

    private static Node find(Parent root, String... selectors) {
        if (root == null || selectors == null) return null;
        for (String sel : selectors) {
            try {
                Node n = root.lookup(sel);
                if (n != null) return n;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static void bind(Parent root) {
        if (root == null) return;
        User u = SessionManager.getCurrentUser();
        if (u == null) return;

        String display = (u.getFullName()!=null && !u.getFullName().isBlank()) ? u.getFullName() : u.getUsername();
        String first = (display==null || display.trim().isEmpty()) ? "U" : display.trim().substring(0,1).toUpperCase();

        // Try by fx:id first, then by styleClass fallback
        Label name = (Label) find(root, "#sidebarName", ".user-name");
        Label sub  = (Label) find(root, "#sidebarSubtext", ".user-status");
        Label av   = (Label) find(root, "#avatarLabel", ".user-avatar");

        if (name != null) name.setText(display);
        if (sub  != null) sub.setText("Member");
        if (av   != null) av.setText(first);

        // প্রোফাইল ব্লক ক্লিক করলে Profile.fxml ওপেন করবে
        Node profileBox = find(root, "#profileBox", ".user-profile");
        if (profileBox != null) {
            profileBox.setOnMouseClicked(e -> {
                try { HelloApplication.loadPage("Profile.fxml"); } catch (Exception ignored) {}
            });
        }

        // লগআউট (fx:id থাকলে)—থাকলে অ্যাটাচ করবে, না থাকলে স্কিপ
        Button logoutBtn = (Button) find(root, "#logoutBtn");
        if (logoutBtn != null) {
            logoutBtn.setOnAction(e -> {
                SessionManager.clear();
                try { HelloApplication.loadPage("login.fxml"); } catch (Exception ignored) {}
            });
        }
    }
}
