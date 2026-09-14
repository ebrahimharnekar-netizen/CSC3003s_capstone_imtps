package frontend;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.application.Platform;

public class HelpController {

    @FXML
    private Button btnBack;  // injected from the FXML

    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) btnBack.getScene().getWindow();
            
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

    public void onClickbtnBack(ActionEvent event) {
        try {
            // Navigate back to the main IMTPS screen (imtps.fxml)
            Parent root = FXMLLoader.load(getClass().getResource("imtps.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.out.println("Error returning to main screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}