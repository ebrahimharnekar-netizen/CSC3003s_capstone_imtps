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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;

public class IMTPSController {
    @FXML private Label lblUsernameIMTPS;
    
    // Taskbar Navigation Element
    @FXML private TabPane mainTabPane;

    // Add Task Tab Elements
    @FXML private TextField btnAddName;
    @FXML private ComboBox<String> btnCategory;
    @FXML private Spinner<Integer> btnImportance;
    @FXML private Spinner<Double> btnDuration;
    @FXML private DatePicker btnStartDate;
    @FXML private TextField txtStartTime;
    @FXML private DatePicker btnDueDate;
    @FXML private Label lblAddStatus;

    // View Tasks Tab Elements
    @FXML private VBox taskContainer;
    @FXML private DatePicker dpViewDate;
    @FXML private ComboBox<String> cmbSchedulerType;
    @FXML private RadioButton rdbtnWork;
    @FXML private RadioButton rdbtnHealth;
    @FXML private RadioButton rdbtnPersonal;

    // Edit Tasks Tab Elements
    @FXML private TextField txtSearchTask;
    @FXML private VBox editTaskContainer;
    @FXML private Label lblEditStatus;

    // Completed Tasks Tab Elements
    @FXML private VBox completedTaskContainer;
    @FXML private Label lblCompletedStatus;

    private static final LocalTime START_OF_DAY = LocalTime.of(0, 0);

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

        // Initialize Scheduler ComboBox
        cmbSchedulerType.getItems().addAll("Priority Scheduler", "Deadline Scheduler", "Knapsack Scheduler");
        cmbSchedulerType.getSelectionModel().selectFirst();

        Platform.runLater(() -> {
            Stage stage = (Stage) lblUsernameIMTPS.getScene().getWindow();
            stage.setMaximized(true);
        });

        // Auto-refresh each tab's content when the user switches to it, instead of
        // requiring a manual "Refresh"/"Search" click every time.
        mainTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            int tab = newIndex.intValue();
            if (tab == 1) {
                onClickRefreshTasks(null);
            } else if (tab == 2) {
                onClickRefreshCompletedTasks(null);
            } else if (tab == 3) {
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
                if (startTime != null && !startTime.toLocalTime().equals(START_OF_DAY)) {
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

        String taskType = (startTime != null && !startTime.toLocalTime().equals(START_OF_DAY))
                ? "pinned" : "floating";
        lblAddStatus.setText("Success: '" + addName + "' added as " + taskType + " task!");
        lblAddStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        btnAddName.clear();
        txtStartTime.clear();

        // Keep the View tasks list current so it doesn't need a manual refresh
        // next time the user switches to it.
        onClickRefreshTasks(null);
    }
    // endregion

    // region Edit Task Logic
    @FXML
    public void onClickSearchTasks(ActionEvent event) {
        editTaskContainer.getChildren().clear();
        lblEditStatus.setText("");

        String searchQuery = txtSearchTask.getText().toLowerCase().trim();
        String userID = Session.currentUserId; 
        String sql = "SELECT * FROM tasks WHERE user_id = ?";

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

                    boolean isPinned = (start != null && !start.toLocalTime().equals(START_OF_DAY));

                    HBox editCard = new HBox(15);
                    editCard.setAlignment(Pos.CENTER_LEFT);
                    editCard.setStyle("-fx-padding: 10; -fx-border-color: gray; -fx-border-radius: 5;");

                    VBox inputsBox = new VBox(10);

                    HBox row1 = new HBox(10);
                    TextField editName = new TextField(name);
                    editName.setPrefWidth(200);
                    ComboBox<String> editCat = new ComboBox<>();
                    editCat.getItems().addAll("1. Work", "2. Health", "3. Personal");
                    editCat.getSelectionModel().select(cat - 1);
                    row1.getChildren().addAll(new Label("Name:"), editName, new Label("Cat:"), editCat);

                    HBox row2 = new HBox(10);
                    Spinner<Integer> editImp = new Spinner<>(1, 5, imp);
                    editImp.setPrefWidth(70);
                    Spinner<Double> editDur = new Spinner<>(0.0, 24.0, dur, 0.25);
                    editDur.setPrefWidth(70);
                    row2.getChildren().addAll(new Label("Imp:"), editImp, new Label("Dur (hrs):"), editDur);

                    HBox row3 = new HBox(10);
                    DatePicker editStartDate = new DatePicker();
                    if (start != null) editStartDate.setValue(start.toLocalDate());
                    else if (startDate != null) editStartDate.setValue(startDate);
                    editStartDate.setPrefWidth(120);

                    TextField editStartTime = new TextField();
                    if (isPinned && start != null) {
                        editStartTime.setText(start.toLocalTime().toString());
                    }
                    editStartTime.setPromptText("HH:mm (for pinned)");
                    editStartTime.setPrefWidth(120);
                    row3.getChildren().addAll(new Label("Start Date:"), editStartDate, new Label("Time:"), editStartTime);

                    HBox row4 = new HBox(10);
                    DatePicker editDueDate = new DatePicker();
                    if (due != null) editDueDate.setValue(due.toLocalDate());
                    editDueDate.setPrefWidth(120);
                    row4.getChildren().addAll(new Label("Due Date:"), editDueDate);

                    Label taskTypeLabel = new Label("Type: " + (isPinned ? "📌 Pinned" : "📋 Floating"));
                    taskTypeLabel.setStyle((isPinned ? "-fx-text-fill: #2196F3;" : "-fx-text-fill: #FF9800;") + " -fx-font-weight: bold;");
                    inputsBox.getChildren().addAll(row1, row2, row3, row4, taskTypeLabel);

                    Button btnSave = new Button("Save Changes");
                    btnSave.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

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
                                    if (!pt.equals(START_OF_DAY)) {
                                        if (DatabaseHelper.hasTimeOverlap(Session.currentUserId, newStart, newDur, id)) {
                                            lblEditStatus.setText("Error: This time overlaps with an existing task!");
                                            lblEditStatus.setStyle("-fx-text-fill: red;");
                                            return;
                                        }
                                    } else {
                                        newStartDate = newStartVal;
                                        newStart = null;
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
                            lblEditStatus.setStyle("-fx-text-fill: green;");
                        } catch (Exception ex) {
                            lblEditStatus.setText("Error: " + ex.getMessage());
                            lblEditStatus.setStyle("-fx-text-fill: red;");
                        }
                    });

                    editCard.getChildren().addAll(inputsBox, btnSave);
                    editTaskContainer.getChildren().add(editCard);
                }
            }
        } catch (Exception e) {
            System.out.println("Error searching tasks: " + e.getMessage());
        }
    }
    // endregion

    // region Navigation
    @FXML public void onClickBtnNavAddTask(ActionEvent event) { mainTabPane.getSelectionModel().select(0); }
    @FXML public void onClickBtnNavViewTasks(ActionEvent event) { mainTabPane.getSelectionModel().select(1); }
    @FXML public void onClickBtnNavCompletedTasks(ActionEvent event) { mainTabPane.getSelectionModel().select(2); }
    @FXML public void onClickBtnNavEditTasks(ActionEvent event) { mainTabPane.getSelectionModel().select(3); }

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

    // region Refresh Tasks logic

    // Ticking a task off marks it completed (it moves to the Completed tasks tab)
    // instead of being deleted outright, so it can still be viewed/restored later.
    private void markCheckedTasksCompleted() {
        for (Node node : taskContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox card = (HBox) node;
                CheckBox cb = (CheckBox) card.getChildren().get(0);
                if (cb.isSelected()) {
                    String taskId = (String) cb.getUserData(); 
                    DatabaseHelper.markTaskCompleted(taskId);
                }
            }
        }
    }

    //This method fetches all active (not-yet-completed) tasks from the database for the current user. 
    // It retrieves task details such as ID, name, category, importance, duration, due date, start time, and start date. 
    // The method constructs Task objects for each record and returns a list of all tasks.
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

    //This method splits the list of all tasks into two separate lists: pinned tasks and floating tasks. 
    // It also keeps track of the IDs of the originally pinned tasks in a set for later reference.
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

    //This method creates clones of the floating tasks to ensure that the original task objects remain unchanged during scheduling.
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

    //This method constructs a Questionnaire object based on the user's selected preferences in the UI.
    private Questionnaire buildPreferences() {

     // 1. Get the current mode from the Radio Buttons
         int currentMode = Questionnaire.PersonalMode;
         if (rdbtnWork.isSelected()) currentMode = Questionnaire.WorkMode;
        else if (rdbtnHealth.isSelected()) currentMode = Questionnaire.HealthMode;
        else if (rdbtnPersonal.isSelected()) currentMode = Questionnaire.PersonalMode;

    // 2. Set default values
        int peakProdTime = Questionnaire.MorningPeriod;
        boolean prefQuickFirst = true;
        int dailyProdHours = 8;
        String startTime = "07:00";
        String endTime = "22:00";

    // 3. Fetch the actual preferences using your DatabaseHelper
        DatabaseHelper.Preferences dbPrefs = DatabaseHelper.getPreferences(Session.currentUserId);
    
     if (dbPrefs != null) {
            peakProdTime = dbPrefs.peakProdTime;
            prefQuickFirst = dbPrefs.preferQuickFirst;
            dailyProdHours = dbPrefs.dailyProdHours;
            if (dbPrefs.startTime != null) startTime = dbPrefs.startTime;
            if (dbPrefs.endTime != null) endTime = dbPrefs.endTime;
    }

    // 4. Construct Questionnaire and set the dynamic window
        Questionnaire prefs = new Questionnaire(currentMode, peakProdTime, prefQuickFirst, dailyProdHours);
        prefs.setWindow(startTime, endTime);
    
        return prefs;
    }

    //This method retrieves the selected scheduling algorithm based on the user's choice in the UI.
    private SchedulingAlg getSelectedScheduler() {
        String selection = cmbSchedulerType.getValue();
        if ("Deadline Scheduler".equals(selection)) {
            return new DeadlineScheduler();
        } else if ("Knapsack Scheduler".equals(selection)) {
            return new KnapsackScheduler();
        }
        // Default to Priority Scheduler
        return new Scheduler();
    }

    //This method filters the list of tasks to include only those that are relevant for the specified view date. 
    // It checks each task's start and end dates (or times) to determine if it falls within the range of the view date. 
    // The method also sorts the resulting list of tasks based on their start times, ensuring that tasks with earlier start times appear first in the list.
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

    //This method is responsible for rendering the tasks in the UI. It creates a visual representation of each task, 
    // including its name, category, time information, and importance. The tasks are displayed in a VBox container, 
    // with each task represented as an HBox containing a checkbox and a VBox for text details.

    private void renderTasks(List<Task> tasksForToday, Set<String> originallyPinnedIds) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd");

        for (Task t : tasksForToday) {
            HBox taskCard = new HBox(15);
            taskCard.setAlignment(Pos.CENTER_LEFT);

            String baseStyle = "-fx-padding: 10; -fx-border-radius: 5; -fx-background-radius: 5; -fx-border-width: 0 0 0 6; ";
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

            CheckBox taskCheckbox = new CheckBox();
            taskCheckbox.setUserData(t.taskId);

            String timeString;
            String taskType;
            boolean isPinned = originallyPinnedIds.contains(t.taskId);

            if (isPinned) {
                LocalDateTime endTime = t.startTime.plusMinutes((long)(t.duration * 60));
                timeString = t.startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter);
                taskType = "📌 Pinned";
            } else if (t.startTime != null) {
                LocalDateTime endTime = t.startTime.plusMinutes((long)(t.duration * 60));
                timeString = t.startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter);
                taskType = "📋 Floating (Scheduled)";
            } else if (t.originalStartDate != null && t.originalDueDate != null) {
                timeString = "Starts: " + t.originalStartDate.format(dateFormatter) + " | Due: " + t.originalDueDate.format(dateFormatter);
                taskType = "📋 Floating";
            } else if (t.startDate != null && t.dueDate != null) {
                timeString = "Starts: " + t.startDate.format(dateFormatter) + " | Due: " + t.dueDate.format(dateFormatter);
                taskType = "📋 Floating";
            } else if (t.startDate != null) {
                timeString = "Starts: " + t.startDate.format(dateFormatter);
                taskType = "📋 Floating";
            } else if (t.dueDate != null) {
                timeString = "Due: " + t.dueDate.format(dateFormatter);
                taskType = "📋 Floating";
            } else {
                timeString = "No time set";
                taskType = "📋 Floating";
            }

            VBox textBox = new VBox(5);
            Label nameLabel = new Label(t.taskName);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

            String detailsText = String.format("%s  |  %s  |  %s  |  Importance: %d",
                catString, timeString, taskType, t.importance);
            Label detailsLabel = new Label(detailsText);
            detailsLabel.setStyle("-fx-text-fill: dimgray; -fx-font-size: 12px;");

            textBox.getChildren().addAll(nameLabel, detailsLabel);
            taskCard.getChildren().addAll(taskCheckbox, textBox);
            taskContainer.getChildren().add(taskCard);
        }

    
    }
    //refresh tasks button click handler, utilizes the above methods to delete checked tasks, 
    // fetch all tasks, split them into pinned and floating, schedule the floating tasks, filter for the selected date, 
    // and render them in the UI.
    @FXML
    public void onClickRefreshTasks(ActionEvent event) {
        //First mark any tasks that have been checked as completed, then clear the task container to prepare for re-rendering.
        markCheckedTasksCompleted();
        taskContainer.getChildren().clear();
        //Next, fetch all tasks from the database. If there are no tasks, simply return without further processing.
        List<Task> allTasks = fetchAllTasksFromDB();
        if (allTasks.isEmpty()) return;
        //Split the fetched tasks into pinned and floating tasks, while also keeping track of the IDs of originally pinned tasks.
        List<Task> pinnedTasks = new ArrayList<>();
        List<Task> floatingTasks = new ArrayList<>();
        Set<String> originallyPinnedIds = new HashSet<>();

        splitTasks(allTasks, pinnedTasks, floatingTasks, originallyPinnedIds);
        //Create clones of the floating tasks to ensure that the original task objects remain unchanged during scheduling.
        List<Task> allClones = new ArrayList<>();
        allClones.addAll(pinnedTasks);
        allClones.addAll(cloneFloatingTasks(floatingTasks));
        //Build a Questionnaire object based on the user's selected preferences in the UI, and retrieve the selected scheduling algorithm.
        Questionnaire prefs = buildPreferences();
        SchedulingAlg scheduler = getSelectedScheduler();
        //Generate a schedule for the floating tasks using the selected scheduling algorithm and user preferences.
        List<Task> scheduledFloating = scheduler.genSchedule(allClones, prefs);
        //Combine the pinned tasks and the newly scheduled floating tasks into a single list for display.
        List<Task> displayTasks = new ArrayList<>();
        displayTasks.addAll(pinnedTasks);
        displayTasks.addAll(scheduledFloating);
        //Filter the combined list of tasks to include only those that are relevant for the selected view date, and render them in the UI.
        LocalDate viewDate = dpViewDate.getValue();
        if (viewDate == null) viewDate = LocalDate.now();
        //Filter the combined list of tasks to include only those that are relevant for the selected view date, and render them in the UI.
        List<Task> tasksForToday = filterTasksForDate(displayTasks, viewDate, originallyPinnedIds);
        renderTasks(tasksForToday, originallyPinnedIds);
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
                card.setStyle("-fx-padding: 10; -fx-border-color: lightgray; -fx-border-radius: 5; " +
                              "-fx-background-radius: 5; -fx-background-color: #F0F0F0;");

                VBox textBox = new VBox(4);
                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
                Label detailsLabel = new Label(catString + "  |  " + dueString);
                detailsLabel.setStyle("-fx-text-fill: dimgray; -fx-font-size: 12px;");
                textBox.getChildren().addAll(nameLabel, detailsLabel);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                Button btnRestore = new Button("Restore");
                btnRestore.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                LocalDateTime dueForClosure = due;
                btnRestore.setOnAction(e -> restoreCompletedTask(id, dueForClosure));

                card.getChildren().addAll(textBox, btnRestore);
                completedTaskContainer.getChildren().add(card);
            }

        } catch (Exception e) {
            System.out.println("Error fetching completed tasks: " + e.getMessage());
        }
    }

    // Restores a completed task. If its due date has already passed, the user
    // is asked to pick a new one first so it doesn't come back already overdue.
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
                        lblCompletedStatus.setStyle("-fx-text-fill: red;");
                        return;
                    }
                    DatabaseHelper.updateTaskDueDate(taskId, newDue.atTime(23, 59));
                    DatabaseHelper.restoreTask(taskId);
                    onClickRefreshCompletedTasks(null);
                }
            });
        } else {
            DatabaseHelper.restoreTask(taskId);
            onClickRefreshCompletedTasks(null);
        }
    }

    // Permanently deletes every completed task, after an explicit confirmation
    // (this cannot be undone, so it's gated to help prevent accidental clicks).
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
                lblCompletedStatus.setStyle("-fx-text-fill: green;");
            }
        });
    }
    // endregion
}