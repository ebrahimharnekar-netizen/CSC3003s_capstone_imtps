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

public class SignupController {

    public static String username;
    public static String password;
    public static String email;

    @FXML
    public void onCheckchbShowPassword(ActionEvent event) {
        

    }

    public void onClickbtnCreateAccount(ActionEvent event) {
        // 1. Safely get the Scene from the button (Node) that was clicked
        Scene scene = ((Node) event.getSource()).getScene();

        // 2. Search the scene, and cast the RESULT to a TextField/PasswordField
        username = ((TextField) scene.lookup("#txtUsernameSignup")).getText();
        password = ((PasswordField) scene.lookup("#txtPasswordSignup")).getText();
        email = ((TextField) scene.lookup("#txtEmailSignup")).getText();

        // 3. Check if the user already exists in the database
        if (DatabaseHelper.userExists(username)) {
            // 4. If the user exists, display an error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Signup Failed");
            alert.setHeaderText(null); // Set to null to remove the header space
            alert.setContentText("A user with that username already exists!");
            alert.showAndWait();
        } else {
            // 5. If the user does not exist, create a new user in the database
            DatabaseHelper.addUser(username, email, password);

            // 6. Load the login page
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
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
