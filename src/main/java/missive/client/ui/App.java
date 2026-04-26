package missive.client.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("Missive");
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        try {
            var icon = getClass().getResourceAsStream("/images/icon.png");
            if (icon != null) stage.getIcons().add(new Image(icon));
        } catch (Exception ignored) {}

        showLogin();
        stage.show();
    }

    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 460, 600);
        scene.getStylesheets().add(App.class.getResource("/css/terminal.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
    }

    public static void showChat(missive.client.ChatClient client) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/chat.fxml"));
        Parent root = loader.load();
        ChatController controller = loader.getController();
        controller.init(client);

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(App.class.getResource("/css/terminal.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
    }

    public static Stage getStage() { return primaryStage; }

    public static void main(String[] args) {
        launch(args);
    }
}
