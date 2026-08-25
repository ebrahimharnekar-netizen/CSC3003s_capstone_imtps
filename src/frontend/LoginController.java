package frontend;
import db.DatabaseHelper;
import algorithm.*;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import javafx.scene.Node;


public class LoginController {

    @FXML private TextField txtUsernameLogin;
    @FXML private PasswordField txtPasswordLogin;
    
    @FXML private CheckBox chbShowPasswordLogin;



    public void onCheckchbShowPasswordL(ActionEvent event) {
        

    }

    public void onClickbtnSignIn(ActionEvent event) {

        String username = txtUsernameLogin.getText();
        String password = txtPasswordLogin.getText();

        // Check if the user exists in the database
        if (DatabaseHelper.validateUser(username, password)) {

            // Track who is logged in so later screens know whose data to save
            Session.currentUserId = DatabaseHelper.getUserId(username);
            Session.currentUsername = username;

            //Loads the imtps page if the user exists in the database
        try {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Login Successful");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("imtps.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // User does not exist, shows an error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Login Failed");
            alert.setHeaderText(null); // Set to null to remove the header space
            alert.setContentText("Invalid username or password. Please try again.");
            alert.showAndWait();
            
        }


    }

    public void onClickbtnLoginSignup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("signup.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
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
}