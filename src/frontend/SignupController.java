package frontend;
import db.DatabaseHelper;
import algorithm.*;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import javafx.scene.Node;
import javafx.application.Platform;

public class SignupController {

    @FXML private TextField txtUsernameSignup;
    @FXML private PasswordField txtPasswordSignup;
    @FXML private TextField txtEmailSignup;

    public static String username;
    public static String password;
    public static String email;

    public void initialize() {
        // This method is called after the FXML file has been loaded

        txtPasswordSignup.setText(null);
        txtUsernameSignup.setText(null);
        txtEmailSignup.setText(null);

        Platform.runLater(() -> {
            Stage stage = (Stage) txtUsernameSignup.getScene().getWindow();
            stage.setWidth(600);
            stage.setHeight(435);
            // Optional: center the window on screen
            stage.centerOnScreen();
        });
    }

    @FXML
    public void onCheckchbShowPassword(ActionEvent event) {
        

    }

    public void onClickbtnCreateAccount(ActionEvent event) {

        username = txtUsernameSignup.getText();
        password = txtPasswordSignup.getText();
        email = txtEmailSignup.getText();

        //Check if the user already exists in the database
        if (DatabaseHelper.userExists(username)) {
            // 4. If the user exists, display an error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Signup Failed");
            alert.setHeaderText(null); // Set to null to remove the header space
            alert.setContentText("A user with that username already exists!");
            alert.showAndWait();
        } else {
            //If the user does not exist, create a new user in the database
            int newUserID = DatabaseHelper.addUser(username, email, password);
            // Track who just signed up so the questionnaire knows whose preferences to save
            Session.currentUserId = newUserID;
            Session.currentUsername = username;
            //Load the questionnaire page with the user new logged in
            try {
                 FXMLLoader loader = new FXMLLoader(getClass().getResource("Questions.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    public void onClickbtnBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClickbtnLogInSignup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
