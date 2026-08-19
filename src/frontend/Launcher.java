/*
 * This is the launcher class for the JavaFX application.
 * It serves as the entry point of the application and calls the main method in the Main class. We cant run the Main class 
 * directly because it extends Application, which requires a JavaFX runtime environment to be initialized. 
 * The Launcher class provides a simple way to start the application without needing to set up the JavaFX environment manually.
 */
package frontend;

public class Launcher {
    public static void main(String[] args) {
        // This calls the main method in existing Application class
        Main.main(args); 
    }
}