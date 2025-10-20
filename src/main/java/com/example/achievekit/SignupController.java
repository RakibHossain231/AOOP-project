package com.example.achievekit;

import javafx.fxml.FXML;

// UI controls
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

// layout
import javafx.scene.layout.GridPane;

// util
import javafx.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SignupController {

    public Label signupTitle;
    public Label signupLogo;
    public Label signupSubtitle;
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button signupBtn;
    @FXML private Button cancelBtn;

    private final UserDao userDao = new UserDao();
    private static final Pattern EMAIL_RE =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML
    private void onSignupClicked() {
        String username = safe(usernameField.getText());
        String fullName = safe(fullNameField.getText());
        String email    = safe(emailField.getText());
        String pass     = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        try {
            // 1) Basic validations
            if (isBlank(username)) fail("Username required");
            if (isBlank(email) || !EMAIL_RE.matcher(email).matches()) fail("Invalid email");
            if (pass == null || pass.length() < 8) fail("Password must be ≥ 8 characters");
            if (!pass.equals(confirm)) fail("Passwords do not match");

            // 2) Uniqueness
            if (userDao.existsByEmail(email))     fail("Email already in use");
            if (userDao.existsByUsername(username)) fail("Username already in use");

            // 3) Open Security Q/A dialog
            Pair<String,String> qa = showSecurityDialog();
            if (qa == null) return; // user cancelled
            String question = qa.getKey();
            String answer   = qa.getValue();
            if (isBlank(question)) fail("Select a security question");
            if (isBlank(answer))   fail("Security answer required");

            // 4) Hash + persist
            String passwordHash = PasswordUtil.hash(pass);
            String answerHash   = PasswordUtil.hash(answer.trim().toLowerCase()); // case-insensitive compare

            User u = new User(username, email.toLowerCase(), passwordHash,
                    fullName == null ? "" : fullName,
                    question, answerHash);

            int id = userDao.insert(u);

            info("Success", "Account created! (UserID: " + id + ")");
            clear();
            HelloApplication.loadPage("login.fxml");

        } catch (IllegalArgumentException iae) {
            error("Validation", iae.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            error("Error", "Signup failed. Please try again.");
        }
    }

    @FXML
    private void onCancelClicked() {
        try { HelloApplication.loadPage("login.fxml"); }
        catch (Exception e) { cancelBtn.getScene().getWindow().hide(); }
    }

    // ---------- Security Dialog ----------
    private Pair<String,String> showSecurityDialog() {
        Dialog<Pair<String,String>> dlg = new Dialog<>();
        dlg.setTitle("Security Question");
        dlg.setHeaderText("Select a question and provide your answer.\nYou'll use this to reset your password.");

        ButtonType okBtn  = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(okBtn, cancel);

        // UI
        ComboBox<String> questionBox = new ComboBox<>();
        PasswordField answerField = new PasswordField();

        List<String> qs = Arrays.asList(
                "What is your favorite teacher's name?",
                "Where Do you live?",
                "What city were you born in?",
                "What is your first pet's name?",
                "What is your mother's maiden name?"
        );
        questionBox.getItems().addAll(qs);
        questionBox.getSelectionModel().selectFirst();

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.add(new Label("Question:"), 0, 0);
        gp.add(questionBox,            1, 0);
        gp.add(new Label("Answer:"),   0, 1);
        gp.add(answerField,            1, 1);
        dlg.getDialogPane().setContent(gp);

        // Disable OK if answer empty
        final Button ok = (Button) dlg.getDialogPane().lookupButton(okBtn);
        ok.setDisable(true);
        answerField.textProperty().addListener((obs, o, n) -> ok.setDisable(isBlank(n)));

        dlg.setResultConverter(btn -> {
            if (btn == okBtn) return new Pair<>(questionBox.getValue(), answerField.getText());
            return null;
        });

        return dlg.showAndWait().orElse(null);
    }

    // ---------- Helpers ----------
    private static boolean isBlank(String s){ return s == null || s.trim().isEmpty(); }
    private static String safe(String s){ return s == null ? null : s.trim(); }

    private void clear() {
        usernameField.clear();
        fullNameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }

    private void fail(String m) { throw new IllegalArgumentException(m); }
    private void error(String title, String msg) { show(Alert.AlertType.ERROR, title, msg); }
    private void info(String title, String msg) { show(Alert.AlertType.INFORMATION, title, msg); }

    private void show(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
