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

public class IMTPSController {

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

    @FXML
    public void initialize() {
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
    }
    
    // --- ADD TASK LOGIC WITH VALIDATION ---
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
        
        if (startVal != null && txtStartTime.getText() != null && !txtStartTime.getText().isEmpty()) {
            try {
                LocalTime parsedTime = LocalTime.parse(txtStartTime.getText()); 
                startTime = startVal.atTime(parsedTime); 
            } catch (Exception e) {
                lblAddStatus.setText("Error: Invalid time format. Use HH:mm (e.g., 14:30)");
                lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                return;
            }
        }

        if (startTime != null) {
            if (checkTimeOverlap(Session.currentUserId, startTime, duration)) {
                lblAddStatus.setText("Error: You already have a task scheduled during this time!");
                lblAddStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                return;
            }
        }

        LocalDate dueVal = btnDueDate.getValue();
        LocalDateTime dueDate = (dueVal != null) ? dueVal.atTime(23, 59) : null;

        int userID = Session.currentUserId; 
        DatabaseHelper.addTask(userID, addName, categoryInt, importance, duration, dueDate, startTime);
        
        lblAddStatus.setText("Success: '" + addName + "' added successfully!");
        lblAddStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        
        btnAddName.clear();
        txtStartTime.clear();
    }

    private boolean checkTimeOverlap(int userId, LocalDateTime newStart, double newDuration) {
        LocalDateTime newEnd = newStart.plusMinutes((long)(newDuration * 60));
        String sql = "SELECT start_time, duration FROM tasks WHERE user_id = ?";
        
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Object startObj = rs.getObject("start_time");
                if (startObj != null) {
                    LocalDateTime existingStart = (startObj instanceof LocalDateTime) 
                        ? (LocalDateTime) startObj 
                        : LocalDateTime.parse(startObj.toString().replace(" ", "T"));
                    
                    double existingDur = rs.getDouble("duration");
                    LocalDateTime existingEnd = existingStart.plusMinutes((long)(existingDur * 60));
                    
                    if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                        return true; 
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Overlap check error: " + e.getMessage());
        }
        return false;
    }

    // --- EDIT TASK LOGIC (SEARCH & INLINE EDITING) ---
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
                    editStartDate.setPrefWidth(120);
                    
                    TextField editStartTime = new TextField();
                    if (start != null) editStartTime.setText(start.toLocalTime().toString());
                    editStartTime.setPromptText("HH:mm");
                    editStartTime.setPrefWidth(80);
                    row3.getChildren().addAll(new Label("Start Date:"), editStartDate, new Label("Time:"), editStartTime);

                    HBox row4 = new HBox(10);
                    DatePicker editDueDate = new DatePicker();
                    if (due != null) editDueDate.setValue(due.toLocalDate());
                    editDueDate.setPrefWidth(120);
                    row4.getChildren().addAll(new Label("Due Date:"), editDueDate);

                    inputsBox.getChildren().addAll(row1, row2, row3, row4);

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
                            if (newStartVal != null && !editStartTime.getText().trim().isEmpty()) {
                                LocalTime pt = LocalTime.parse(editStartTime.getText().trim());
                                newStart = newStartVal.atTime(pt);
                            } else if (newStartVal != null) {
                                newStart = newStartVal.atTime(8, 0); 
                            }

                            LocalDate newDueVal = editDueDate.getValue();
                            LocalDateTime newDue = (newDueVal != null) ? newDueVal.atTime(23, 59) : null;

                            DatabaseHelper.updateTaskName(id, newName);
                            DatabaseHelper.updateTaskCategory(id, newCat);
                            DatabaseHelper.updateTaskImportance(id, newImp);
                            DatabaseHelper.updateTaskDuration(id, newDur);
                            
                            // USING YOUR NEW LOCALDATETIME DB METHODS
                            DatabaseHelper.updateTaskStartTime(id, newStart);
                            DatabaseHelper.updateTaskDueDate(id, newDue);
                            
                            lblEditStatus.setText("Task Updated Successfully!");
                            lblEditStatus.setStyle("-fx-text-fill: green;");
                        } catch (Exception ex) {
                            lblEditStatus.setText("Error: Invalid time format (Use HH:mm).");
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

    // --- NAVIGATION & VIEW TASKS ---
    public void onClickbtnPref(ActionEvent event) { switchScene(event, "preferences.fxml"); }
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

    @FXML
    public void onClickRefreshTasks(ActionEvent event) {
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

        taskContainer.getChildren().clear();

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

                Task task = new Task(id, name, cat, imp, dur, due, start);
                allTasks.add(task);
            }
        } catch (Exception e) {
            System.out.println("Error fetching tasks: " + e.getMessage());
            return;
        }

        int currentMode = Questionnaire.PersonalMode; 
        if (rdbtnWork.isSelected()) currentMode = Questionnaire.WorkMode;
        if (rdbtnHealth.isSelected()) currentMode = Questionnaire.HealthMode;

        Questionnaire prefs = new Questionnaire(currentMode, Questionnaire.MorningPeriod, true, 8); 
        Scheduler scheduler = new Scheduler();
        scheduler.genSchedule(allTasks, prefs);

        for (Task t : allTasks) {
            if (t.startTime != null) {
                // USING YOUR NEW LOCALDATETIME DB METHOD
                DatabaseHelper.updateTaskStartTime(t.taskId, t.startTime); 
            }
        }

        LocalDate viewDate = dpViewDate.getValue();
        if (viewDate == null) viewDate = LocalDate.now();

        List<Task> tasksForToday = new ArrayList<>();
        for (Task t : allTasks) {
            if (t.startTime != null && t.startTime.toLocalDate().equals(viewDate)) {
                tasksForToday.add(t);
            }
        }

        tasksForToday.sort(Comparator.comparing(t -> t.startTime));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

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

            LocalDateTime endTime = t.startTime.plusMinutes((long)(t.duration * 60));
            String timeString = t.startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter);

            VBox textBox = new VBox(5); 
            Label nameLabel = new Label(t.taskName);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

            String detailsText = String.format("%s  |  %s  |  Importance: %d", catString, timeString, t.importance);
            Label detailsLabel = new Label(detailsText);
            detailsLabel.setStyle("-fx-text-fill: dimgray; -fx-font-size: 12px;");

            textBox.getChildren().addAll(nameLabel, detailsLabel);
            taskCard.getChildren().addAll(taskCheckbox, textBox);
            
            taskContainer.getChildren().add(taskCard);
        }
    }
}