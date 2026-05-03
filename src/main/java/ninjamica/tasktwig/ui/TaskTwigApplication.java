package ninjamica.tasktwig.ui;

import atlantafx.base.theme.Theme;
import com.gluonhq.attach.storage.StorageService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ninjamica.tasktwig.core.TaskTwig;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class TaskTwigApplication extends Application {
    @Override
    public void start(Stage stage) {

        StorageService storageService = StorageService.create().orElseThrow();
        File dataDir = storageService.getPrivateStorage().orElseThrow();
        TaskTwig taskTwig = new TaskTwig(dataDir);

        TaskTwigController controller = new TaskTwigController(this, taskTwig);
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
