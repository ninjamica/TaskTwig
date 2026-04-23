package ninjamica.tasktwig.ui;

import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class TaskTwigApplication extends Application {
    private TaskTwigController controller;
    @Override
    public void start(Stage stage) throws IOException {

        // FXMLLoader fxmlLoader = new FXMLLoader(TaskTwigApplication.class.getResource("fxml/main-view.fxml"));
        // Scene scene = new Scene(fxmlLoader.load());
        controller = new TaskTwigController(this);
        Scene scene = new Scene(controller.getRoot());
        controller.setStage(stage);
        
//        setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        stage.getIcons().addAll(
          new Image(TaskTwigApplication.class.getResourceAsStream("images/icon-64.png")),
          new Image(TaskTwigApplication.class.getResourceAsStream("images/icon-32.png")),
          new Image(TaskTwigApplication.class.getResourceAsStream("images/icon-16.png"))
        );
        stage.setTitle("TaskTwig");
        stage.setScene(scene);
        stage.setOnCloseRequest(controller::closeTwig);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
//        controller.closeTwig();
        super.stop();
    }

    void setTheme(Theme theme) {
        setUserAgentStylesheet(theme.getUserAgentStylesheet());
    }
}
