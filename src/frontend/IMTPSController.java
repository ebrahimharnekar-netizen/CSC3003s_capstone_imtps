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
import java.util.List;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;

public class IMTPSController {
    @FXML private Label lblUsernameIMTPS;
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
    @FXML private RadioButton rdbtnWork;
    @FXML private RadioButton rdbtnHealth;
    @FXML private RadioButton rdbtnPersonal;

    // Edit Tasks Tab Elements
    @FXML private TextField txtSearchTask;
    @FXML private VBox editTaskContainer;
    @FXML private Label lblEditStatus;

    // Constants
    private static final LocalTime START_OF_DAY = LocalTime.of(0, 0);

    @FXML
    // region Initialization
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

        Platform.runLater(() -> {
            Stage stage = (Stage) lblUsernameIMTPS.getScene().getWindow();
            stage.setWidth(780);
            stage.setHeight(570);
            stage.centerOnScreen();
        });
    }
    // endregion
    
    // region Add Task Logic
    public void onClickBtnAddTask(ActionEvent event) {
        String addName = btnAddName.getText();

        if (addName == null || addName.trim().isEmpty()) {
            lblAddStatus.setText("Error: Task Name cannot be empty.");
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

        LocalDate startVal = btnStartDate.getValue();
        LocalDateTime startTime = null;
        LocalDate startDate = null;
        LocalDate dueVal = btnDueDate.getValue();
        LocalDateTime dueDate = (dueVal != null) ? dueVal.atTime(23, 59) : null;
        
        // Check if user provided a time
        boolean hasTime = txtStartTime.getText() != null && !txtStartTime.getText().isEmpty();
        
        if (startVal != null && hasTime) {
            try {
                LocalTime parsedTime = LocalTime.parse(txtStartTime.getText()); 
                startTime = startVal.atTime(parsedTime);
                
                // Check if this is a PINNED task (has time AND start date matches due date)
                if (dueDate != null && startTime.toLocalDate().equals(dueDate.toLocalDate())) {
                    // Only check overlap for pinned tasks
                    if (DatabaseHelper.hasTimeOverlap(Session.currentUserId, startTime, duration, -1)) {
                        lblAddStatus.setText("Error: You already have a task scheduled during this time!");
                        lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        return;
                    }
                } else {
                    // Start date doesn't match due date - this is a floating task with start date
                    // Store the start date separately
                    startDate = startVal;
                    startTime = null; // Clear start time
                }
            } catch (Exception e) {
                lblAddStatus.setText("Error: Invalid time format. Use HH:mm (e.g., 14:30)");
                lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                return;
            }
        } else if (startVal != null) {
            // Start date with no time - floating task
            startDate = startVal;
            startTime = null;
        }

        int userID = Session.currentUserId; 
        DatabaseHelper.addTask(userID, addName, categoryInt, importance, duration, dueDate, startTime, startDate);
        
        String taskType = (startTime != null && dueDate != null && startTime.toLocalDate().equals(dueDate.toLocalDate()) && hasTime) 
                         ? "pinned" : "floating";
        lblAddStatus.setText("Success: '" + addName + "' added as " + taskType + " task!");
        lblAddStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        
        btnAddName.clear();
        txtStartTime.clear();
    }
    // endregion

    // region Edit Task Logic
    @FXML
    public void onClickSearchTasks(ActionEvent event) {
        editTaskContainer.getChildren().clear();
        lblEditStatus.setText("");
        
        String searchQuery = txtSearchTask.getText().toLowerCase().trim();
        int userID = Session.currentUserId;
        String sql = "SELECT * FROM tasks WHERE user_id = ?";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("task_name");
                
                if (searchQuery.isEmpty() || name.toLowerCase().contains(searchQuery)) {
                    int id = rs.getInt("task_id");
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

                    // Determine if this is a pinned task
                    boolean isPinned = false;
                    LocalDateTime displayStart = null;
                    LocalDate displayStartDate = startDate;
                    String displayTime = "";
                    
                    if (start != null) {
                        // Check if it's a pinned task (has time and start date matches due date)
                        if (due != null && start.toLocalDate().equals(due.toLocalDate())) {
                            // Check if the time is not 00:00 (which would indicate floating with start date)
                            if (!start.toLocalTime().equals(START_OF_DAY)) {
                                isPinned = true;
                                displayStart = start;
                                displayTime = start.toLocalTime().toString();
                            } else {
                                // This is a floating task with start date (00:00 time)
                                displayStartDate = start.toLocalDate();
                            }
                        } else {
                            // Floating task with start date
                            displayStartDate = start.toLocalDate();
                        }
                    }

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
                    if (displayStart != null) {
                        editStartDate.setValue(displayStart.toLocalDate());
                    } else if (displayStartDate != null) {
                        editStartDate.setValue(displayStartDate);
                    }
                    editStartDate.setPrefWidth(120);
                    
                    TextField editStartTime = new TextField();
                    if (isPinned && displayStart != null) {
                        editStartTime.setText(displayStart.toLocalTime().toString());
                    } else if (start != null && start.toLocalTime().equals(START_OF_DAY)) {
                        // Floating task with start date - leave time field empty
                        editStartTime.setText("");
                    }
                    editStartTime.setPromptText("HH:mm (for pinned)");
                    editStartTime.setPrefWidth(120);
                    row3.getChildren().addAll(new Label("Start Date:"), editStartDate, new Label("Time:"), editStartTime);

                    HBox row4 = new HBox(10);
                    DatePicker editDueDate = new DatePicker();
                    if (due != null) editDueDate.setValue(due.toLocalDate());
                    editDueDate.setPrefWidth(120);
                    row4.getChildren().addAll(new Label("Due Date:"), editDueDate);

                    // Show task type indicator
                    Label taskTypeLabel = new Label("Type: " + (isPinned ? "📌 Pinned" : "📋 Floating"));
                    taskTypeLabel.setStyle("-fx-font-weight: bold;");
                    if (isPinned) {
                        taskTypeLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
                    } else {
                        taskTypeLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    }

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
                            if (newDueVal != null) {
                                newDue = newDueVal.atTime(23, 59);
                            }

                            boolean hasTime = !editStartTime.getText().trim().isEmpty();
                            
                            if (newStartVal != null) {
                                if (hasTime) {
                                    try {
                                        LocalTime pt = LocalTime.parse(editStartTime.getText().trim());
                                        newStart = newStartVal.atTime(pt);
                                        
                                        // Only check overlap if this is a valid PINNED task
                                        if (newDue != null && newStart.toLocalDate().equals(newDue.toLocalDate())) {
                                            // Check if it's a real time (not 00:00)
                                            if (!pt.equals(START_OF_DAY)) {
                                                if (DatabaseHelper.hasTimeOverlap(Session.currentUserId, newStart, newDur, id)) {
                                                    lblEditStatus.setText("Error: This time overlaps with an existing task!");
                                                    lblEditStatus.setStyle("-fx-text-fill: red;");
                                                    return;
                                                }
                                            }
                                        } else {
                                            // Start date doesn't match due date - floating task, store start date separately
                                            newStartDate = newStartVal;
                                            newStart = null;
                                        }
                                    } catch (Exception ex) {
                                        lblEditStatus.setText("Error: Invalid time format (Use HH:mm).");
                                        lblEditStatus.setStyle("-fx-text-fill: red;");
                                        return;
                                    }
                                } else {
                                    // Start date with no time - floating task
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

    // region Navigation Buttons
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

    // region Refresh Tasks Logic

    @FXML
    public void onClickRefreshTasks(ActionEvent event) {
        // 1. Delete checked tasks
        for (Node node : taskContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox card = (HBox) node;
                CheckBox cb = (CheckBox) card.getChildren().get(0);
                if (cb.isSelected()) {
                    int taskId = (int) cb.getUserData();
                    DatabaseHelper.deleteTask(taskId); 
                }
            }
        }

        // 2. Clear UI
        taskContainer.getChildren().clear();

        // 3. Fetch all tasks from database
        int userID = Session.currentUserId;
        String sql = "SELECT * FROM tasks WHERE user_id = ?";
        List<Task> allTasks = new ArrayList<>();

        try (Connection conn = DatabaseHelper.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("task_id");
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

                // Determine if this is a PINNED task (has start_time in database)
                // A task is pinned if it has a start_time AND start_date matches due_date
                // AND the time is NOT 00:00
                LocalDateTime taskStart = start;
                LocalDate taskStartDate = startDate;
                
                if (start != null && due != null) {
                    if (start.toLocalDate().equals(due.toLocalDate())) {
                        // Check if the time is not 00:00 (floating with start date)
                        if (start.toLocalTime().equals(START_OF_DAY)) {
                            // This is a floating task with start date (00:00 time)
                            // Keep the startDate but clear startTime
                            taskStart = null;
                            taskStartDate = start.toLocalDate();
                        }
                        // If time is not 00:00, it's a pinned task - keep startTime
                    } else {
                        // Start date and due date don't match - floating task
                        // Use startDate if available, otherwise use the date from startTime
                        taskStart = null;
                        if (startDate == null) {
                            taskStartDate = start.toLocalDate();
                        }
                    }
                } else if (start != null) {
                    // Has start but no due date - floating task
                    taskStart = null;
                    if (startDate == null) {
                        taskStartDate = start.toLocalDate();
                    }
                }

                // Create task with the correct startTime and startDate from database
                Task task = new Task(id, name, cat, imp, dur, due, taskStart, taskStartDate);
                allTasks.add(task);
            }
        } catch (Exception e) {
            System.out.println("Error fetching tasks: " + e.getMessage());
            return;
        }

        // 4. Determine focus mode from the selected RadioButton
        int currentMode = Questionnaire.PersonalMode;
        if (rdbtnWork.isSelected()) {
            currentMode = Questionnaire.WorkMode;
        } else if (rdbtnHealth.isSelected()) {
            currentMode = Questionnaire.HealthMode;
        } else if (rdbtnPersonal.isSelected()) {
            currentMode = Questionnaire.PersonalMode;
        }

        // 5. Create Questionnaire with the selected mode
        Questionnaire prefs = new Questionnaire(currentMode, Questionnaire.MorningPeriod, true, 8);
        
        // 6. Instantiate the scheduler
        Scheduler scheduler = new Scheduler();
        
        // 7. Generate the schedule – assigns startTime to floating tasks IN MEMORY ONLY
        // The scheduler will assign startTime to tasks that don't have one
        // This creates a COPY of the task list with scheduled times
        List<Task> scheduledTasks = scheduler.genSchedule(allTasks, prefs);
        
        // 8. IMPORTANT: We need to keep track of which tasks got assigned a time
        // We'll create a map to remember the scheduled start times
        java.util.Map<Integer, LocalDateTime> scheduledTimes = new java.util.HashMap<>();
        for (Task task : scheduledTasks) {
            // If the task originally had no startTime in the database (was floating)
            // AND the scheduler assigned one, store it for display
            if (task.startTime != null) {
                // Check if this task was actually floating (no startTime in database)
                // We need to check the original state from the database
                // We'll use a flag to track this
                boolean wasFloating = false;
                
                // Check if the task's original state was floating
                // We stored the original startTime in the task object
                // If the task is pinned in the database, we should keep its original time
                // If it was floating, the scheduler assigned a new time
                for (Task originalTask : allTasks) {
                    if (originalTask.taskId == task.taskId) {
                        // If the original task had no startTime in the database (was floating)
                        // AND it has a startDate (or not), then this is a scheduled floating task
                        if (originalTask.startTime == null) {
                            wasFloating = true;
                        }
                        break;
                    }
                }
                
                // Store the scheduled time if it was a floating task that got scheduled
                if (wasFloating) {
                    scheduledTimes.put(task.taskId, task.startTime);
                }
            }
        }
        
        // 9. For the display, we want to show the scheduled times
        // But we DON'T want to modify the original tasks' startTime (they should remain floating)
        // We'll create a separate list for display purposes
        List<Task> tasksForDisplay = new ArrayList<>();
        for (Task t : allTasks) {
            // Create a copy of the task for display
            Task displayTask = new Task(
                t.taskId, 
                t.taskName, 
                t.category, 
                t.importance, 
                t.duration, 
                t.dueDate, 
                t.startTime,  // Keep the original startTime (null for floating)
                t.startDate
            );
            
            // If this task got scheduled by the algorithm, use the scheduled time for display
            if (scheduledTimes.containsKey(t.taskId)) {
                displayTask.startTime = scheduledTimes.get(t.taskId);
            }
            
            tasksForDisplay.add(displayTask);
        }

        // 10. Display tasks for the selected date
        LocalDate viewDate = dpViewDate.getValue();
        if (viewDate == null) viewDate = LocalDate.now();

        List<Task> tasksForToday = new ArrayList<>();
        for (Task t : tasksForDisplay) {
            boolean shouldDisplay = false;
            
            // Pinned task: has start time and matches view date
            if (t.startTime != null && t.startTime.toLocalDate().equals(viewDate)) {
                shouldDisplay = true;
            }
            // Floating task with start date
            else if (t.startDate != null && t.startDate.equals(viewDate)) {
                shouldDisplay = true;
            }
            // Floating task with due date
            else if (t.dueDate != null && t.dueDate.toLocalDate().equals(viewDate)) {
                shouldDisplay = true;
            }
            // Floating task with start date and due date: show if view date is between them
            else if (t.startDate != null && t.dueDate != null) {
                LocalDate startDate = t.startDate;
                LocalDate dueDate = t.dueDate.toLocalDate();
                if ((viewDate.isEqual(startDate) || viewDate.isAfter(startDate)) && 
                    (viewDate.isEqual(dueDate) || viewDate.isBefore(dueDate))) {
                    shouldDisplay = true;
                }
            }
            
            if (shouldDisplay) {
                tasksForToday.add(t);
            }
        }

        tasksForToday.sort(Comparator.comparing(t -> {
            if (t.startTime != null) {
                return t.startTime;
            } else if (t.startDate != null) {
                return t.startDate.atStartOfDay();
            } else if (t.dueDate != null) {
                return t.dueDate;
            }
            return LocalDateTime.now();
        }));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd");

        // 11. Build UI task cards
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
            
            // Determine if this is a pinned task
            boolean isPinned = t.isPinned();
            
            if (isPinned) {
                // Pinned task with specific time
                LocalDateTime endTime = t.startTime.plusMinutes((long)(t.duration * 60));
                timeString = t.startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter);
                taskType = "📌 Pinned";
            } else if (t.startTime != null) {
                // Floating task that got scheduled by the algorithm
                LocalDateTime endTime = t.startTime.plusMinutes((long)(t.duration * 60));
                timeString = t.startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter);
                taskType = "📋 Floating (Scheduled)";
            } else if (t.startDate != null && t.dueDate != null) {
                // Floating task with start date and due date
                timeString = "Starts: " + t.startDate.format(dateFormatter) + " | Due: " + t.dueDate.format(dateFormatter);
                taskType = "📋 Floating";
            } else if (t.startDate != null) {
                // Floating task with start date only
                timeString = "Starts: " + t.startDate.format(dateFormatter);
                taskType = "📋 Floating";
            } else if (t.dueDate != null) {
                // Floating task with due date only
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
    // endregion
}