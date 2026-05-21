package ninjamica.tasktwig.ui.util;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import ninjamica.tasktwig.core.TaskInterface;

public class TaskInterfaceBoxBase<T extends TaskInterface> extends VBox {

    protected final HBox nameBox = new HBox();
    protected final Text nameText = new Text();

    public TaskInterfaceBoxBase() {
        nameBox.setMinWidth(50);
        nameBox.getChildren().add(nameText);
        getChildren().add(nameBox);
    }

    public TaskInterfaceBoxBase(T task) {
        this();
        setTask(task);
    }

    public void setTask(T task) {
        unbind();

        if (task != null) {
            nameText.textProperty().bind(task.nameProperty());
        }
    }

    public void unbind() {
        nameText.textProperty().unbind();
    }
}
