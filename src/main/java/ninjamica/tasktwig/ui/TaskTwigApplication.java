package ninjamica.tasktwig.ui;

import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

public class TaskTwigApplication extends Application {
    @Override
    public void start(Stage stage) {

        TaskTwigController controller = new TaskTwigController(this);
        Scene scene = new Scene(controller.getRoot());
        controller.setStage(stage);

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

    void setTheme(@NotNull Theme theme) {
        setUserAgentStylesheet(theme.getUserAgentStylesheet());
    }
}
