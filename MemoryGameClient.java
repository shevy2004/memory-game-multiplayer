
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Main class that runs the memory game client using JavaFX. */
public class MemoryGameClient extends Application {

    /** Starts the app, loads the FXML UI, sets the scene and connects to the server. */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Default values for server connection
            String host = "localhost";
            int port = 8080;

            // Get host from command-line argument if provided
            if (getParameters().getRaw().size() >= 1) {
                host = getParameters().getRaw().get(0);
            }

            // Get port from command-line argument if provided
            if (getParameters().getRaw().size() >= 2) {
                try {
                    port = Integer.parseInt(getParameters().getRaw().get(1));
                } catch (NumberFormatException e) {
                    System.err.println("פורט לא תקין, משתמש בברירת מחדל: " + port);
                }
            }

            System.out.println("מתחבר לשרת: " + host + ":" + port);

            // Load the game UI from FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MemoryGameView.fxml"));
            Parent root = loader.load();

            // Get the controller and pass it host, port and stage
            GameController controller = loader.getController();
            controller.setConnectionParameters(host, port);
            controller.setPrimaryStage(primaryStage);

            // Set up the main game window
            primaryStage.setTitle("Memory Game - " + host + ":" + port);
            primaryStage.setScene(new Scene(root));
            primaryStage.setResizable(false);
            primaryStage.show();

            // Connect to the game server
            controller.connectToServer();

        } catch (Exception e) {
            System.err.println("שגיאה בטעינת הממשק: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Main method that launches the app. */
    public static void main(String[] args) {
        System.out.println("מפעיל לקוח משחק הזיכרון...");
        launch(args);
    }
}