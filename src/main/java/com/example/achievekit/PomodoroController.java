package com.example.achievekit;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import com.example.achievekit.util.SoundFX;
import com.example.achievekit.util.SoundForPomodoro;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static javax.swing.text.html.HTML.Tag.S;

/** Pomodoro controller that keeps state across navigation using PomodoroState singleton. */
public class PomodoroController implements PomodoroState.UiListener {

    // For Loading page indicator
    @FXML
    private Button dashboardBtn, pomodoroBtn, tasksBtn, expensesBtn, chatBtn;
    @FXML
    private Button analyticsBtn, settingsBtn, tipsBtn;

    @FXML
    private Label selectedTasksLabel;

    @FXML
    private Button confirmGroupBtn;

    // ---------- FXML ----------
    @FXML
    private Label sessionTypeLabel, sessionNumberLabel, timerLabel;
    @FXML
    private ProgressBar timerProgress;
    @FXML
    private Button startPauseBtn, stopBtn, resetBtn, skipSessionBtn;
    @FXML
    private CheckBox autoStartCheckbox;
    @FXML
    private Label focusDurationLabel, shortBreakLabel, longBreakLabel, currentTaskLabel;
    @FXML
    private ComboBox<Task> taskSelector;
    @FXML private CheckBox groupTaskModeCheckbox;
    @FXML private ListView<Task> groupTaskListView;

    // Group Task internal state
    private final java.util.List<Task> groupTaskQueue = new java.util.ArrayList<>();
    private int currentGroupTaskIndex = 0;

    @FXML
    private TableView<HistoryItem> sessionHistoryTable;
    @FXML
    private TableColumn<HistoryItem, String> sessionTypeColumn, statusColumn, durationColumn, startTimeColumn, endTimeColumn;

    // ---------- Singletons/DAO ----------
    private final PomodoroState S = PomodoroState.get();
    private final TaskDAO taskDAO = new TaskDAO();

    // ---------- State for phase-complete detection ----------
    private int lastRemaining = -1;
    private final ExecutorService bg = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pomodoro-db");
        t.setDaemon(true);
        return t;
    });

    @FXML
    public void initialize() {

        Nav.highlight(pomodoroBtn, dashboardBtn, dashboardBtn, tasksBtn, expensesBtn, chatBtn, analyticsBtn, settingsBtn, tipsBtn);

        try {
            // 1) History table
            setupHistoryTable();

            // 2) User settings
            ensureSettingsTable();
            loadUserSettingsIntoState();

            // 3) Duration labels + manual input
            updateDurationLabels();
            enableManualInput(focusDurationLabel, "Focus minutes", S::setFocusMinutes);
            enableManualInput(shortBreakLabel, "Short break minutes", S::setShortBreakMinutes);
            enableManualInput(longBreakLabel, "Long break minutes", S::setLongBreakMinutes);

            // 4) Auto-start checkbox (bind + persist)
            if (autoStartCheckbox != null) {
                autoStartCheckbox.setSelected(S.isAutoStart());
                autoStartCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    S.setAutoStart(newVal);
                    saveUserSettingsFromState();
                });
            }

            // 5) Load tasks & style combobox
            loadTasks();
            styleTaskSelector();

            // --- Group Task Mode: ListView + label + confirm button ---
            if (groupTaskListView != null && taskSelector != null) {
                groupTaskListView.setItems(taskSelector.getItems());
                groupTaskListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

                groupTaskListView.setCellFactory(list -> new ListCell<>() {
                    @Override
                    protected void updateItem(Task task, boolean empty) {
                        super.updateItem(task, empty);
                        setText(empty || task == null ? null : task.getTitle() + " (" + task.getPriority() + ")");
                    }
                });

                if (selectedTasksLabel != null) selectedTasksLabel.setVisible(false);
                if (confirmGroupBtn != null)   { confirmGroupBtn.setVisible(false); confirmGroupBtn.setDisable(true); }

                // Update label + show/hide confirm button on selection
                groupTaskListView.getSelectionModel().getSelectedItems()
                        .addListener((ListChangeListener<Task>) ch -> {
                            var selected = groupTaskListView.getSelectionModel().getSelectedItems();

                            boolean hasSelection = selected != null && !selected.isEmpty();
                            if (selectedTasksLabel != null) {
                                selectedTasksLabel.setVisible(hasSelection);
                                selectedTasksLabel.setText(hasSelection
                                        ? "Selected Tasks: " + selected.stream().map(Task::getTitle).collect(Collectors.joining(", "))
                                        : "");
                            }

                            if (confirmGroupBtn != null) {
                                boolean groupMode = groupTaskModeCheckbox != null && groupTaskModeCheckbox.isSelected();
                                confirmGroupBtn.setVisible(groupMode && hasSelection);
                                // keep disabled if timer running or session finished
                                confirmGroupBtn.setDisable(S.isRunning() || S.sessionFinished || !hasSelection);
                            }
                        });
            }

            // Toggle group mode → show/hide list/label/button
            if (groupTaskModeCheckbox != null) {
                groupTaskModeCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                    if (groupTaskListView == null) return;

                    groupTaskListView.setVisible(newV);
                    if (selectedTasksLabel != null) selectedTasksLabel.setVisible(newV && !groupTaskListView.getSelectionModel().getSelectedItems().isEmpty());
                    if (confirmGroupBtn != null) {
                        boolean hasSelection = groupTaskListView.getSelectionModel().getSelectedItems() != null
                                && !groupTaskListView.getSelectionModel().getSelectedItems().isEmpty();
                        confirmGroupBtn.setVisible(newV && hasSelection);
                        confirmGroupBtn.setDisable(S.isRunning() || S.sessionFinished || !hasSelection);
                    }

                    if (!newV) {
                        groupTaskListView.getSelectionModel().clearSelection();
                        groupTaskQueue.clear();
                        currentGroupTaskIndex = 0;
                        if (selectedTasksLabel != null) { selectedTasksLabel.setVisible(false); selectedTasksLabel.setText(""); }
                    } else {
                        Platform.runLater(groupTaskListView::requestFocus);
                    }
                });
            }

            // --- Single-task combobox behavior ---
            if (taskSelector != null) {
                taskSelector.valueProperty().addListener((obs, oldTask, newTask) -> {
                    if (newTask == null) {
                        S.currentTaskId = null;
                        if (currentTaskLabel != null) currentTaskLabel.setText("No task selected");
                        return;
                    }

                    boolean phaseStarted   = (S.phaseStartAt != null);
                    boolean runningOrOpen  = S.isRunning() || phaseStarted || (S.currentCycleId != null);

                    if (runningOrOpen && !S.sessionFinished) {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Switch Task?");
                        confirm.setHeaderText("You're in the middle of a Pomodoro.");
                        confirm.setContentText("Switch to \"" + newTask.getTitle() + "\"? Current progress will be saved as interrupted.");
                        ButtonType yes = new ButtonType("Switch", ButtonBar.ButtonData.OK_DONE);
                        ButtonType no  = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                        confirm.getButtonTypes().setAll(yes, no);

                        if (confirm.showAndWait().orElse(no) == yes) {
                            pauseTimer();
                            completeCurrentPhase("interrupted", "interrupted");
                            S.currentTaskId = newTask.getTaskId();
                            if (currentTaskLabel != null) currentTaskLabel.setText(newTask.getTitle());
                            S.phaseStartAt = java.time.LocalDateTime.now();
                            if (S.isAutoStart()) startTimer();
                            refreshUI();
                        } else {
                            Platform.runLater(() -> taskSelector.getSelectionModel().select(oldTask));
                        }
                    } else {
                        S.currentTaskId = newTask.getTaskId();
                        if (currentTaskLabel != null) currentTaskLabel.setText(newTask.getTitle());
                    }
                });
            }

            // Attach controller and bring UI up to date
            S.attach(this);
            Platform.runLater(this::refreshUI);
            Platform.runLater(this::restoreTimerView);
            loadTodayHistoryFromDb();

        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Initialization Error");
            a.setHeaderText("Failed to initialize Pomodoro page");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }


    @FXML
    private void handleGroupStart() {
        if (groupTaskListView == null) return;

        var selectedTasks = groupTaskListView.getSelectionModel().getSelectedItems();
        if (selectedTasks == null || selectedTasks.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select at least one task to start.").show();
            return;
        }
        if (selectedTasks.size() > 10) {
            new Alert(Alert.AlertType.WARNING, "You can select up to 10 tasks only.").show();
            return;
        }

        String taskNames = selectedTasks.stream().map(Task::getTitle).collect(Collectors.joining(", "));
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Group Tasks");
        confirm.setHeaderText("You’ve selected these tasks:");
        confirm.setContentText(taskNames + "\n\nStart Pomodoro sessions in this order?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        // Queue tasks in the chosen order
        groupTaskQueue.clear();
        groupTaskQueue.addAll(selectedTasks);
        currentGroupTaskIndex = 0;

        // Set first task
        Task first = groupTaskQueue.get(0);
        S.currentTaskId = first.getTaskId();
        if (currentTaskLabel != null) currentTaskLabel.setText(first.getTitle());

        // Disable the confirm button while the timer is running
        if (confirmGroupBtn != null) confirmGroupBtn.setDisable(true);

        // Start!
        startTimer();
    }

    /**
     * Persist the elapsed portion of the current phase as "interrupted" for a specific task.
     * Does NOT advance phase, does NOT change remainingSeconds.
     */
    private void persistElapsedForCurrentTaskAsInterrupted(Integer taskId) {
        try {
            LocalDateTime end = LocalDateTime.now();
            if (S.phaseStartAt == null) return;  // nothing elapsed

            long actualSec = java.time.Duration.between(S.phaseStartAt, end).getSeconds();
            int actualMin  = Math.max(1, (int)Math.round(actualSec / 60.0));

            int plannedMin = switch (S.getPhase()) {
                case FOCUS -> S.getFocusMinutes();
                case SHORT_BREAK -> S.getShortBreakMinutes();
                case LONG_BREAK -> S.getLongBreakMinutes();
            };

            // Save the partial cycle to DB (status = interrupted) under the provided taskId
            bg.submit(() -> {
                try {
                    // You already have persistCycle(...). We reuse it:
                    persistCycle(plannedMin, actualMin, S.phaseStartAt, end, "interrupted");

                    // If your persistCycle uses S.currentTaskId internally, you can temporarily set:
                    Integer old = S.currentTaskId;
                    S.currentTaskId = taskId;
                    persistCycle(plannedMin, actualMin, S.phaseStartAt, end, "interrupted");
                    S.currentTaskId = old;
                } catch (Exception ignored) {}
            });

            // IMPORTANT: keep S.phaseStartAt as-is so remaining time continues from now.
            // If you want more precise accounting, reset stamp so the next elapsed slice starts fresh:
            S.phaseStartAt = LocalDateTime.now();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Rebind UI to the existing PomodoroState without resetting anything.
     */
    private void restoreTimerView() {
        // DO NOT reapply or recalc the phase — just show the live values
        if (timerLabel != null) timerLabel.setText(formatTime(S.getRemainingSeconds()));

        if (timerProgress != null) {
            int total = S.getTotalSeconds();
            int rem = S.getRemainingSeconds();
            timerProgress.setProgress(total == 0 ? 0 : (double) (total - rem) / total);
        }

        if (sessionTypeLabel != null)
            sessionTypeLabel.setText(capitalizeWords(S.getPhase().name().replace('_', ' ')));

        if (sessionNumberLabel != null)
            sessionNumberLabel.setText("Session " + S.getFocusIndex() + " of 4");

        if (startPauseBtn != null)
            startPauseBtn.setText(S.isRunning() ? "⏸ Pause" : "▶ Start");

        // restore the task label
        if (currentTaskLabel != null) {
            String txt = "No task selected";
            if (S.currentTaskId != null && taskSelector != null) {
                Task sel = taskSelector.getItems().stream()
                        .filter(t -> t.getTaskId() == S.currentTaskId)
                        .findFirst().orElse(null);
                if (sel != null) {
                    txt = sel.getTitle();
                    taskSelector.getSelectionModel().select(sel);
                }
            }
            currentTaskLabel.setText(txt);
        }
    }


    // ---------- Table ----------
    private void setupHistoryTable() {
        if (sessionHistoryTable == null) return;
        sessionTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        sessionHistoryTable.setItems(javafx.collections.FXCollections.observableArrayList());
        sessionHistoryTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(HistoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                    return;
                }
                String status = item.getStatus().toLowerCase(Locale.ROOT);
                String type = item.getType().toLowerCase(Locale.ROOT);
                if (status.contains("skipped")) setStyle("-fx-background-color: rgba(255,0,0,0.12);");
                else if (type.contains("short")) setStyle("-fx-background-color: rgba(0,170,255,0.10);");
                else if (type.contains("long")) setStyle("-fx-background-color: rgba(150,100,255,0.10);");
                else if (status.contains("completed")) setStyle("-fx-background-color: rgba(0,255,100,0.12);");
                else setStyle("");
            }
        });
    }

    // ---------- Tasks ----------
    private void loadTasks() {
        try {
            if (!SessionManager.isLoggedIn() || taskSelector == null) return;
            int uid = SessionManager.getCurrentUser().getUserId();
            List<Task> tasks = taskDAO.listByUser(uid).stream()
                    .filter(t -> !"completed".equalsIgnoreCase(t.getStatus()))
                    .toList();
            taskSelector.getItems().setAll(tasks);
            taskSelector.setPromptText("Select a task...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void styleTaskSelector() {
        if (taskSelector == null) return;
        taskSelector.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                    return;
                }
                String pr = task.getPriority() == null ? "MEDIUM" : task.getPriority().toUpperCase();
                setText(task.getTitle() + " (" + pr + ")");
                switch (pr.toLowerCase()) {
                    case "high" -> setTextFill(Color.RED);
                    case "medium" -> setTextFill(Color.ORANGE);
                    case "low" -> setTextFill(Color.LIGHTGREEN);
                    default -> setTextFill(Color.WHITE);
                }
            }
        });
        taskSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                setText(empty || task == null ? "Select a task..." : task.getTitle());
            }
        });
    }


    // ---------- Manual minutes ----------
    private interface MinutesSetter {
        void set(int m);
    }

    private void enableManualInput(Label label, String title, MinutesSetter setter) {
        if (label == null) return;
        label.setOnMouseClicked(ev -> {
            TextInputDialog d = new TextInputDialog(label.getText().replace(" min", ""));
            d.setTitle(title);
            d.setHeaderText(null);
            d.setContentText("Enter minutes (1..600):");
            d.showAndWait().ifPresent(txt -> {
                try {
                    if (txt == null || txt.trim().isEmpty()) throw new NumberFormatException();
                    int m = Integer.parseInt(txt.trim());
                    if (m < 1 || m > 600) throw new NumberFormatException();
                    setter.set(m);
                    saveUserSettingsFromState();
                    updateDurationLabels();
                    refreshUI(); // if current phase changed length
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.WARNING,
                            "Please enter a whole number of minutes between 1 and 600.").showAndWait();
                }
            });
        });
    }

    private void updateDurationLabels() {
        if (focusDurationLabel != null) focusDurationLabel.setText(S.getFocusMinutes() + " min");
        if (shortBreakLabel != null) shortBreakLabel.setText(S.getShortBreakMinutes() + " min");
        if (longBreakLabel != null) longBreakLabel.setText(S.getLongBreakMinutes() + " min");
    }

    // ---------- Controls ----------
    @FXML
    private void toggleTimer() {
        if (S.isRunning()) pauseTimer();
        else startTimer();
    }

    @FXML
    private void stopTimer() {
        // Finish current phase NOW and persist as 'interrupted'
        pauseTimer();
        completeCurrentPhase("interrupted", "interrupted"); // Save actual minutes and status
        S.resetPhase();                                     // Reset same phase timer
        refreshUI();
    }

    @FXML
    private void resetTimer() {
        try {
            pauseTimer();                  // stop running countdown
            S.resetAllForNewSession();     // clears timers/counters/sessionFinished
            S.currentTaskId = null;
            S.phaseStartAt = null;

            // --- Buttons & controls ---
            if (startPauseBtn != null) { startPauseBtn.setDisable(false); startPauseBtn.setText("▶ Start"); }
            if (stopBtn != null)          stopBtn.setDisable(false);
            if (skipSessionBtn != null)   skipSessionBtn.setDisable(false);
            if (resetBtn != null)         resetBtn.setDisable(false);
            if (taskSelector != null)     taskSelector.setDisable(false);

            // --- Labels/UI text ---
            if (currentTaskLabel != null) currentTaskLabel.setText("No task selected");
            if (sessionTypeLabel != null) sessionTypeLabel.setText("Focus");
            if (sessionNumberLabel != null) sessionNumberLabel.setText("Session 1 of 4");
            if (timerLabel != null)       timerLabel.setText(formatTime(S.getFocusMinutes() * 60));
            if (timerProgress != null)    timerProgress.setProgress(0);

            // --- Auto-start is OFF after a full reset ---
            if (autoStartCheckbox != null) autoStartCheckbox.setSelected(false);
            S.setAutoStart(false);

            // --- Task dropdown back to neutral ---
            if (taskSelector != null) {
                taskSelector.getSelectionModel().clearSelection();
                taskSelector.setPromptText("Select a task...");
                loadTasks(); // refresh list (completed tasks may have disappeared)
            }

            refreshUI();

            // ✅ 7. Feedback to user
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Pomodoro Reset");
            a.setHeaderText("Session Restarted");
            a.setContentText("Your Pomodoro has been reset.\nYou can now start a fresh cycle.");
            a.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Reset failed: " + e.getMessage()).showAndWait();
        }
    }


    @FXML
    private void skipSession() {
        // Finish the current phase NOW and persist as 'skipped'
        pauseTimer();
        completeCurrentPhase("skipped", "skipped"); // Save real minutes and mark as skipped
        showInfo("Skipped", "You skipped this " +
                S.getPhase().name().replace('_', ' ').toLowerCase() + ".");
        gotoNextOrCloseThenMaybeAutoStart();
    }

    private void advanceToNextGroupTaskIfNeededBeforeNextPhase() {
        boolean groupMode = (groupTaskModeCheckbox != null && groupTaskModeCheckbox.isSelected());
        if (!groupMode) return;

        // We are about to move to next phase.
        // If the phase that just ended was FOCUS, advance to the next task
        if (S.getPhase() == PomodoroState.Phase.FOCUS) {
            currentGroupTaskIndex++;

            // End-of-queue behavior
            if (currentGroupTaskIndex >= groupTaskQueue.size()) {
                if (groupLoopEnabled && !groupTaskQueue.isEmpty()) {
                    currentGroupTaskIndex = 0; // loop
                } else {
                    // Finished the list — turn off group mode
                    currentGroupTaskIndex = 0;
                    groupTaskQueue.clear();
                    if (groupTaskModeCheckbox != null) groupTaskModeCheckbox.setSelected(false);
                    showInfo("Group tasks complete", "All selected tasks are done.");
                    return;
                }
            }

            // Switch S.currentTaskId to the next task for the next FOCUS phase
            Task next = groupTaskQueue.get(currentGroupTaskIndex);
            S.currentTaskId = next.getTaskId();

            // Update UI selection labels
            Platform.runLater(() -> {
                if (currentTaskLabel != null) currentTaskLabel.setText(next.getTitle());
                if (taskSelector != null) taskSelector.getSelectionModel().select(next);
            });
        }
    }


    private void gotoNextOrCloseThenMaybeAutoStart() {
        // Don’t go further if the full Pomodoro session has ended
        if (S.sessionFinished) {
            return;
        }

        // decide next task if the phase that just ended was a FOCUS
        advanceToNextGroupTaskIfNeededBeforeNextPhase();

        S.gotoNextPhase();
        refreshUI();

        // If auto-start is on, continue automatically
        if (S.isAutoStart()) startTimer();
    }

    @FXML
    private void toggleAutoStart() {
        if (autoStartCheckbox != null) {
            S.setAutoStart(autoStartCheckbox.isSelected());
            saveUserSettingsFromState();
        }
    }

    private void startTimer() {
        // block starting if the 4-session block has been flagged as finished
        if (S.sessionFinished) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Session finished");
            a.setHeaderText("You’ve completed all 4 sessions.");
            a.setContentText("Press Reset to start a new Pomodoro block.");
            a.showAndWait();
            return;
        }

        Task selected = null;

        // --- GROUP TASK MODE ---
        if (groupTaskModeCheckbox != null && groupTaskModeCheckbox.isSelected()) {
            ObservableList<Task> selectedTasks =
                    (groupTaskListView != null) ? groupTaskListView.getSelectionModel().getSelectedItems() : null;

            if (selectedTasks != null && selectedTasks.size() > 10) {
                Alert limitAlert = new Alert(Alert.AlertType.WARNING);
                limitAlert.setTitle("Task Limit");
                limitAlert.setHeaderText("You can only select up to 10 tasks.");
                limitAlert.setContentText("Please deselect extra tasks before starting.");
                limitAlert.showAndWait();
                return;
            }


            if (selectedTasks == null || selectedTasks.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("No Group Tasks");
                a.setHeaderText("No tasks selected for group mode.");
                a.setContentText("Please select at least one task before starting.");
                a.showAndWait();
                return;
            }

            // store queue order for later (C -> A -> B, etc.)
            groupTaskQueue.clear();
            groupTaskQueue.addAll(selectedTasks);
            currentGroupTaskIndex = 0;

            // first task in the chosen order
            selected = groupTaskQueue.get(0);

        } else {
            // --- SINGLE TASK MODE (your original behavior) ---
            selected = (taskSelector != null) ? taskSelector.getSelectionModel().getSelectedItem() : null;
            if (selected == null) selected = ensureDummyTask();   // your helper
        }

        // set current task id and label
        if (selected != null) {
            S.currentTaskId = selected.getTaskId();
            if (currentTaskLabel != null) currentTaskLabel.setText(selected.getTitle());
        }

        // --- open DB session at the first focus cycle (unchanged) ---
        if (S.phaseStartAt == null) {
            S.phaseStartAt = LocalDateTime.now();
            ensureDbSessionOpen();   // your method
        }

        // --- mark task as in-progress (unchanged) ---
        final Task taskRef = selected;
        bg.submit(() -> {
            try {
                if (taskRef != null) taskDAO.updateStatus(taskRef.getTaskId(), "in-progress");
            } catch (Exception ignored) { }
        });

        // --- start the timer (unchanged) ---
        S.start();
        switch (S.getPhase()) {  // play sound by phase
            case FOCUS -> SoundForPomodoro.play("focus_start.wav");
            case SHORT_BREAK -> SoundForPomodoro.play("short_break_start.wav");
            case LONG_BREAK -> SoundForPomodoro.play("long_break_start.wav");
        }

        addTimerGlow(true);
        refreshUI();
    }

    private void pauseTimer() {
        S.pause();
        addTimerGlow(false);
        refreshUI();
    }



    // Called when a phase reaches 0 or when skipped/stopped
    private void completeCurrentPhase(String statusText, String dbStatus) {
        // --- Always refresh table visually ---
        Platform.runLater(() -> sessionHistoryTable.refresh());
        LocalDateTime end = LocalDateTime.now();

        // --- 🔊 Play sound feedback based on outcome ---
        switch (dbStatus.toLowerCase()) {
            case "completed" -> SoundForPomodoro.play("focus_end.wav");
            case "skipped" -> SoundForPomodoro.play("skip.wav");
            case "interrupted" -> SoundForPomodoro.play("stop.wav");
        }

        // --- Ensure phase start time exists ---
        if (S.phaseStartAt == null)
            S.phaseStartAt = end;

        // --- Determine planned duration ---
        int plannedMin = switch (S.getPhase()) {
            case FOCUS -> S.getFocusMinutes();
            case SHORT_BREAK -> S.getShortBreakMinutes();
            case LONG_BREAK -> S.getLongBreakMinutes();
        };

        // --- Actual duration (minimum 1 minute for clarity) ---
        long actualSec = java.time.Duration.between(S.phaseStartAt, end).getSeconds();
        int actualMin = Math.max(1, (int) Math.round(actualSec / 60.0));
        String typeStr = S.getPhase().name().replace('_', ' ').toLowerCase(Locale.ROOT);

        // --- Build readable label for UI ---
        String statusLabel = switch (dbStatus.toLowerCase()) {
            case "completed" -> "completed";
            case "skipped" -> "skipped " + actualMin + " min";
            case "interrupted" -> "interrupted " + actualMin + " min";
            default -> dbStatus;
        };

        // --- Add history record visually ---
        if (sessionHistoryTable != null) {
            sessionHistoryTable.getItems().add(0, new HistoryItem(
                    capitalize(typeStr),
                    statusLabel,
                    actualMin + " min",
                    pretty(S.phaseStartAt),
                    pretty(end)
            ));
        }

        // --- Handle final session completion (4 focus sessions done) ---
        if (S.getPhase() == PomodoroState.Phase.LONG_BREAK
                && "completed".equalsIgnoreCase(dbStatus)) {

            closeDbSession(end);
            S.finishSessionBlock();

            // Delete current task if any
            if (S.currentTaskId != null) {
                try {
                    taskDAO.delete(S.currentTaskId); // make sure your DAO has a delete method
                    S.currentTaskId = null;
                } catch (Exception e) {
                    System.err.println("Failed to delete completed task: " + e.getMessage());
                }
            }

            // Reset session to defaults
            S.resetAllForNewSession(); // You already have this method
            S.phaseStartAt = null;

            // Update UI
            Platform.runLater(() -> {
                if (currentTaskLabel != null) currentTaskLabel.setText("No task selected");
                if (sessionTypeLabel != null) sessionTypeLabel.setText("Focus");
                if (sessionNumberLabel != null) sessionNumberLabel.setText("Session 1 of 4");
                if (timerLabel != null) timerLabel.setText(formatTime(S.getFocusMinutes() * 60));
                if (autoStartCheckbox != null) autoStartCheckbox.setSelected(false);

                startPauseBtn.setDisable(false);
                stopBtn.setDisable(false);
                resetBtn.setDisable(false);
                skipSessionBtn.setDisable(false);

                refreshUI();

                Alert done = new Alert(Alert.AlertType.INFORMATION);
                done.setTitle("Pomodoro Cycle Complete");
                done.setHeaderText("Awesome work!");
                done.setContentText("You've completed your study block. Ready to start a new task?");
                done.showAndWait();
            });

            return; // stop here
        }


        // --- Persist to DB in background ---
        final int plannedF = plannedMin;
        final int actualF = actualMin;
        final LocalDateTime startF = S.phaseStartAt;
        final LocalDateTime endF = end;
        final String statusF = dbStatus;
        bg.submit(() -> persistCycle(plannedF, actualF, startF, endF, statusF));

        if (S.getPhase() == PomodoroState.Phase.FOCUS
                && "completed".equalsIgnoreCase(dbStatus)
                && S.currentTaskId != null) {

            final int doneTaskId = S.currentTaskId;
            bg.submit(() -> {
                try { taskDAO.updateStatus(doneTaskId, "completed"); } catch (Exception ignored) {}
            });
        }


        // --- If focus completed, mark the task completed ---
        // ✅ Final long break completed — mark finished, stop, and inform the user
        if (S.getPhase() == PomodoroState.Phase.LONG_BREAK && "completed".equalsIgnoreCase(dbStatus)) {
            closeDbSession(end);
            S.sessionFinished = true;   // <-- just set the flag
            S.pause();                  // stop timer
            S.phaseStartAt = null;      // clear stamp just in case
            S.setRunning(false);        // (if you keep an explicit flag)

            // Don't disable any buttons here. Just turn off auto-start.
            if (autoStartCheckbox != null) autoStartCheckbox.setSelected(false);
            S.setAutoStart(false);

            Platform.runLater(() -> {
                Alert done = new Alert(Alert.AlertType.INFORMATION);
                done.setTitle("Pomodoro Session Complete");
                done.setHeaderText("Great job!");
                done.setContentText(
                        "You've completed 4 focus sessions.\n" +
                                "Click Reset to start a brand new Pomodoro block."
                );
                done.showAndWait();
            });

            // do not advance to next phase
            return;
        }
    }


        // ---------- UiListener tick ----------
    @Override public void onTick() {
        // Detect phase completion (0 just reached)
        int now = S.getRemainingSeconds();
        if (lastRemaining != -1 && lastRemaining > 0 && now == 0) {
            // phase completed naturally
            SoundFX.incoming();
            SoundForPomodoro.play("focus_end.wav");
            completeCurrentPhase("completed", "completed");
            gotoNextOrCloseThenMaybeAutoStart();
        }
        lastRemaining = now;
        refreshUI();
    }

    // ---------- DB ----------

    // === Load today's session history from DB and fill the table ===
    private void loadTodayHistoryFromDb() {
        if (sessionHistoryTable == null) return;
        sessionHistoryTable.getItems().clear();

        Integer uid = getUid();
        if (uid == null) return;

        String sql = """
        SELECT c.CycleType, c.PlannedDuration, c.ActualDuration, c.StartTime, c.EndTime, c.Status
        FROM PomodoroCycles c
        JOIN PomodoroSessions s ON s.SessionID = c.SessionID
        WHERE s.UserID = ? AND DATE(c.StartTime) = CURDATE()
        ORDER BY c.StartTime DESC
    """;

        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cycleType = rs.getString(1);
                    int planned = rs.getInt(2);
                    Integer actual = (Integer) rs.getObject(3); // may be null
                    java.sql.Timestamp st = rs.getTimestamp(4);
                    java.sql.Timestamp et = rs.getTimestamp(5);
                    String dbStatus = rs.getString(6) == null ? "completed" : rs.getString(6);

                    int actualMin;
                    if (actual != null) {
                        actualMin = actual;
                    } else if (st != null && et != null) {
                        actualMin = (int) Math.max(0, Math.round((et.getTime() - st.getTime()) / 60000.0));
                    } else {
                        actualMin = planned;
                    }

                    String typeUi = capitalizeWords(cycleType.replace('_',' '));
                    String statusUi = dbStatus.equalsIgnoreCase("completed") ? "completed" : ("skipped " + actualMin + " min");
                    String durationUi = actualMin + " min";
                    String startUi = (st == null) ? "" : pretty(st.toLocalDateTime());
                    String endUi = (et == null) ? "" : pretty(et.toLocalDateTime());

                    sessionHistoryTable.getItems().add(new HistoryItem(typeUi, statusUi, durationUi, startUi, endUi));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Load History", "Failed to load today's sessions: " + e.getMessage());
        }
    }

    // Button action to refresh (used by the new Refresh button)
    @FXML private void handleRefreshSessions() { loadTodayHistoryFromDb(); }


    private void ensureDbSessionOpen() {
        if (S.currentSessionId != null) return;
        if (S.getPhase() != PomodoroState.Phase.FOCUS || S.getFocusIndex() != 1) return;
        Integer uid = getUid(); if (uid == null) return;
        S.sessionStartAt = LocalDateTime.now();
        String sql = "INSERT INTO PomodoroSessions (UserID, StartTime, Duration, BreakDuration, Status, CompletedCycles) "
                + "VALUES (?, ?, 0, NULL, 'completed', 0)";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, uid);
            ps.setTimestamp(2, Timestamp.valueOf(S.sessionStartAt));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) S.currentSessionId = rs.getInt(1); }
        } catch (Exception ignored) {}
    }

    private void persistCycle(int planned, int actual, LocalDateTime start, LocalDateTime end, String status) {
        // ensure session exists first
        if (S.currentSessionId == null) ensureDbSessionOpen();
        if (S.currentSessionId == null) return;

        String type = S.getPhase().name().toLowerCase(Locale.ROOT);

        String sql = """
        INSERT INTO PomodoroCycles 
        (SessionID, CycleNumber, CycleType, PlannedDuration, ActualDuration, StartTime, EndTime, Status, TaskID) 
        VALUES (?,?,?,?,?,?,?,?,?)
    """;

        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, S.currentSessionId);
            ps.setInt(2, S.getCycleIndex());
            ps.setString(3, type);
            ps.setInt(4, planned);
            ps.setInt(5, actual);
            ps.setTimestamp(6, Timestamp.valueOf(start));
            ps.setTimestamp(7, Timestamp.valueOf(end));
            ps.setString(8, status);

            if (S.currentTaskId != null) ps.setInt(9, S.currentTaskId);
            else ps.setNull(9, Types.INTEGER);

            ps.executeUpdate();

            // 🔹 Save generated CycleID for later updates (used by recordCycleStatus)
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    S.currentCycleId = rs.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCompletedCycles(int completed) {
        if (S.currentSessionId == null) return;
        String sql = "UPDATE PomodoroSessions SET CompletedCycles=? WHERE SessionID=?";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, completed);
            ps.setInt(2, S.currentSessionId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void closeDbSession(LocalDateTime end) {
        if (S.currentSessionId == null || S.sessionStartAt == null) return;
        long totalMin = Math.max(0, java.time.Duration.between(S.sessionStartAt, end).toMinutes());
        String sql = "UPDATE PomodoroSessions SET EndTime=?, Duration=?, CompletedCycles=? WHERE SessionID=?";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(end));
            ps.setInt(2, (int) totalMin);
            ps.setInt(3, S.completedCycles);
            ps.setInt(4, S.currentSessionId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
        S.currentSessionId = null;
        S.sessionStartAt = null;
    }

    private Integer getUid() {
        try { return SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getUserId() : null; }
        catch (Exception e) { return null; }
    }

    // ---------- Dummy Task (no no-args Task() needed) ----------
    private Task ensureDummyTask() {
        try {
            Integer uid = getUid();
            if (uid == null) return null;
            Task t = new Task(uid, "Dummy Task", null); // uses your existing ctor
            t.setDescription("Auto-created by Pomodoro");
            t.setPriority("low");
            t.setStatus("pending");
            int id = taskDAO.insert(t);
            t.setTaskId(id);
            if (taskSelector != null) {
                Platform.runLater(() -> {
                    taskSelector.getItems().add(0, t);
                    taskSelector.getSelectionModel().select(t);
                });
            }
            return t;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    private static String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;
        String[] parts = text.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts)
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        return sb.toString().trim();
    }


    // ---------- UI Helpers ----------
    private void refreshUI() {
        if (timerLabel != null) timerLabel.setText(formatTime(S.getRemainingSeconds()));

        if (timerProgress != null) {
            int total = S.getTotalSeconds();
            int rem   = S.getRemainingSeconds();
            timerProgress.setProgress(total == 0 ? 0 : (double) (total - rem) / total);
        }

        if (sessionTypeLabel != null)
            sessionTypeLabel.setText(capitalizeWords(S.getPhase().name().replace('_', ' ')));
        if (sessionNumberLabel != null)
            sessionNumberLabel.setText("Session " + S.getFocusIndex() + " of 4");

        if (startPauseBtn != null) {
            startPauseBtn.setDisable(false);     // always enabled
            startPauseBtn.setText(S.isRunning() ? "⏸ Pause" : "▶ Start");
        }
        if (resetBtn != null) resetBtn.setDisable(false);      // always enabled
        if (stopBtn != null)  stopBtn.setDisable(!S.isRunning());
        if (skipSessionBtn != null) skipSessionBtn.setDisable(false);
        if (confirmGroupBtn != null) {
            boolean groupMode    = groupTaskModeCheckbox != null && groupTaskModeCheckbox.isSelected();
            boolean hasSelection = groupTaskListView != null
                    && groupTaskListView.getSelectionModel().getSelectedItems() != null
                    && !groupTaskListView.getSelectionModel().getSelectedItems().isEmpty();

            // show only in group mode with a selection
            confirmGroupBtn.setVisible(groupMode && hasSelection);

            // disable while running or session finished
            confirmGroupBtn.setDisable(S.isRunning() || S.sessionFinished || !hasSelection);
        }



        // --- current task display ---
        if (currentTaskLabel != null) {
            String txt = "No task selected";

            // if user has selected a task, reflect it
            if (S.currentTaskId != null && taskSelector != null) {
                Task sel = taskSelector.getItems().stream()
                        .filter(t -> t.getTaskId() == S.currentTaskId)
                        .findFirst()
                        .orElse(null);

                if (sel != null) {
                    txt = sel.getTitle();

                    // ensure combo selection reflects the global state
                    if (taskSelector.getSelectionModel().getSelectedItem() == null ||
                            taskSelector.getSelectionModel().getSelectedItem().getTaskId() != sel.getTaskId()) {
                        taskSelector.getSelectionModel().select(sel);
                    }
                }
            }

            boolean locked = S.sessionFinished;
            if (startPauseBtn != null) {
                startPauseBtn.setDisable(locked);
                startPauseBtn.setText(S.isRunning() ? "⏸ Pause" : "▶ Start");
            }
            if (stopBtn != null)  stopBtn.setDisable(locked);
            if (skipSessionBtn != null) skipSessionBtn.setDisable(locked);
            if (autoStartCheckbox != null) {
                autoStartCheckbox.setSelected(S.isAutoStart());
                autoStartCheckbox.setDisable(locked);
            }

            currentTaskLabel.setText(txt);
        }

        boolean locked = S.sessionFinished;
        if (!S.isRunning() && !S.sessionFinished) locked = false;

        updateDurationLabels();
    }

    private void addTimerGlow(boolean on) {
        if (timerLabel == null) return;
        if (on) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#FFD700"));
            glow.setRadius(25);
            timerLabel.setEffect(glow);
        } else {
            timerLabel.setEffect(null);
        }
    }

    private static String pretty(LocalDateTime t) { return t == null ? "" : t.toString().replace('T',' '); }
    private static String capitalize(String s) { return s.isEmpty()?s : Character.toUpperCase(s.charAt(0))+s.substring(1); }
    private static String formatTime(int sec) { if (sec < 0) sec = 0; int m = sec/60, s = sec%60; return String.format("%02d:%02d", m, s); }
    private static void showInfo(String title, String msg){ Alert a=new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.show(); }


    // --- Group Task Mode UI ---
//    @FXML private CheckBox groupTaskModeCheckbox;
//    @FXML private ListView<Task> groupTaskListView;

    // --- Group Task Mode state ---
//    private final java.util.List<Task> groupTaskQueue = new java.util.ArrayList<>();
//    private int currentGroupTaskIndex = 0;
    private boolean groupLoopEnabled = false; // optional: set true if you want looping




    // ---------- Export & Clear ----------
    @FXML private void exportSessions() {
        try {
            if (sessionHistoryTable == null || sessionHistoryTable.getItems().isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "No data to export.").show(); return;
            }
            FileChooser fc = new FileChooser();
            fc.setTitle("Export Pomodoro Sessions");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            fc.setInitialFileName("pomodoro_sessions.csv");
            java.io.File f = fc.showSaveDialog(sessionHistoryTable.getScene().getWindow());
            if (f == null) return;
            try (java.io.FileWriter w = new java.io.FileWriter(f)) {
                w.write("Type,Status,Duration,Start Time,End Time\n");
                for (HistoryItem i : sessionHistoryTable.getItems()) {
                    w.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                            i.getType(), i.getStatus(), i.getDuration(), i.getStartTime(), i.getEndTime()));
                }
            }
            new Alert(Alert.AlertType.INFORMATION, "Exported successfully!").show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).show();
        }
    }



    @FXML private void clearTodayHistory() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This will delete all Pomodoro sessions from today. Continue?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Clear Today's History?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                Integer uid = getUid();
                if (uid != null) {
                    try (Connection con = DB.getConnection()) {
                        PreparedStatement ps1 = con.prepareStatement(
                                "DELETE FROM PomodoroCycles WHERE SessionID IN (" +
                                        "SELECT SessionID FROM PomodoroSessions WHERE UserID=? AND DATE(StartTime)=CURDATE())");
                        ps1.setInt(1, uid); ps1.executeUpdate();
                        PreparedStatement ps2 = con.prepareStatement(
                                "DELETE FROM PomodoroSessions WHERE UserID=? AND DATE(StartTime)=CURDATE()");
                        ps2.setInt(1, uid); ps2.executeUpdate();
                    }
                }
                if (sessionHistoryTable != null) {
                    String today = java.time.LocalDate.now().toString();
                    sessionHistoryTable.getItems().removeIf(i -> i.getStartTime().startsWith(today));
                }
                new Alert(Alert.AlertType.INFORMATION, "Today's Pomodoro history has been cleared!").show();
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Could not clear today's history.\n" + e.getMessage()).show();
            }
        });
    }

    // ---------- Settings persistence (simple) ----------
    private void ensureSettingsTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS UserSettings (
                  UserID INT PRIMARY KEY,
                  FocusMinutes INT NOT NULL DEFAULT 25,
                  ShortBreakMinutes INT NOT NULL DEFAULT 5,
                  LongBreakMinutes INT NOT NULL DEFAULT 15,
                  AutoStart TINYINT(1) NOT NULL DEFAULT 0,
                  FOREIGN KEY (UserID) REFERENCES Users(UserID)
                )
                """;
        try (Connection c = DB.getConnection(); Statement s = c.createStatement()) { s.execute(sql); }
        catch (Exception ignored) {}
    }

    private void loadUserSettingsIntoState() {
        Integer uid = getUid(); if (uid == null) return;
        String q = "SELECT FocusMinutes, ShortBreakMinutes, LongBreakMinutes, AutoStart FROM UserSettings WHERE UserID=?";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    S.setFocusMinutes(rs.getInt(1));
                    S.setShortBreakMinutes(rs.getInt(2));
                    S.setLongBreakMinutes(rs.getInt(3));
                    S.setAutoStart(rs.getInt(4) == 1);
                } else {
                    // insert defaults
                    String ins = "INSERT INTO UserSettings(UserID) VALUES(?)";
                    try (PreparedStatement insPs = c.prepareStatement(ins)) {
                        insPs.setInt(1, uid); insPs.executeUpdate();
                    }
                }
            }
        } catch (Exception ignored) {}
        updateDurationLabels();
    }

    private void saveUserSettingsFromState() {
        Integer uid = getUid(); if (uid == null) return;
        String up = "REPLACE INTO UserSettings(UserID, FocusMinutes, ShortBreakMinutes, LongBreakMinutes, AutoStart) VALUES(?,?,?,?,?)";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(up)) {
            ps.setInt(1, uid);
            ps.setInt(2, S.getFocusMinutes());
            ps.setInt(3, S.getShortBreakMinutes());
            ps.setInt(4, S.getLongBreakMinutes());
            ps.setInt(5, S.isAutoStart() ? 1 : 0);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    // ---------- Navigation (detach to avoid leaks) ----------
    public void showDashboard() throws IOException { S.detach(); HelloApplication.loadPage("Homepage.fxml"); }
    public void showPomodoro() throws IOException { S.detach(); HelloApplication.loadPage("pomodoroPage.fxml"); }
    public void showTasks() throws IOException { S.detach(); HelloApplication.loadPage("todo.fxml"); }
    public void showExpenses() throws IOException { S.detach(); HelloApplication.loadPage("expense.fxml"); }
    public void showAnalytics() { System.out.println("Show analytics"); }
    public void showTips() { System.out.println("Show tips"); }
    public void showSettings() { System.out.println("Show settings"); }
    public void showChat() { new HomeController().getOpenCourseChatPicker(); }

    @FXML private void logout() throws IOException {
        S.detach();
        SessionManager.logout();
        HelloApplication.loadPage("login.fxml");
    }

    // ---------- Header placeholders so FXML never breaks ----------
    @FXML private void openPomodoroSettings() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Pomodoro Settings");
        a.setHeaderText(null);
        a.setContentText("Settings will be added later.");
        a.showAndWait();
    }
    @FXML private void showPomodoroHelp() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Pomodoro Help");
        a.setHeaderText(null);
        a.setContentText("Help will be added later.");
        a.showAndWait();
    }

    // ---------- History row ----------
    public static class HistoryItem {
        private final String type, status, duration, startTime, endTime;
        public HistoryItem(String type, String status, String duration, String startTime, String endTime) {
            this.type = type; this.status = status; this.duration = duration; this.startTime = startTime; this.endTime = endTime;
        }
        public String getType() { return type; }
        public String getStatus() { return status; }
        public String getDuration() { return duration; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
    }

    // --- legacy handlers for +/- buttons in FXML (so FXML doesn't break) ---
    @FXML private void decreaseFocusTime() { S.setFocusMinutes(Math.max(1, S.getFocusMinutes() - 1)); refreshUI(); saveUserSettingsFromState(); }
    @FXML private void increaseFocusTime() { S.setFocusMinutes(S.getFocusMinutes() + 1); refreshUI(); saveUserSettingsFromState(); }

    @FXML private void decreaseShortBreak() { S.setShortBreakMinutes(Math.max(1, S.getShortBreakMinutes() - 1)); refreshUI(); saveUserSettingsFromState(); }
    @FXML private void increaseShortBreak() { S.setShortBreakMinutes(S.getShortBreakMinutes() + 1); refreshUI(); saveUserSettingsFromState(); }

    @FXML private void decreaseLongBreak() { S.setLongBreakMinutes(Math.max(1, S.getLongBreakMinutes() - 1)); refreshUI(); saveUserSettingsFromState(); }
    @FXML private void increaseLongBreak() { S.setLongBreakMinutes(S.getLongBreakMinutes() + 1); refreshUI(); saveUserSettingsFromState(); }

}
