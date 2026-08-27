package frontend;
import db.DatabaseHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import algorithm.*;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.SpinnerValueFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

public class IMTPSController {

    @FXML private TextField btnAddName;
    @FXML private ComboBox<String> btnCategory;
    @FXML private Spinner<Integer> btnImportance;
    @FXML private Spinner<Double> btnDuration;
    @FXML private DatePicker btnStartDate; 
    @FXML private DatePicker btnDueDate;

    @FXML
    public void initialize() {
        // 1. Give the ComboBox its dropdown options
        btnCategory.getItems().addAll("1. Work", "2. Health", "3. Personal");

        // 2. Set Spinner rules: (Minimum, Maximum, Starting Default)
        SpinnerValueFactory<Integer> importanceFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1);
        btnImportance.setValueFactory(importanceFactory);

        // 3. Set Duration rules: (Min 0.0, Max 24.0, Default 1.0, Step by 0.5)
        SpinnerValueFactory<Double> durationFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 24.0, 1.0, 0.25);
        btnDuration.setValueFactory(durationFactory);
    }
    
    public void onClickBtnAddTask(ActionEvent event) {

        String addName = btnAddName.getText();

        // 2. Get the Category String from the dropdown
        String categoryString = btnCategory.getValue(); 
        int categoryInt = 3; // Default to Personal (3) if something goes wrong

        // Translate the text into your algorithm's integers
        if (categoryString != null) {
            if (categoryString.contains("Work")) {
                categoryInt = 1;
            } else if (categoryString.contains("Health")) {
                categoryInt = 2;
            } else if (categoryString.contains("Personal")) {
                categoryInt = 3;
            }
        }

        // 5. Get the values from the Spinners
        // (We use a fallback of 0 just in case the field is left entirely blank)
        int importance = 0;
        if (btnImportance.getValue() != null) {
            importance = btnImportance.getValue();
        }

        double duration = 0.0;
        if (btnDuration.getValue() != null) {
            duration = btnDuration.getValue();
        }

        LocalDate startVal = btnStartDate.getValue();
        LocalDateTime startTime = null; // Stays null if user didn't pick a date (floating task)
        if (startVal != null) {
            // Attach 08:00 AM to the chosen day
            startTime = startVal.atTime(8, 0); 
        }

        LocalDate dueVal = btnDueDate.getValue();
        LocalDateTime dueDate = null; // Stays null if there is no deadline
        if (dueVal != null) {
            // Attach 23:59 (11:59 PM) to the chosen deadline day
            dueDate = dueVal.atTime(23, 59);
        }

        
        // At this exact spot, you have all 6 pieces of data needed to create a new Task object!
        // Task newTask = new Task(addName, categoryInt, importance, duration, dueDate, startTime);
        
        int userID = Session.currentUserId; // Get the current logged-in user's ID
        DatabaseHelper.addTask(userID, addName, categoryInt, importance, duration, dueDate, startTime);
    }

    public void onClickbtnPref(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("preferences.fxml"));
            Scene newScene = new Scene(root);
        
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(newScene);
            window.show();
        } catch (Exception e) {
            System.out.println("Error loading screen: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void onClickbtnSignOut(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
            Scene newScene = new Scene(root);
        
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(newScene);
            window.show();
        } catch (Exception e) {
            System.out.println("Error loading screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onClickbtnHelp(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("help.fxml"));
            Scene newScene = new Scene(root);
        
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(newScene);
            window.show();
        } catch (Exception e) {
            System.out.println("Error loading screen: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
