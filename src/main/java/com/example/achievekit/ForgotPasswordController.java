package com.example.achievekit;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ForgotPasswordController {

    @FXML private TextField loginField;
    @FXML private Label questionText;
    @FXML private PasswordField answerField;
    @FXML private PasswordField newPassField, confirmPassField;
    @FXML private Button verifyBtn, resetBtn;

    private final UserDao userDao = new UserDao();
    private User loadedUser;        // findByLogin() result cached
    private boolean answerVerified = false;

    @FXML
    private void initialize() {
        setStep2Enabled(false);
        setStep3Enabled(false);
    }

    /* Step 1: load question by username/email */
    @FXML
    private void onLoadQuestion() {
        clearMessages();
        String login = nv(loginField.getText());
        if (login.isEmpty()) { alert(Alert.AlertType.WARNING, "Enter username or email first."); return; }

        try {
            loadedUser = userDao.findByLogin(login);
            if (loadedUser == null) {
                alert(Alert.AlertType.ERROR, "No account found with that username/email.");
                setStep2Enabled(false); setStep3Enabled(false);
                questionText.setText(""); return;
            }

            // question is stored against Username
            String q = userDao.findSecurityQuestionByUsername(loadedUser.getUsername());
            if (q == null || q.isBlank()) {
                alert(Alert.AlertType.ERROR, "No security question set for this user.");
                setStep2Enabled(false); setStep3Enabled(false);
                questionText.setText(""); return;
            }

            questionText.setText(q);
            answerField.clear();
            answerVerified = false;

            setStep2Enabled(true);
            setStep3Enabled(false);
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Failed to load question.");
        }
    }

    /* Step 2: verify answer */
    @FXML
    private void onVerifyAnswer() {
        if (loadedUser == null) { alert(Alert.AlertType.WARNING, "Load your question first."); return; }
        String ans = nv(answerField.getText());
        if (ans.isEmpty()) { alert(Alert.AlertType.WARNING, "Enter your answer."); return; }

        try {
            String hash = userDao.findSecurityAnswerHashByUsername(loadedUser.getUsername());
            if (hash == null || hash.isBlank()) {
                alert(Alert.AlertType.ERROR, "Security info missing for this user.");
                return;
            }
            boolean ok = PasswordUtil.matches(ans.toLowerCase(), hash);
            if (!ok) {
                alert(Alert.AlertType.ERROR, "Incorrect answer.");
                setStep3Enabled(false);
                answerVerified = false;
                return;
            }

            answerVerified = true;
            setStep3Enabled(true);
            alert(Alert.AlertType.INFORMATION, "Answer verified. You can set a new password.");
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Verification failed.");
        }
    }

    /* Step 3: reset password after verification */
    @FXML
    private void resetPassword() {
        if (!answerVerified || loadedUser == null) {
            alert(Alert.AlertType.WARNING, "Verify your answer first.");
            return;
        }

        String p1 = nv(newPassField.getText());
        String p2 = nv(confirmPassField.getText());

        if (p1.length() < 8) { alert(Alert.AlertType.ERROR, "Password must be at least 8 characters."); return; }
        if (!p1.equals(p2)) { alert(Alert.AlertType.ERROR, "Passwords do not match."); return; }

        try {
            int updated = userDao.updatePasswordByUsername(loadedUser.getUsername(), PasswordUtil.hash(p1));
            if (updated == 1) {
                alert(Alert.AlertType.INFORMATION, "Password reset successful. Please log in.");
                HelloApplication.loadPage("login.fxml");
            } else {
                alert(Alert.AlertType.ERROR, "Reset failed. Try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Reset failed: " + e.getMessage());
        }
    }

    @FXML
    private void backToLogin() {
        try { HelloApplication.loadPage("login.fxml"); }
        catch (Exception e) { alert(Alert.AlertType.ERROR, e.getMessage()); }
    }

    /* UI helpers */
    private void setStep2Enabled(boolean enabled) {
        questionText.setDisable(!enabled);
        answerField.setDisable(!enabled);
        verifyBtn.setDisable(!enabled);
    }

    private void setStep3Enabled(boolean enabled) {
        newPassField.setDisable(!enabled);
        confirmPassField.setDisable(!enabled);
        resetBtn.setDisable(!enabled);
    }

    private void clearMessages() { /* placeholder if you add status label */ }
    private String nv(String s){ return s==null? "": s.trim(); }
    private void alert(Alert.AlertType t, String m){ new Alert(t,m).showAndWait(); }
}
