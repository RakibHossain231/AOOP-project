package com.example.achievekit;

import javafx.scene.control.Label;

public final class SidebarHelper {
    private SidebarHelper(){}

    public static void bind(Label nameLabel, Label subLabel, Label avatarLabel) {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;

        String display = (u.getFullName() != null && !u.getFullName().isBlank())
                ? u.getFullName() : u.getUsername();

        if (nameLabel != null) nameLabel.setText(display);
        if (subLabel != null) subLabel.setText("Member");

        // First letter of display name (Fallback: U)
        String letter = display.trim().isEmpty() ? "U" :
                display.trim().substring(0,1).toUpperCase();
        if (avatarLabel != null) avatarLabel.setText(letter);
    }
}
