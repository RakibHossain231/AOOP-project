package com.example.achievekit;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField usernameField;   // Username বা Email
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMe;
    @FXML private Label errorMsg;
    @FXML private Button loginBtn;

    private final AuthService authService = new AuthService();

    @FXML
    private void onLogin() {
        String login = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass  = passwordField.getText() == null ? "" : passwordField.getText();

        if (login.isEmpty() || pass.isEmpty()) {
            showError("Oops! You need to fill in both Username/Email and Password.");
            return;
        }

        try {
            User u = authService.authenticate(login, pass);
            if (u != null) {
                SessionManager.setCurrentUser(u);
                HelloApplication.loadPage("Homepage.fxml");
            } else {
                showError("Invalid credentials or inactive account.");
            }
        } catch (Exception ex) {
            showError("Login failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onGoSignup() {
        try { HelloApplication.loadPage("signup.fxml"); }
        catch (Exception e){ new Alert(Alert.AlertType.ERROR, "Failed to open Sign up: " + e.getMessage()).showAndWait(); }
    }

    @FXML
    private void onForgotPassword() {
        try { HelloApplication.loadPage("ForgotPassword.fxml"); }
        catch (Exception e){ new Alert(Alert.AlertType.ERROR, "Failed to open Reset page: " + e.getMessage()).showAndWait(); }
    }

    private void showError(String msg) {
        if (errorMsg != null) { errorMsg.setText(msg); errorMsg.setVisible(true); }
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
