/*
 * This is the controller class for the menu window.
 * It handles user interactions and events from the UI components defined in the FXML files.
 * The methods in this class are linked to the UI elements via the onAction attribute in the FXML file.
 * There are other controller classes for other windows, such as LoginController and QuestionsController, 
 * which handle their own UI interactions. This was done for better organization and separation of concerns, 
 * as each window has its own specific functionality and logic.
 */

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
import javafx.stage.Stage;
import javafx.scene.control.CheckBox;
import javafx.scene.Node;

public class Controller {
    
    // This method name must match the onAction="#onClickbtnExitMain" in your FXML
    @FXML
    public void onClickbtnExitMain(ActionEvent event) {
        System.exit(0);
    }


    @FXML
    public void onClickbtnSignUpMain(ActionEvent event) {
       try {
           Parent root = FXMLLoader.load(getClass().getResource("signup.fxml"));
           Scene newScene = new Scene(root);
        
           Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
           window.setScene(newScene);
           window.show();
        } catch (Exception e) {
            System.out.println("Error loading screen: " + e.getMessage());
            e.printStackTrace();
    }

    }

    public void onClickbtnLogin(ActionEvent event) {
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
}