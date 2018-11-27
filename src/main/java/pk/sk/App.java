package pk.sk;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pk.sk.controller.MainController;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);

        stage.setTitle("SK Project - Group Selection");
        stage.setScene(scene);
        stage.setMinWidth(650);
        stage.setMinHeight(630);
        stage.setHeight(630);
        stage.show();
    }

    @Override
    public void stop() {
        MainController.quit();
    }
}
