package frontend;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Load the FXML file
        Parent root = FXMLLoader.load(getClass().getResource("menu.fxml"));
        
        // 2. Attach it to a Scene (you can also set the width/height here)
        Scene scene = new Scene(root, 600, 400); 
        
        // 3. Set up the window (Stage) and show it
        primaryStage.setTitle("My Menu App"); // The text at the top of the window
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // This launches the JavaFX application
        launch(args);
    }
}
