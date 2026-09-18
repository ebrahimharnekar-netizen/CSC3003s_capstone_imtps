package frontend;
import db.DatabaseHelper;
import algorithm.*;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.application.Platform;

/**
 * QuestionsController
 * Backs the onboarding questionnaire. Answers are saved to
 * the preferences table for the currently logged-in user
 * and used later by the scheduling algorithm. Any question left unanswered
 * falls back to a default. 
 * Because this screen doubles as "change your answers at any point", it also
 * pre-fills existing saved preferences on load, if there are any.
 */
public class QuestionsController {

    // Defaults used when a question is left unanswered
    private static final int DEFAULT_DAILY_PROD_HOURS = 4;
    private static final boolean DEFAULT_QUICK_TASKS_FIRST = true;
    private static final int DEFAULT_PEAK_PROD_TIME = Questionnaire.MorningPeriod;

    @FXML private ComboBox<Integer> cmbProductiveHours;
    @FXML private RadioButton rdbtnQuickYes;
    @FXML private RadioButton rdbtnQuickNo;
    @FXML private RadioButton rdbtnMorning;
    @FXML private RadioButton rdbtnMidday;
    @FXML private RadioButton rdbtnNight;
    @FXML private TextField txtPrefStartTime;
    @FXML private TextField txtPrefEndTime;
    @FXML private Button btnSaveQuestionnaire;
    @FXML private Button btnBack;

    @FXML
    public void initialize() {

        for (int hours = 1; hours <= 10; hours++) {
            cmbProductiveHours.getItems().add(hours);
        }

        ToggleGroup quickGroup = new ToggleGroup();
        rdbtnQuickYes.setToggleGroup(quickGroup);
        rdbtnQuickNo.setToggleGroup(quickGroup);

        ToggleGroup timeGroup = new ToggleGroup();
        rdbtnMorning.setToggleGroup(timeGroup);
        rdbtnMidday.setToggleGroup(timeGroup);
        rdbtnNight.setToggleGroup(timeGroup);

        // Pre-fill with the user's existing answers, if they've done this before
        DatabaseHelper.Preferences existing = DatabaseHelper.getPreferences(Session.currentUserId);

        if (existing != null) {

            cmbProductiveHours.setValue(existing.dailyProdHours);

            if (existing.preferQuickFirst) {
                rdbtnQuickYes.setSelected(true);
            } else {
                rdbtnQuickNo.setSelected(true);
            }

            if (existing.peakProdTime == Questionnaire.MorningPeriod) {
                rdbtnMorning.setSelected(true);
            } else if (existing.peakProdTime == Questionnaire.AfternoonPeriod) {
                rdbtnMidday.setSelected(true);
            } else if (existing.peakProdTime == Questionnaire.EveningPeriod) {
                rdbtnNight.setSelected(true);
            }
            
            if (existing.startTime != null) {
                txtPrefStartTime.setText(existing.startTime);
            }
            if (existing.endTime != null) {
                txtPrefEndTime.setText(existing.endTime);
            }

        }

        Platform.runLater(() -> {
            Stage stage = (Stage) btnSaveQuestionnaire.getScene().getWindow();
            
            // Fetch the dimensions of the user's actual monitor
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            
            // Bind the window to the monitor's edges
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            
            // Force the OS to maximize the window
            stage.setMaximized(true);
        });

    }

    @FXML
    public void onClickbtnBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("imtps.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.out.println("Error returning to main screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onClickbtnSaveQuestionnaire(ActionEvent event) {

        int dailyProdHours = (cmbProductiveHours.getValue() != null)
                ? cmbProductiveHours.getValue()
                : DEFAULT_DAILY_PROD_HOURS;

        boolean preferQuickFirst;
        if (rdbtnQuickYes.isSelected()) {
            preferQuickFirst = true;
        } else if (rdbtnQuickNo.isSelected()) {
            preferQuickFirst = false;
        } else {
            preferQuickFirst = DEFAULT_QUICK_TASKS_FIRST;
        }

        int peakProdTime;
        if (rdbtnMorning.isSelected()) {
            peakProdTime = Questionnaire.MorningPeriod;
        } else if (rdbtnMidday.isSelected()) {
            peakProdTime = Questionnaire.AfternoonPeriod;
        } else if (rdbtnNight.isSelected()) {
            peakProdTime = Questionnaire.EveningPeriod;
        } else {
            peakProdTime = DEFAULT_PEAK_PROD_TIME;
        }

        String startTime = txtPrefStartTime.getText();
        if (startTime == null || startTime.trim().isEmpty()) {
            startTime = "07:00"; // default
        }

        String endTime = txtPrefEndTime.getText();
        if (endTime == null || endTime.trim().isEmpty()) {
            endTime = "22:00"; // default
        }

        DatabaseHelper.setPreferences(Session.currentUserId, peakProdTime, dailyProdHours, preferQuickFirst, startTime, endTime);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("imtps.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}