package frontend;
import db.DatabaseHelper;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import javafx.scene.Node;
import javafx.application.Platform;

public class LoginController {

    @FXML private TextField txtUsernameLogin;
    @FXML private PasswordField txtPasswordLogin;
    @FXML private TextField txtPasswordVisibleLogin; 
    @FXML private CheckBox chbShowPasswordL;

    public void initialize() {
        txtPasswordLogin.setText("");
        txtUsernameLogin.setText("");

        if (txtPasswordVisibleLogin != null) {
            txtPasswordVisibleLogin.textProperty().bindBidirectional(txtPasswordLogin.textProperty()); // Bind the visible password. so that when the user types in the password field, it updates the visible password field and vice versa.
        }

        Platform.runLater(() -> {
            Stage stage = (Stage) txtUsernameLogin.getScene().getWindow();
            stage.setWidth(620);
            stage.setHeight(430);
            stage.centerOnScreen();
        });
    }

    @FXML
    public void onCheckchbShowPasswordL(ActionEvent event) {
        if (chbShowPasswordL.isSelected()) {
            txtPasswordVisibleLogin.setVisible(true);
            txtPasswordLogin.setVisible(false);
        } else {
            txtPasswordVisibleLogin.setVisible(false);
            txtPasswordLogin.setVisible(true);
        }
    }

    @FXML
    public void onClickbtnSignIn(ActionEvent event) {
        String username = txtUsernameLogin.getText();
        String password = txtPasswordLogin.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText(null);
            alert.setContentText("Username and Password cannot be empty.");
            alert.showAndWait();
            return;
        }

        if (DatabaseHelper.validateUser(username, password)) {
            Session.currentUserId = DatabaseHelper.getUserId(username);
            Session.currentUsername = username;

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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Login Failed");
            alert.setHeaderText(null); 
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