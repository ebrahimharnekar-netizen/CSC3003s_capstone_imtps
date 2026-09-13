/* This class serves as the main entry point for the JavaFX application. It extends the Application class, 
which is a requirement for any JavaFX application. The start method is overridden to set up the primary stage (window)
 of the application, load the initial FXML layout, and display it to the user. The main method is also provided to launch the 
 application. This class is launched through the Launcher class, which provides a simple way to start the application without needing to 
 set up the JavaFX environment manually.
 */


package frontend;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        //Load the FXML file
        Parent root = FXMLLoader.load(getClass().getResource("/frontend/login.fxml")); //Login is the first page 
                                                                                    // that will be displayed when the application starts
        
        //Attach it to a Scene
        Scene scene = new Scene(root); 
        //maximize the window
        primaryStage.setMaximized(true);
        
        //Set up the window (Stage) and show it
        primaryStage.setTitle("IMTPS Application"); // The text at the top of the window
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // This launches the JavaFX application
        launch(args);
    }
}
