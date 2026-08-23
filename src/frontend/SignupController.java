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

    @FXML private TextField txtUsernameSignup;
    @FXML private PasswordField txtPasswordSignup;
    @FXML private TextField txtEmailSignup;

    public static String username;
    public static String password;
    public static String email;

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
            DatabaseHelper.addUser(username, email, password);

            //Load the login page
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
