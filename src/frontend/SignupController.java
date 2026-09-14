package frontend;
import db.DatabaseHelper;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

public class SignupController {

    @FXML private TextField txtUsernameSignup;
    @FXML private PasswordField txtPasswordSignup;
    @FXML private TextField txtPasswordVisibleSignup; 
    @FXML private TextField txtEmailSignup;
    @FXML private CheckBox chbShowPassword;

    public static String username;
    public static String password;
    public static String email;

    public void initialize() {
        txtPasswordSignup.setText("");
        txtUsernameSignup.setText("");
        txtEmailSignup.setText("");

        if (txtPasswordVisibleSignup != null) {
            txtPasswordVisibleSignup.textProperty().bindBidirectional(txtPasswordSignup.textProperty());
        }

    Platform.runLater(() -> {
        Stage stage = (Stage) txtUsernameSignup.getScene().getWindow();
        
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
    public void onCheckchbShowPassword(ActionEvent event) {
        if (chbShowPassword.isSelected()) {
            txtPasswordVisibleSignup.setVisible(true);
            txtPasswordSignup.setVisible(false);
        } else {
            txtPasswordVisibleSignup.setVisible(false);
            txtPasswordSignup.setVisible(true);
        }
    }

    @FXML
    public void onClickbtnCreateAccount(ActionEvent event) {
        username = txtUsernameSignup.getText();
        password = txtPasswordSignup.getText();
        email = txtEmailSignup.getText();

        if (username == null || username.trim().isEmpty() || 
            password == null || password.isEmpty() || 
            email == null || email.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "All fields are required!");
            return;
        }

        // DATA VALIDATION
        boolean isValidEmail = false;
        int atCount = 0;
        int dotCountAfterAt = 0;
        int atPosition = -1;

        for (int i = 0; i < email.length(); i++) {
            char c = email.charAt(i);
            if (c == '@') {
                atCount++;
                atPosition = i;
            } else if (c == '.' && atCount == 1 && i > atPosition + 1) {
                dotCountAfterAt++;
            }
        }

        if (atCount == 1 && dotCountAfterAt >= 1 && atPosition > 0 && email.charAt(email.length() - 1) != '.') {
            isValidEmail = true;
        }

        if (!isValidEmail) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid email address.");
            return;
        }

        if (password.length() < 8) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Password must be at least 8 characters long.");
            return;
        }

        boolean hasNumber = false;
        for (int i = 0; i < password.length(); i++) {
            if (Character.isDigit(password.charAt(i))) {
                hasNumber = true;
                break;
            }
        }
        
        if (!hasNumber) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Password must contain at least one number.");
            return;
        }

        if (DatabaseHelper.userExists(username)) {
            showAlert(Alert.AlertType.ERROR, "Signup Failed", "A user with that username already exists!");
        } else {
            String newUserID = DatabaseHelper.addUser(username, email, password); // Modified to String
            Session.currentUserId = newUserID;
            Session.currentUsername = username;
            
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void onClickbtnBack(ActionEvent event) {
        System.exit(0);
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