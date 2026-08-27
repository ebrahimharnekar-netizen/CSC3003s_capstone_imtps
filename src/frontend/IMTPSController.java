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
    @FXML private TextField txtStartTime; // NEW: Time input field
    @FXML private DatePicker btnDueDate;

    // View Tasks Tab Elements
    @FXML private VBox taskContainer;
    @FXML private DatePicker dpViewDate; // NEW: Controls which date we look at
    @FXML private RadioButton rdbtnWork;
    @FXML private RadioButton rdbtnHealth;
    @FXML private RadioButton rdbtnPersonal;

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
        
        // Default the view date to today
        dpViewDate.setValue(LocalDate.now()); 
    }
    
    public void onClickBtnAddTask(ActionEvent event) {
        String addName = btnAddName.getText();

        String categoryString = btnCategory.getValue(); 
        int categoryInt = 3; 

        if (categoryString != null) {
            if (categoryString.contains("Work")) categoryInt = 1;
            else if (categoryString.contains("Health")) categoryInt = 2;
            else if (categoryString.contains("Personal")) categoryInt = 3;
        }

        int importance = (btnImportance.getValue() != null) ? btnImportance.getValue() : 0;
        double duration = (btnDuration.getValue() != null) ? btnDuration.getValue() : 0.0;

        // NEW TIME LOGIC: Only pin the task if they provide BOTH a date and a valid time!
        LocalDate startVal = btnStartDate.getValue();
        LocalDateTime startTime = null; 
        
        if (startVal != null && txtStartTime.getText() != null && !txtStartTime.getText().isEmpty()) {
            try {
                LocalTime parsedTime = LocalTime.parse(txtStartTime.getText()); // Parses "14:30"
                startTime = startVal.atTime(parsedTime); 
            } catch (Exception e) {
                System.out.println("Invalid time format entered. Task will remain floating.");
            }
        }

        LocalDate dueVal = btnDueDate.getValue();
        LocalDateTime dueDate = (dueVal != null) ? dueVal.atTime(23, 59) : null;

        int userID = Session.currentUserId; 
        DatabaseHelper.addTask(userID, addName, categoryInt, importance, duration, dueDate, startTime);
        
        btnAddName.clear();
        txtStartTime.clear();
    }

    public void onClickbtnPref(ActionEvent event) {
        switchScene(event, "preferences.fxml");
    }

    public void onClickbtnSignOut(ActionEvent event) {
        switchScene(event, "login.fxml");
    }

    public void onClickbtnHelp(ActionEvent event) {
        switchScene(event, "help.fxml");
    }

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

        // 3. Fetch all tasks 
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
                
                // Assuming DatabaseHelper saves Dates/Times appropriately
                Object dueObj = rs.getObject("due_date");
                LocalDateTime due = (dueObj instanceof LocalDateTime) ? (LocalDateTime) dueObj : null;
                
                Object startObj = rs.getObject("start_time");
                LocalDateTime start = (startObj instanceof LocalDateTime) ? (LocalDateTime) startObj : null;

                Task task = new Task(id, name, cat, imp, dur, due, start);
                allTasks.add(task);
            }
        } catch (Exception e) {
            System.out.println("Error fetching tasks: " + e.getMessage());
            return;
        }

        // 4. Run your algorithm to schedule floating tasks!
        int currentMode = Questionnaire.PersonalMode; 
        if (rdbtnWork.isSelected()) currentMode = Questionnaire.WorkMode;
        if (rdbtnHealth.isSelected()) currentMode = Questionnaire.HealthMode;

        Questionnaire prefs = new Questionnaire(currentMode, Questionnaire.MorningPeriod, true, 8); 
        Scheduler scheduler = new Scheduler();
        scheduler.genSchedule(allTasks, prefs);

        // 5. UPDATE THE DATABASE WITH THE NEW TIMES!
        for (Task t : allTasks) {
            if (t.startTime != null) {
                DatabaseHelper.updateTaskStartTime(t.taskId, t.startTime); 
            }
        }

        // 6. Filter tasks to ONLY show the ones for the selected DatePicker date!
        LocalDate viewDate = dpViewDate.getValue();
        if (viewDate == null) viewDate = LocalDate.now();

        List<Task> tasksForToday = new ArrayList<>();
        for (Task t : allTasks) {
            if (t.startTime != null && t.startTime.toLocalDate().equals(viewDate)) {
                tasksForToday.add(t);
            }
        }

        tasksForToday.sort(Comparator.comparing(t -> t.startTime));

        // 7. Build UI Cards
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

            // Calculate exact End Time based on duration
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