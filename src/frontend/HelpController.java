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
        // Use Platform.runLater to ensure the stage is fully available
        Platform.runLater(() -> {
            if (btnBack != null && btnBack.getScene() != null) {
                Stage stage = (Stage) btnBack.getScene().getWindow();
                stage.setWidth(665);
                stage.setHeight(535);
                // Optional: center the window on screen
                stage.centerOnScreen();
            }
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