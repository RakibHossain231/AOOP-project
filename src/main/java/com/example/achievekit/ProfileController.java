package com.example.achievekit;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ProfileController {

    @FXML private Label avatarLabel, nameLabel, usernameLabel, emailLabel, joinedLabel, statusLabel;
    @FXML private Button backBtn;

    @FXML
    public void initialize() {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;

        String display = (u.getFullName()!=null && !u.getFullName().isBlank()) ? u.getFullName() : u.getUsername();
        nameLabel.setText(nv(display));
        usernameLabel.setText(nv(u.getUsername()));
        emailLabel.setText(nv(u.getEmail()));
        statusLabel.setText(nv(u.getStatus()));
        joinedLabel.setText(String.valueOf(u.getJoinDate()));

        String letter = display.trim().isEmpty()? "U": display.substring(0,1).toUpperCase();
        avatarLabel.setText(letter);
    }

    @FXML
    private void goBack() {
        try { HelloApplication.loadPage("Homepage.fxml"); }
        catch (Exception ignored) {}
    }

    private String nv(String s){ return (s==null || s.isBlank())? "-" : s; }
}
