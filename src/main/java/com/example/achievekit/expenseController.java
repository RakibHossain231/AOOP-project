package com.example.achievekit;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class expenseController {

    @FXML private Button dashboardBtn, pomodoroBtn, tasksBtn, expensesBtn, chatBtn;
    @FXML private Button analyticsBtn, settingsBtn, tipsBtn;

    // --- Chart ---
    @FXML private BarChart<String, Number> weeklyExpenseChart;

    // --- Add form ---
    @FXML private TextField expenseNameField;
    @FXML private TextField expenseAmountField;
    @FXML private TextField expenseNotesField;     // notes input on add form
    @FXML private Button addExpenseBtn;

    // --- Table & columns ---
    @FXML private TableView<Expense> expenseTable;
    @FXML private TableColumn<Expense, String>      expenseNameColumn;
    @FXML private TableColumn<Expense, BigDecimal>  expenseAmountColumn;
    @FXML private TableColumn<Expense, LocalDate>   expenseDateColumn;
    @FXML private TableColumn<Expense, String>      expenseCategoryColumn;
    @FXML private TableColumn<Expense, String>      expenseNotesColumn;
    @FXML private TableColumn<Expense, Void>        expenseActionColumn;

    @FXML private VBox emptyExpenseState; // empty view

    private final ExpenseDAO dao = new ExpenseDAO();
    private final ObservableList<Expense> data = FXCollections.observableArrayList();

    // --- Fixed category options for dropdown ---
    private static final ObservableList<String> CATEGORY_OPTIONS = FXCollections.observableArrayList(
            "General", "Food", "Transport", "Education", "Bills", "Health", "Shopping", "Entertainment", "Other"
    );

    @FXML
    public void initialize() {
        // Page Load indicate
        Nav.highlight(expensesBtn, dashboardBtn, pomodoroBtn, tasksBtn, expensesBtn, chatBtn, analyticsBtn, settingsBtn, tipsBtn);

        // --- Table sizing so right-side space goes to Action column ---
        expenseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // base widths (these will shrink if space is tight)
        expenseNameColumn.setPrefWidth(180);
        expenseAmountColumn.setPrefWidth(100);
        expenseDateColumn.setPrefWidth(140);
        expenseCategoryColumn.setPrefWidth(140);
        expenseNotesColumn.setPrefWidth(220);

        // give action column enough room for two buttons
        expenseActionColumn.setMinWidth(200);
        expenseActionColumn.setPrefWidth(220);
        expenseActionColumn.setResizable(true);
        expenseActionColumn.setSortable(false);

        // Table bindings
        expenseNameColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getNameForTable()));
        expenseAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        expenseDateColumn.setCellValueFactory(new PropertyValueFactory<>("expenseDate"));
        expenseCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        expenseNotesColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getNotesForTable()));

        addActionButtons();

        expenseTable.setItems(data);
        expenseTable.setPlaceholder(new Label("No content in table"));

        refreshTable();
        buildWeeklyChart();

        updateEmptyState();
        data.addListener((ListChangeListener<? super Expense>) change -> updateEmptyState());
    }

    /* ---------- Navigation (sidebar) ---------- */
    public void showDashboard() throws IOException { HelloApplication.loadPage("Homepage.fxml"); }
    public void showPomodoro() throws IOException { HelloApplication.loadPage("pomodoroPage.fxml"); }
    public void showExpenses() { /* already here */ }
    /** Sidebar -> Study Chat */
    HomeController a= new HomeController();
    public void showChat() {
        a.getOpenCourseChatPicker(); // ✅ multi-select picker for Chat
    }

    public void showAnalytics() { System.out.println("Showing Analytics"); }
    public void showTips() { System.out.println("Showing Tips"); }
    public void showTasks() throws IOException { HelloApplication.loadPage("todo.fxml"); }
    public void logout() { System.out.println("Logout requested"); }
    public void openExpensesSettings() { System.out.println("Open settings"); }
    public void showExpensesHelp() { System.out.println("Help"); }
    public void showSettings(){ System.out.println("Settings"); }

    /* ---------- Add Expense ---------- */
    public void addExpense() {
        User u = SessionManager.getCurrentUser();
        if (u == null) {
            new Alert(Alert.AlertType.WARNING, "Please login first.").showAndWait();
            return;
        }

        String name = safe(expenseNameField.getText());
        String amtStr = safe(expenseAmountField.getText());
        String notesStr = safe(expenseNotesField.getText()); // can be empty

        if (name.isBlank()) {
            toast("Expense Name is required.");
            expenseNameField.requestFocus();
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amtStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            toast("Enter a valid positive amount.");
            expenseAmountField.requestFocus();
            return;
        }

        // capture Category/Date via dialog (Category now ComboBox)
        CategoryDateResult extra = askCategoryAndDate();
        if (extra == null) return;

        Expense e = new Expense();
        e.setUserId(u.getUserId());
        e.setDescription(name);
        e.setAmount(amount);
        e.setCategory(extra.category);
        e.setType("expense");
        e.setExpenseDate(extra.date != null ? extra.date : LocalDate.now());
        e.setNotes(notesStr); // save notes from add form (may be empty)

        try {
            int newId = dao.insert(e);
            e.setExpenseId(newId);
            data.add(0, e);
            clearAddForm();
            buildWeeklyChart();
            toast("Expense added.");
        } catch (Exception ex) {
            ex.printStackTrace();
            toast("Failed to add expense.");
        }
    }

    /* ---------- Helpers ---------- */
    private void refreshTable() {
        data.clear();
        User u = SessionManager.getCurrentUser();
        if (u == null) return;
        data.addAll(dao.findAllByUser(u.getUserId()));
    }

    private void buildWeeklyChart() {
        weeklyExpenseChart.getData().clear();
        User u = SessionManager.getCurrentUser();
        if (u == null) return;

        Map<LocalDate, java.math.BigDecimal> map = dao.last7DaysTotals(u.getUserId());

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Expense");

        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            java.math.BigDecimal val = map.getOrDefault(d, java.math.BigDecimal.ZERO);
            String label = d.getDayOfWeek().name().substring(0, 3); // Mon/Tue/...
            s.getData().add(new XYChart.Data<>(label, val));
        }
        weeklyExpenseChart.getData().add(s);
    }

    private void addActionButtons() {
        expenseActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button del  = new Button("Delete");
            private final HBox box = new HBox(8, edit, del);

            {
                box.setAlignment(Pos.CENTER);

                edit.getStyleClass().add("control-btn");
                del.getStyleClass().add("control-btn");

                // Make sure text isn't ellipsized
                edit.setPrefWidth(90);  edit.setMinWidth(90);
                del.setPrefWidth(90);   del.setMinWidth(90);
                edit.setPadding(new Insets(4,10,4,10));
                del.setPadding(new Insets(4,10,4,10));

                edit.setOnAction(e -> onEdit(getCurrentItem()));
                del.setOnAction(e -> onDelete(getCurrentItem()));
            }

            private Expense getCurrentItem() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void onEdit(Expense e) {
        if (e == null) return;
        // For edit: show dialog with Category + Date + Notes
        EditDialogResult res = askEditFields(e.getCategory(), e.getExpenseDate(), e.getNotes());
        if (res == null) return;

        // Optional quick edits from top inputs
        String newNameTop = safe(expenseNameField.getText());
        String newAmtTop  = safe(expenseAmountField.getText());
        String newNotesTop = safe(expenseNotesField.getText());
        if (!newNameTop.isBlank())  e.setDescription(newNameTop);
        if (!newAmtTop.isBlank())   { try { e.setAmount(new BigDecimal(newAmtTop)); } catch (Exception ignored) {} }
        if (!newNotesTop.isBlank()) e.setNotes(newNotesTop); // if user typed on top

        // Dialog fields override
        e.setCategory(res.category);
        e.setExpenseDate(res.date == null ? e.getExpenseDate() : res.date);
        e.setNotes(res.notes == null ? "" : res.notes);

        try {
            dao.update(e);
            expenseTable.refresh();
            buildWeeklyChart();
            clearAddForm();
            toast("Expense updated.");
        } catch (Exception ex) {
            ex.printStackTrace();
            toast("Failed to update.");
        }
    }

    private void onDelete(Expense e) {
        if (e == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete this expense?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        a.showAndWait();
        if (a.getResult() != ButtonType.OK) return;

        try {
            dao.delete(e.getExpenseId(), e.getUserId());
            data.remove(e);
            buildWeeklyChart();
            toast("Deleted.");
        } catch (Exception ex) {
            ex.printStackTrace();
            toast("Failed to delete.");
        }
    }

    private void clearAddForm() {
        expenseNameField.clear();
        expenseAmountField.clear();
        if (expenseNotesField != null) expenseNotesField.clear();
    }

    private void updateEmptyState() {
        if (emptyExpenseState != null) emptyExpenseState.setVisible(data.isEmpty());
    }

    private String safe(String s){ return (s == null) ? "" : s.trim(); }

    private void toast(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /* ---------- Small dialogs ---------- */
    private static class CategoryDateResult {
        String category;
        LocalDate date;
    }
    private static class EditDialogResult {
        String category;
        LocalDate date;
        String notes;
    }

    // Add flow: Category (ComboBox) & Date
    private CategoryDateResult askCategoryAndDate() {
        Dialog<CategoryDateResult> dlg = new Dialog<>();
        dlg.setTitle("Expense Details");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> categoryBox = new ComboBox<>(CATEGORY_OPTIONS);
        categoryBox.setEditable(false);
        categoryBox.setPromptText("Select category");
        categoryBox.getSelectionModel().select("General");

        DatePicker datePicker = new DatePicker(LocalDate.now());

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));
        gp.addRow(0, new Label("Category"), categoryBox);
        gp.addRow(1, new Label("Date"), datePicker);

        dlg.getDialogPane().setContent(gp);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                CategoryDateResult r = new CategoryDateResult();
                String chosen = categoryBox.getValue();
                r.category = (chosen == null || chosen.isBlank()) ? "General" : chosen.trim();
                r.date = (datePicker.getValue() == null) ? LocalDate.now() : datePicker.getValue();
                return r;
            }
            return null;
        });

        return dlg.showAndWait().orElse(null);
    }

    // Edit flow: Category (ComboBox), Date, Notes (textarea)
    private EditDialogResult askEditFields(String defaultCategory, LocalDate defaultDate, String defaultNotes) {
        Dialog<EditDialogResult> dlg = new Dialog<>();
        dlg.setTitle("Expense Details");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> categoryBox = new ComboBox<>(CATEGORY_OPTIONS);
        categoryBox.setEditable(false);
        categoryBox.setPromptText("Select category");
        // default select
        if (defaultCategory != null && CATEGORY_OPTIONS.contains(defaultCategory)) {
            categoryBox.getSelectionModel().select(defaultCategory);
        } else {
            categoryBox.getSelectionModel().select("General");
        }

        DatePicker datePicker = new DatePicker(defaultDate == null ? LocalDate.now() : defaultDate);

        TextArea notesArea = new TextArea(defaultNotes == null ? "" : defaultNotes);
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));
        gp.addRow(0, new Label("Category"), categoryBox);
        gp.addRow(1, new Label("Date"), datePicker);
        gp.addRow(2, new Label("Notes"), notesArea);

        dlg.getDialogPane().setContent(gp);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                EditDialogResult r = new EditDialogResult();
                String chosen = categoryBox.getValue();
                r.category = (chosen == null || chosen.isBlank()) ? "General" : chosen.trim();
                r.date = (datePicker.getValue() == null) ? LocalDate.now() : datePicker.getValue();
                r.notes = notesArea.getText() == null ? "" : notesArea.getText().trim();
                return r;
            }
            return null;
        });

        return dlg.showAndWait().orElse(null);
    }
}
