package frontend;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelpController {

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