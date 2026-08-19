package frontend;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;

public class Controller {
    
    // This method name must match the onAction="#onClickbtnExitMain" in your FXML
    @FXML
    public void onClickbtnExitMain(ActionEvent event) {
        System.out.println("Exit button was clicked!");
        // You can add your exit logic here later, e.g., System.exit(0);
    }

    // This method name must match the onAction="#onClickbtnHelpMain" in your FXML
    @FXML
    public void onClickbtnHelpMain(ActionEvent event) {
        System.out.println("Help button was clicked!");
        // You can add your help logic here later
    }
}