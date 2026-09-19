package frontend;

import db.DatabaseHelper;
import algorithm.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

public class IMTPSController {
    @FXML private Label lblUsernameIMTPS;
    
    @FXML private TabPane mainTabPane;
    @FXML private Tab tabView;
    @FXML private Tab tabAdd;
    @FXML private Tab tabEdit;

    @FXML private TextField btnAddName;
    @FXML private ComboBox<String> btnCategory;
    @FXML private Spinner<Integer> btnImportance;
    @FXML private Spinner<Double> btnDuration;
    @FXML private DatePicker btnStartDate;
    @FXML private TextField txtStartTime;
    @FXML private DatePicker btnDueDate;
    @FXML private Label lblAddStatus;

    @FXML private VBox taskContainer;
    @FXML private DatePicker dpViewDate;
    @FXML private ComboBox<String> cmbSchedulerType;
    @FXML private RadioButton rdbtnWork;
    @FXML private RadioButton rdbtnHealth;
    @FXML private RadioButton rdbtnPersonal;

    @FXML private TextField txtSearchTask;
    @FXML private VBox editTaskContainer;
    @FXML private Label lblEditStatus;

    @FXML private VBox completedTaskContainer;
    @FXML private Label lblCompletedStatus;

    private static final LocalTime START_OF_DAY = LocalTime.of(0, 0);

    private static class TaskPlacement {
        double startY, endY;
        int col;
        TaskPlacement(double startY, double endY, int col) {
            this.startY = startY;
            this.endY = endY;
            this.col = col;
        }
    }

    @FXML
    public void initialize() {
        lblUsernameIMTPS.setText("Welcome, " + Session.currentUsername + "!");
        btnCategory.getItems().addAll("1. Work", "2. Health", "3. Personal");

        SpinnerValueFactory<Integer> importanceFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1);
        btnImportance.setValueFactory(importanceFactory);

        SpinnerValueFactory<Double> durationFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 24.0, 1.0, 0.25);
        btnDuration.setValueFactory(durationFactory);

        ToggleGroup modeGroup = new ToggleGroup();
        rdbtnWork.setToggleGroup(modeGroup);
        rdbtnHealth.setToggleGroup(modeGroup);
        rdbtnPersonal.setToggleGroup(modeGroup);
        rdbtnPersonal.setSelected(true);

        dpViewDate.setValue(LocalDate.now());

        cmbSchedulerType.getItems().addAll("Priority Scheduler", "Deadline Scheduler", "Knapsack Scheduler");
        cmbSchedulerType.getSelectionModel().selectFirst();

        // Add a listener to the ComboBox to enable/disable radio buttons based on the selected scheduler type
        cmbSchedulerType.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isPriority = "Priority Scheduler".equals(newVal);
            rdbtnWork.setDisable(!isPriority);
            rdbtnHealth.setDisable(!isPriority);
            rdbtnPersonal.setDisable(!isPriority);
        });

        // Set the initial disabled/enabled state upon initialization
        boolean isInitiallyPriority = "Priority Scheduler".equals(cmbSchedulerType.getValue());
        rdbtnWork.setDisable(!isInitiallyPriority);
        rdbtnHealth.setDisable(!isInitiallyPriority);
        rdbtnPersonal.setDisable(!isInitiallyPriority);
       

        Platform.runLater(() -> {
            Stage stage = (Stage) lblUsernameIMTPS.getScene().getWindow();
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            stage.setMaximized(true);
            
            onClickRefreshTasks(null);
            onClickSearchTasks(null);
        });

        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tabView) {
                onClickRefreshTasks(null);
            } else if (newTab == tabEdit) {
                onClickSearchTasks(null);
            }
        });
    }

    // region Add Task Logic
    public void onClickBtnAddTask(ActionEvent event) {
        String addName = btnAddName.getText();
        if (addName == null || addName.trim().isEmpty()) {
            lblAddStatus.setText("Error: Task Name cannot be empty.");
            lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return;
        }

        if (btnCategory.getValue() == null) {
            lblAddStatus.setText("Error: Please select a category.");
            lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return;
        }

        LocalDate startVal = btnStartDate.getValue();
        if (startVal == null) {
            lblAddStatus.setText("Error: Start Date is required.");
            lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return;
        }

        LocalDate dueVal = btnDueDate.getValue();
        if (dueVal == null) {
            lblAddStatus.setText("Error: Due Date is required.");
            lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return;
        }

        if (startVal.isAfter(dueVal)) {
            lblAddStatus.setText("Error: Start Date cannot be after Due Date.");
            lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return;
        }

        String categoryString = btnCategory.getValue();
        int categoryInt = 3;
        if (categoryString != null) {
            if (categoryString.contains("Work")) categoryInt = 1;
            else if (categoryString.contains("Health")) categoryInt = 2;
            else if (categoryString.contains("Personal")) categoryInt = 3;
        }

        int importance = (btnImportance.getValue() != null) ? btnImportance.getValue() : 1;
        double duration = (btnDuration.getValue() != null) ? btnDuration.getValue() : 1.0;

        LocalDateTime startTime = null;
        LocalDate startDate = null;
        LocalDateTime dueDate = dueVal.atTime(23, 59);

        boolean hasTime = txtStartTime.getText() != null && !txtStartTime.getText().isEmpty();

        if (startVal != null && hasTime) {
            try {
                LocalTime parsedTime = LocalTime.parse(txtStartTime.getText());
                startTime = startVal.atTime(parsedTime);
                if (startTime != null) {
                    if (DatabaseHelper.hasTimeOverlap(Session.currentUserId, startTime, duration, null)) {
                        lblAddStatus.setText("Error: You already have a task scheduled during this time!");
                        lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        return;
                    }
                }
                startDate = startVal;
            } catch (Exception e) {
                lblAddStatus.setText("Error: Invalid time format. Use HH:mm (e.g., 14:30)");
                lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                return;
            }
        } else if (startVal != null) {
            startDate = startVal;
            startTime = null;
        }

        String userID = Session.currentUserId; 
        DatabaseHelper.addTask(userID, addName, categoryInt, importance, duration, dueDate, startTime, startDate);

        String taskType = (startTime != null) ? "pinned" : "floating";
        lblAddStatus.setText("Success: '" + addName + "' added as " + taskType + " task!");
        lblAddStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

        btnAddName.clear();
        txtStartTime.clear();

        onClickRefreshTasks(null);
    }
    // endregion

    // region Edit Task Logic

    // Search for tasks based on the user's input in the search bar, and dynamically generate editable cards for each matching task.
    //Uses Hbox to display task details and allows users to edit task properties or mark them as completed. 
    // Handles both pinned and floating tasks.
    //The hbox is stored in the editTaskContainer(Vbox) for display. Similar implementation in view tasks tab
    // The method also handles database updates when the user saves changes or marks a task as completed.
    @FXML
    public void onClickSearchTasks(ActionEvent event) {
        editTaskContainer.getChildren().clear();
        lblEditStatus.setText("");

        String searchQuery = txtSearchTask.getText() != null ? txtSearchTask.getText().toLowerCase().trim() : "";
        String userID = Session.currentUserId; 
        String sql = "SELECT * FROM tasks WHERE user_id = ? AND (completed = 0 OR completed IS NULL)";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("task_name");
                if (searchQuery.isEmpty() || name.toLowerCase().contains(searchQuery)) {
                    String id = rs.getString("task_id");
                    int cat = rs.getInt("category");
                    int imp = rs.getInt("importance");
                    double dur = rs.getDouble("duration");

                    Object startObj = rs.getObject("start_time");
                    LocalDateTime start = null;
                    if (startObj != null) {
                        start = (startObj instanceof LocalDateTime) ? (LocalDateTime) startObj : LocalDateTime.parse(startObj.toString().replace(" ", "T"));
                    }

                    Object dueObj = rs.getObject("due_date");
                    LocalDateTime due = null;
                    if (dueObj != null) {
                        due = (dueObj instanceof LocalDateTime) ? (LocalDateTime) dueObj : LocalDateTime.parse(dueObj.toString().replace(" ", "T"));
                    }

                    Object startDateObj = rs.getObject("start_date");
                    LocalDate startDate = null;
                    if (startDateObj != null) {
                        startDate = (startDateObj instanceof LocalDate) ? (LocalDate) startDateObj : LocalDate.parse(startDateObj.toString());
                    }

                    boolean isPinned = (start != null);

                    HBox editCard = new HBox(25);
                    editCard.setAlignment(Pos.CENTER_LEFT);
                    editCard.setStyle("-fx-padding: 25; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-border-width: 2; -fx-background-color: white;");

                    VBox inputsBox = new VBox(20);
                    HBox.setHgrow(inputsBox, Priority.ALWAYS);

                    HBox row1 = new HBox(20);
                    row1.setAlignment(Pos.CENTER_LEFT);
                    TextField editName = new TextField(name);
                    editName.setPrefWidth(300); editName.setPrefHeight(40);
                    ComboBox<String> editCat = new ComboBox<>();
                    editCat.getItems().addAll("1. Work", "2. Health", "3. Personal");
                    editCat.getSelectionModel().select(cat - 1);
                    editCat.setPrefWidth(180); editCat.setPrefHeight(40);
                    row1.getChildren().addAll(new Label("Name:"), editName, new Label("Cat:"), editCat);

                    HBox row2 = new HBox(20);
                    row2.setAlignment(Pos.CENTER_LEFT);
                    Spinner<Integer> editImp = new Spinner<>(1, 5, imp);
                    editImp.setPrefWidth(100); editImp.setPrefHeight(40);
                    Spinner<Double> editDur = new Spinner<>(0.0, 24.0, dur, 0.25);
                    editDur.setPrefWidth(100); editDur.setPrefHeight(40);
                    row2.getChildren().addAll(new Label("Imp:"), editImp, new Label("Dur (hrs):"), editDur);

                    HBox row3 = new HBox(20);
                    row3.setAlignment(Pos.CENTER_LEFT);
                    DatePicker editStartDate = new DatePicker();
                    if (start != null) editStartDate.setValue(start.toLocalDate());
                    else if (startDate != null) editStartDate.setValue(startDate);
                    editStartDate.setPrefWidth(150); editStartDate.setPrefHeight(40);

                    TextField editStartTime = new TextField();
                    if (isPinned && start != null) {
                        editStartTime.setText(start.toLocalTime().toString());
                    }
                    editStartTime.setPromptText("HH:mm (for pinned)");
                    editStartTime.setPrefWidth(150); editStartTime.setPrefHeight(40);
                    row3.getChildren().addAll(new Label("Start Date:"), editStartDate, new Label("Time:"), editStartTime);

                    HBox row4 = new HBox(20);
                    row4.setAlignment(Pos.CENTER_LEFT);
                    DatePicker editDueDate = new DatePicker();
                    if (due != null) editDueDate.setValue(due.toLocalDate());
                    editDueDate.setPrefWidth(150); editDueDate.setPrefHeight(40);
                    row4.getChildren().addAll(new Label("Due Date:"), editDueDate);

                    Label taskTypeLabel = new Label("Type: " + (isPinned ? "📌 Pinned" : "📋 Floating"));
                    taskTypeLabel.setStyle((isPinned ? "-fx-text-fill: #2196F3;" : "-fx-text-fill: #FF9800;") + " -fx-font-weight: bold; -fx-font-size: 15px;");
                    inputsBox.getChildren().addAll(row1, row2, row3, row4, taskTypeLabel);

                    VBox actionBox = new VBox(15);
                    actionBox.setAlignment(Pos.CENTER);

                    Button btnSave = new Button("Save Changes");
                    btnSave.setPrefHeight(45);
                    btnSave.setPrefWidth(140);
                    btnSave.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");

                    btnSave.setOnAction(e -> {
                        try {
                            String newName = editName.getText();
                            int newCat = editCat.getSelectionModel().getSelectedIndex() + 1;
                            int newImp = editImp.getValue();
                            double newDur = editDur.getValue();

                            LocalDate newStartVal = editStartDate.getValue();
                            LocalDateTime newStart = null;
                            LocalDateTime newDue = null;
                            LocalDate newStartDate = null;

                            LocalDate newDueVal = editDueDate.getValue();
                            if (newDueVal != null) newDue = newDueVal.atTime(23, 59);

                            boolean hasEditTime = !editStartTime.getText().trim().isEmpty();

                            if (newStartVal != null) {
                                if (hasEditTime) {
                                    LocalTime pt = LocalTime.parse(editStartTime.getText().trim());
                                    newStart = newStartVal.atTime(pt);
                                    if (DatabaseHelper.hasTimeOverlap(Session.currentUserId, newStart, newDur, id)) {
                                        lblEditStatus.setText("Error: This time overlaps with an existing task!");
                                        lblEditStatus.setStyle("-fx-text-fill: #ef4444;");
                                        return;
                                    }
                                } else {
                                    newStartDate = newStartVal;
                                    newStart = null;
                                }
                            }

                            DatabaseHelper.updateTaskName(id, newName);
                            DatabaseHelper.updateTaskCategory(id, newCat);
                            DatabaseHelper.updateTaskImportance(id, newImp);
                            DatabaseHelper.updateTaskDuration(id, newDur);
                            DatabaseHelper.updateTaskStartTime(id, newStart);
                            DatabaseHelper.updateTaskDueDate(id, newDue);
                            DatabaseHelper.updateTaskStartDate(id, newStartDate);

                            lblEditStatus.setText("Task Updated Successfully!");
                            lblEditStatus.setStyle("-fx-text-fill: #10b981;");
                            onClickRefreshTasks(null); // Keep timeline synced
                        } catch (Exception ex) {
                            lblEditStatus.setText("Error: " + ex.getMessage());
                            lblEditStatus.setStyle("-fx-text-fill: #ef4444;");
                        }
                    });

                    Button btnComplete = new Button("✅ Complete");
                    btnComplete.setPrefHeight(45);
                    btnComplete.setPrefWidth(140);
                    btnComplete.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 14px; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-cursor: hand;");
                    btnComplete.setOnMouseEntered(e -> btnComplete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px; -fx-border-color: #f87171; -fx-border-radius: 4; -fx-cursor: hand;"));
                    btnComplete.setOnMouseExited(e -> btnComplete.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 14px; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-cursor: hand;"));
                    btnComplete.setOnAction(e -> {
                        DatabaseHelper.markTaskCompleted(id);
                        onClickSearchTasks(null); // Instantly remove from list
                        onClickRefreshTasks(null); // Refresh timeline behind the scenes
                    });

                    actionBox.getChildren().addAll(btnSave, btnComplete);
                    editCard.getChildren().addAll(inputsBox, actionBox);
                    editTaskContainer.getChildren().add(editCard);
                }
            }
        } catch (Exception e) {
            System.out.println("Error searching tasks: " + e.getMessage());
        }
    }
    // endregion

    // region Navigation
    @FXML public void onClickBtnNavAddTask(ActionEvent event) { mainTabPane.getSelectionModel().select(tabAdd); }
    @FXML public void onClickBtnNavViewTasks(ActionEvent event) { mainTabPane.getSelectionModel().select(tabView); }
    @FXML public void onClickBtnNavEditTasks(ActionEvent event) { mainTabPane.getSelectionModel().select(tabEdit); }

    public void onClickbtnPref(ActionEvent event) { switchScene(event, "Questions.fxml"); }
    public void onClickbtnSignOut(ActionEvent event) { switchScene(event, "login.fxml"); }
    public void onClickbtnHelp(ActionEvent event) { switchScene(event, "help.fxml"); }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Scene newScene = new Scene(root);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(newScene);
            window.show();
        } catch (Exception e) {
            System.out.println("Error loading screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // endregion

    // region View Tasks Logic and Timeline Rendering

    // Fetch all tasks for the current user from the database, including pinned and floating tasks
    private List<Task> fetchAllTasksFromDB() {
        List<Task> allTasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE user_id = ? AND (completed = 0 OR completed IS NULL) ORDER BY task_id";
        
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, Session.currentUserId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("task_id");
                String name = rs.getString("task_name");
                int cat = rs.getInt("category");
                int imp = rs.getInt("importance");
                double dur = rs.getDouble("duration");

                Object dueObj = rs.getObject("due_date");
                LocalDateTime due = null;
                if (dueObj != null) {
                    due = (dueObj instanceof LocalDateTime) ? (LocalDateTime) dueObj : LocalDateTime.parse(dueObj.toString().replace(" ", "T"));
                }

                Object startObj = rs.getObject("start_time");
                LocalDateTime start = null;
                if (startObj != null) {
                    start = (startObj instanceof LocalDateTime) ? (LocalDateTime) startObj : LocalDateTime.parse(startObj.toString().replace(" ", "T"));
                }

                Object startDateObj = rs.getObject("start_date");
                LocalDate startDate = null;
                if (startDateObj != null) {
                    startDate = (startDateObj instanceof LocalDate) ? (LocalDate) startDateObj : LocalDate.parse(startDateObj.toString());
                }

                allTasks.add(new Task(id, name, cat, imp, dur, due, start, startDate));
            }
        } catch (Exception e) {
            System.out.println("Error fetching tasks: " + e.getMessage());
        }
        return allTasks;
    }

    // Split the fetched tasks into pinned and floating lists, while also tracking originally pinned task IDs
    private void splitTasks(List<Task> allTasks, List<Task> pinnedTasks, List<Task> floatingTasks, Set<String> originallyPinnedIds) {
        for (Task t : allTasks) {
            if (t.isPinned()) {
                pinnedTasks.add(t);
                originallyPinnedIds.add(t.taskId);
            } else {
                floatingTasks.add(t);
            }
        }
    }

    //Clone the floating tasks to avoid modifying the original list when scheduling
    private List<Task> cloneFloatingTasks(List<Task> floatingTasks) { 
        List<Task> floatingClones = new ArrayList<>();
        for (Task t : floatingTasks) {
            floatingClones.add(new Task(
                t.taskId, t.taskName, t.category, t.importance, t.duration,
                t.originalDueDate, null, t.originalStartDate
            ));
        }
        return floatingClones;
    }

    //Build a Questionnaire object based on the user's preferences stored in the database
    private Questionnaire buildPreferences() {
         int currentMode = Questionnaire.PersonalMode;
         if (rdbtnWork.isSelected()) currentMode = Questionnaire.WorkMode;
        else if (rdbtnHealth.isSelected()) currentMode = Questionnaire.HealthMode;
        else if (rdbtnPersonal.isSelected()) currentMode = Questionnaire.PersonalMode;

        int peakProdTime = Questionnaire.MorningPeriod;
        boolean prefQuickFirst = true;
        int dailyProdHours = 8;
        String startTime = "07:00";
        String endTime = "22:00";

        DatabaseHelper.Preferences dbPrefs = DatabaseHelper.getPreferences(Session.currentUserId);
    
     if (dbPrefs != null) {
            peakProdTime = dbPrefs.peakProdTime;
            prefQuickFirst = dbPrefs.preferQuickFirst;
            dailyProdHours = dbPrefs.dailyProdHours;
            if (dbPrefs.startTime != null) startTime = dbPrefs.startTime;
            if (dbPrefs.endTime != null) endTime = dbPrefs.endTime;
    }

        Questionnaire prefs = new Questionnaire(currentMode, peakProdTime, prefQuickFirst, dailyProdHours);
        prefs.setWindow(startTime, endTime);
    
        return prefs;
    }


    //Get the selected scheduling algorithm based on the user's choice in the ComboBox
    private SchedulingAlg getSelectedScheduler() {
        String selection = cmbSchedulerType.getValue();
        if ("Deadline Scheduler".equals(selection)) {
            return new DeadlineScheduler();
        } else if ("Knapsack Scheduler".equals(selection)) {
            return new KnapsackScheduler();
        }
        return new Scheduler();
    }

    // Filter the tasks to only include those that are relevant for the selected date, considering both pinned and floating tasks
    private List<Task> filterTasksForDate(List<Task> displayTasks, LocalDate viewDate, Set<String> originallyPinnedIds) {
        List<Task> tasksForToday = new ArrayList<>();

        for (Task t : displayTasks) {
            LocalDate taskStart = null;
            LocalDate taskEnd = null;

            if (t.startTime != null && !originallyPinnedIds.contains(t.taskId)) {
                taskStart = t.startTime.toLocalDate();
                taskEnd = taskStart;
            } else {
                if (t.originalStartDate != null) taskStart = t.originalStartDate;
                else if (t.startDate != null) taskStart = t.startDate;
                else if (t.startTime != null) taskStart = t.startTime.toLocalDate();

                if (t.originalDueDate != null) taskEnd = t.originalDueDate.toLocalDate();
                else if (t.dueDate != null) taskEnd = t.dueDate.toLocalDate();
                else if (t.startTime != null) taskEnd = t.startTime.toLocalDate();
            }

            if (taskStart == null || taskEnd == null) continue;

            boolean inRange = (viewDate.isEqual(taskStart) || viewDate.isAfter(taskStart)) &&
                              (viewDate.isEqual(taskEnd) || viewDate.isBefore(taskEnd));
            if (inRange) {
                tasksForToday.add(t);
            }
        }

        tasksForToday.sort(Comparator.comparing(t -> {
            if (t.startTime != null) return t.startTime.toLocalTime();
            if (t.startDate != null) return t.startDate.atStartOfDay().toLocalTime();
            if (t.dueDate != null) return t.dueDate.toLocalTime();
            return LocalTime.MAX; 
        }));

        return tasksForToday;
    }

    // CALENDAR TIMELINE RENDERING

    // Create a visual card for a task, including its name, category, type, importance, and time details. Creates Hbox with dynamic 
    // styling based on task properties. The card also includes a button to mark the task as completed.
    private HBox createTaskCard(Task t, Set<String> originallyPinnedIds, DateTimeFormatter dateFormatter, double setHeight) {
        HBox taskCard = new HBox(15);
        taskCard.setAlignment(Pos.TOP_LEFT); 

        String baseStyle = "-fx-padding: 10; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 0 0 0 6; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); ";
        String catString;
        if (t.category == 1) {
            catString = "Work";
            taskCard.setStyle(baseStyle + "-fx-background-color: #E3F2FD; -fx-border-color: #2196F3;");
        } else if (t.category == 2) {
            catString = "Health";
            taskCard.setStyle(baseStyle + "-fx-background-color: #E8F5E9; -fx-border-color: #4CAF50;");
        } else {
            catString = "Personal";
            taskCard.setStyle(baseStyle + "-fx-background-color: #FFF3E0; -fx-border-color: #FF9800;");
        }

        String taskType = originallyPinnedIds.contains(t.taskId) ? "📌 Pinned" : (t.startTime != null ? "📋 Scheduled" : "📋 Floating");

        // Text Container
        VBox textBox = new VBox(5);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        
        Label nameLabel = new Label(t.taskName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1e293b;");
        nameLabel.setWrapText(true);

        //Dynamic Importance Color Label
        String impColor;
        switch (t.importance) {
            case 5: impColor = "#ef4444"; break; // Red
            case 4: impColor = "#f97316"; break; // Orange
            case 3: impColor = "#eab308"; break; // Yellow
            case 2: impColor = "#3b82f6"; break; // Blue
            default: impColor = "#10b981"; break; // Green
        }
        
        Label detailsStart = new Label(catString + "  |  " + taskType + "  |  ");
        detailsStart.setStyle("-fx-text-fill: dimgray; -fx-font-size: 12px;");
        
        Label impLabel = new Label(" Importance Level: " + t.importance);
        impLabel.setStyle("-fx-background-color: " + impColor + "; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 10px;");

        HBox topDetails = new HBox(3, detailsStart, impLabel);
        topDetails.setAlignment(Pos.CENTER_LEFT);

        textBox.getChildren().addAll(nameLabel, topDetails);

        //Append time info for floating tasks that are unscheduled, i.e floating tasks that arent scheduled.
        if (t.startTime == null) {
            String timeStr = "Duration: " + t.duration + " hrs";
            if (t.originalStartDate != null && t.originalDueDate != null) {
                timeStr += "  |  Starts: " + t.originalStartDate.format(dateFormatter) + " - Due: " + t.originalDueDate.format(dateFormatter);
            } else if (t.startDate != null && t.dueDate != null) {
                timeStr += "  |  Starts: " + t.startDate.format(dateFormatter) + " - Due: " + t.dueDate.format(dateFormatter);
            } else if (t.startDate != null) {
                timeStr += "  |  Starts: " + t.startDate.format(dateFormatter);
            } else if (t.dueDate != null) {
                timeStr += "  |  Due: " + t.dueDate.format(dateFormatter);
            }
            if (!timeStr.isEmpty()) {
                Label timeDetailsLabel = new Label(timeStr);
                timeDetailsLabel.setStyle("-fx-text-fill: dimgray; -fx-font-size: 12px;");
                timeDetailsLabel.setWrapText(true);
                textBox.getChildren().add(timeDetailsLabel);
            }
        }

        // One-Click Completion Bin Button
        Button btnComplete = new Button("✅");
        btnComplete.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 0 5 0 5;");
        
        btnComplete.setOnMouseEntered(e -> btnComplete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 0 5 0 5;"));
        btnComplete.setOnMouseExited(e -> btnComplete.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 0 5 0 5;"));
        
        btnComplete.setOnAction(e -> {
            DatabaseHelper.markTaskCompleted(t.taskId);
            onClickRefreshTasks(null);
        });

        taskCard.getChildren().addAll(textBox, btnComplete);

        if (setHeight > 0) {
            taskCard.setMinHeight(setHeight);
            taskCard.setMaxHeight(setHeight);
        }

        return taskCard;
    }

    // Render the tasks for the selected date in a timeline view, handling both pinned, scheduled floating tasks, and "All day tasks"
    // and ensuring proper placement and collision management
    private void renderTasks(List<Task> tasksForToday, Set<String> originallyPinnedIds) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd");

        Questionnaire prefs = buildPreferences();
        int startDayHour = prefs.getDayStartTime().getHour();
        int endDayHour = prefs.getDayEndTime().getHour();

        List<Task> timedTasks = new ArrayList<>();
        List<Task> allDayTasks = new ArrayList<>();

        for (Task t : tasksForToday) {
            if (t.startTime != null) {
                timedTasks.add(t);
            } else {
                allDayTasks.add(t);
            }
        }

        // EXPAND TIMELINE BOUNDS FOR PINNED TASKS, working window does not restrict pinned tasks, 
        // but we want to show them in the timeline
        for (Task t : timedTasks) {
            int tStartH = t.startTime.getHour();
            int durationMins = (int)(t.duration * 60);
            int endMinsFromMidnight = tStartH * 60 + t.startTime.getMinute() + durationMins;
            int tEndH = (int) Math.ceil(endMinsFromMidnight / 60.0);
            if (tEndH > 24) tEndH = 24;

            if (startDayHour <= endDayHour) { // Standard Day
                if (tStartH < startDayHour && tStartH >= 0) startDayHour = tStartH;
                if (tEndH > endDayHour) endDayHour = tEndH;
            }
        }

        int loopEnd = endDayHour;
        if (endDayHour < startDayHour) {
            loopEnd += 24; 
        }

        taskContainer.getChildren().clear();

        // Render "All Day" Tasks
        if (!allDayTasks.isEmpty()) {
            HBox allDayRow = new HBox(20);
            allDayRow.setStyle("-fx-padding: 10 0 20 0; -fx-border-color: #d3d3d3; -fx-border-width: 0 0 2 0;");
            
            Label timeLabel = new Label("All Day");
            timeLabel.setPrefWidth(100);
            timeLabel.setMinWidth(100);
            timeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #333333;");
            timeLabel.setAlignment(Pos.TOP_RIGHT); 

            VBox container = new VBox(10);
            HBox.setHgrow(container, Priority.ALWAYS);
            allDayRow.getChildren().addAll(timeLabel, container);

            for (Task t : allDayTasks) {
                container.getChildren().add(createTaskCard(t, originallyPinnedIds, dateFormatter, -1));
            }
            taskContainer.getChildren().add(allDayRow);
        }

        // Render Full Calendar Timeline Grid
        AnchorPane timeline = new AnchorPane();
        double ROW_HEIGHT = 80.0;
        double TIME_COL_WIDTH = 100.0;
        int totalHours = loopEnd - startDayHour + 1;

        // Draw background grid lines and time labels
        for (int i = 0; i < totalHours; i++) {
            int displayHour = (startDayHour + i) % 24;
            LocalTime hourTime = LocalTime.of(displayHour, 0);

            Label timeLabel = new Label(hourTime.format(DateTimeFormatter.ofPattern("hh:00 a")));
            timeLabel.setPrefWidth(TIME_COL_WIDTH);
            timeLabel.setAlignment(Pos.TOP_RIGHT);
            timeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #333333;");
            
            AnchorPane.setLeftAnchor(timeLabel, 0.0);
            AnchorPane.setTopAnchor(timeLabel, i * ROW_HEIGHT + 10);

            Pane line = new Pane();
            line.setStyle("-fx-background-color: #e2e8f0;");
            line.setPrefHeight(2);
            
            AnchorPane.setLeftAnchor(line, TIME_COL_WIDTH + 20);
            AnchorPane.setRightAnchor(line, 20.0);
            AnchorPane.setTopAnchor(line, i * ROW_HEIGHT + 20);

            timeline.getChildren().addAll(timeLabel, line);
        }

        timeline.setMinHeight(totalHours * ROW_HEIGHT + 50);

        List<TaskPlacement> placements = new ArrayList<>();

        for (Task t : timedTasks) {
            LocalTime tStart = t.startTime.toLocalTime();
            int h = tStart.getHour();
            int m = tStart.getMinute();
            
            int effectiveStartHour = h;
            if (h < startDayHour) {
                effectiveStartHour += 24;
            }

            double startY = (effectiveStartHour - startDayHour) * ROW_HEIGHT + (m / 60.0) * ROW_HEIGHT + 20;
            double height = t.duration * ROW_HEIGHT;
            height -= 4; // visual gap to not block line

            int col = 0;
            boolean collision;
            do {
                collision = false;
                for (TaskPlacement p : placements) {
                    if (p.col == col) {
                        if (startY < p.endY && startY + height > p.startY) {
                            collision = true;
                            col++;
                            break;
                        }
                    }
                }
            } while (collision);

            placements.add(new TaskPlacement(startY, startY + height, col));

            HBox card = createTaskCard(t, originallyPinnedIds, dateFormatter, height);
            
            double startX = TIME_COL_WIDTH + 30 + (col * 40.0); 

            AnchorPane.setTopAnchor(card, startY);
            AnchorPane.setLeftAnchor(card, startX);
            AnchorPane.setRightAnchor(card, 20.0); 
            
            card.setMinHeight(height);
            card.setMaxHeight(height);

            timeline.getChildren().add(card);
        }

        taskContainer.getChildren().add(timeline);
    }
    
    @FXML
    public void onClickRefreshTasks(ActionEvent event) {
        taskContainer.getChildren().clear();
        
        List<Task> allTasks = fetchAllTasksFromDB();
        if (!allTasks.isEmpty()) {
            List<Task> pinnedTasks = new ArrayList<>();
            List<Task> floatingTasks = new ArrayList<>();
            Set<String> originallyPinnedIds = new HashSet<>();

            splitTasks(allTasks, pinnedTasks, floatingTasks, originallyPinnedIds);
            
            List<Task> allClones = new ArrayList<>();
            allClones.addAll(pinnedTasks);
            allClones.addAll(cloneFloatingTasks(floatingTasks));
            
            Questionnaire prefs = buildPreferences();
            SchedulingAlg scheduler = getSelectedScheduler();
            
            List<Task> scheduledFloating = scheduler.genSchedule(allClones, prefs);
            
            List<Task> displayTasks = new ArrayList<>();
            displayTasks.addAll(pinnedTasks);
            displayTasks.addAll(scheduledFloating);
            
            LocalDate viewDate = dpViewDate.getValue();
            if (viewDate == null) viewDate = LocalDate.now();
            
            List<Task> tasksForToday = filterTasksForDate(displayTasks, viewDate, originallyPinnedIds);
            renderTasks(tasksForToday, originallyPinnedIds);
        }

        onClickRefreshCompletedTasks(null);
    }
    // endregion

    // region Completed Tasks
    @FXML
    public void onClickRefreshCompletedTasks(ActionEvent event) {
        completedTaskContainer.getChildren().clear();
        lblCompletedStatus.setText("");

        String userID = Session.currentUserId;
        String sql = "SELECT * FROM tasks WHERE user_id = ? AND completed = 1 ORDER BY task_id";

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("task_id");
                String name = rs.getString("task_name");
                int cat = rs.getInt("category");

                Object dueObj = rs.getObject("due_date");
                LocalDateTime due = null;
                if (dueObj != null) {
                    due = (dueObj instanceof LocalDateTime) ? (LocalDateTime) dueObj : LocalDateTime.parse(dueObj.toString().replace(" ", "T"));
                }

                String catString = (cat == 1) ? "Work" : (cat == 2) ? "Health" : "Personal";
                String dueString = (due != null) ? "Was due: " + due.format(dateFormatter) : "No due date";

                HBox card = new HBox(15);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-padding: 10; -fx-border-color: #cbd5e1; -fx-border-radius: 8; " +
                              "-fx-background-radius: 8; -fx-background-color: #f8f9fa;");

                VBox textBox = new VBox(4);
                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #94a3b8;"); 
                Label detailsLabel = new Label(catString + "  |  " + dueString);
                detailsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
                textBox.getChildren().addAll(nameLabel, detailsLabel);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                Button btnRestore = new Button("Restore");
                btnRestore.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6;");
                LocalDateTime dueForClosure = due;
                btnRestore.setOnAction(e -> restoreCompletedTask(id, dueForClosure));

                card.getChildren().addAll(textBox, btnRestore);
                completedTaskContainer.getChildren().add(card);
            }

        } catch (Exception e) {
            System.out.println("Error fetching completed tasks: " + e.getMessage());
        }
    }

    private void restoreCompletedTask(String taskId, LocalDateTime dueDate) {
        boolean overdue = dueDate != null && dueDate.isBefore(LocalDateTime.now());

        if (overdue) {
            DatePicker newDueDatePicker = new DatePicker(LocalDate.now().plusDays(1));

            Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
            dialog.setTitle("Restore Task");
            dialog.setHeaderText("This task's due date has already passed.");
            VBox content = new VBox(10, new Label("Choose a new due date to restore it:"), newDueDatePicker);
            content.setStyle("-fx-padding: 10 0 0 0;");
            dialog.getDialogPane().setContent(content);

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    LocalDate newDue = newDueDatePicker.getValue();
                    if (newDue == null) {
                        lblCompletedStatus.setText("Error: Please choose a due date.");
                        lblCompletedStatus.setStyle("-fx-text-fill: #ef4444;");
                        return;
                    }
                    DatabaseHelper.updateTaskDueDate(taskId, newDue.atTime(23, 59));
                    DatabaseHelper.restoreTask(taskId);
                    onClickRefreshTasks(null); // Refreshes both seamlessly
                }
            });
        } else {
            DatabaseHelper.restoreTask(taskId);
            onClickRefreshTasks(null); // Refreshes both seamlessly
        }
    }

    @FXML
    public void onClickClearCompleted(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Completed Tasks");
        confirm.setHeaderText("This will permanently delete all completed tasks.");
        confirm.setContentText("Are you sure you want to continue? This cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                DatabaseHelper.clearCompletedTasks(Session.currentUserId);
                onClickRefreshCompletedTasks(event);
                lblCompletedStatus.setText("Completed tasks cleared.");
                lblCompletedStatus.setStyle("-fx-text-fill: #10b981;");
            }
        });
    }
    // endregion
}