package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * JavaFX Client Application Entry Point.
 * Handles UI initialization, network connectivity setup, and EventBus messaging.
 */
public class App extends Application {

    private static Scene scene;
    private SimpleClient client;

    @Override
    public void start(Stage stage) throws IOException {
        // Register the application class to listen for EventBus events
        EventBus.getDefault().register(this);

        // Retrieve command-line parameters to support running on different machines via IP
        List<String> arguments = getParameters().getRaw();

        // Default to "localhost" for single-machine execution, or use provided IP argument
        String serverHost = arguments.isEmpty() ? "localhost" : arguments.get(0);

        System.out.println("Connecting to server at host: " + serverHost + " on port 3000...");

        // Initialize client connection with the target host
        client = SimpleClient.getClient(serverHost);
        client.openConnection();

        // Initialize JavaFX Primary Stage
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setTitle("Tic-Tac-Toe Game");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Replaces the current scene root with a new FXML view.
     */
    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    /**
     * Loads an FXML file resource.
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    /**
     * Clean shutdown handler triggered when the JavaFX Application window is closed.
     */
    @Override
    public void stop() throws Exception {
        EventBus.getDefault().unregister(this);
        if (client != null && client.isConnected()) {
            client.sendToServer("remove client");
            client.closeConnection();
        }
        super.stop();
    }

    /**
     * EventBus subscriber method to handle system warnings safely on the JavaFX Application Thread.
     */
    @Subscribe
    public void onWarningEvent(WarningEvent event) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.WARNING,
                    String.format("Message: %s\nTimestamp: %s\n",
                            event.getWarning().getMessage(),
                            event.getWarning().getTime().toString())
            );
            alert.show();
        });
    }

    public static void main(String[] args) {
        // Pass CLI arguments (e.g., target server IP) to the JavaFX Application lifecycle
        launch(args);
    }
}