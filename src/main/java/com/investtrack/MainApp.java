package com.investtrack;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * The main entry point for the InvestTrack JavaFX application.
 * This class extends {@link Application} and is responsible for loading the
 * main user interface (defined in FXML) and displaying the primary stage (window).
 */
public class MainApp extends Application {

    /**
     * The main entry point for all JavaFX applications.
     * This method is called after the FX runtime environment is initialized.
     * It loads the main FXML view, creates a scene, sets the stage title,
     * and shows the application window.
     *
     * @param primaryStage The primary stage for this application, onto which
     *                     the application scene can be set.
     * @throws IOException If the FXML file cannot be loaded.
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Construct the path to the FXML file relative to the classpath root
        String fxmlPath = "/com/investtrack/view/MainView.fxml";
        URL fxmlUrl = getClass().getResource(fxmlPath);

        // Defensive check: Ensure the FXML file was found
        if (fxmlUrl == null) {
            System.err.println("Error: Cannot find FXML file at path: " + fxmlPath);
            // Consider throwing a more specific exception or showing an error dialog
            throw new IOException("FXML resource not found: " + fxmlPath);
        }

        // Create a new FXMLLoader and set the controller manually
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setController(new com.investtrack.view.MainController());
        
        // Load the FXML file to create the root UI node
        Parent root = loader.load();

        // Create the scene with the loaded UI root
        Scene scene = new Scene(root); // Let FXML define preferred size, or set explicitly: new Scene(root, 800, 600);

        // Configure the primary stage (window)
        primaryStage.setTitle("Investment Portfolio Tracker");
        primaryStage.setScene(scene);

        // Set minimum dimensions if desired
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);


        // Show the stage
        primaryStage.show();
    }

    /**
     * The main method, used to launch the JavaFX application.
     * This method is not required for IDEs that launch JavaFX applications directly,
     * but it is needed for command-line execution or when creating executable JARs.
     *
     * @param args Command line arguments passed to the application. Not used in this application.
     */
    public static void main(String[] args) {
        // Launch the JavaFX application lifecycle
        launch(args);
    }
}
