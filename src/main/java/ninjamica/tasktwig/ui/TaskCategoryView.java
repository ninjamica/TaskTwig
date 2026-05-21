package ninjamica.tasktwig.ui;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import ninjamica.tasktwig.core.Task;
import ninjamica.tasktwig.core.TaskCategory;
import ninjamica.tasktwig.core.TaskInterface;
import ninjamica.tasktwig.ui.util.DraggableListBox;
import ninjamica.tasktwig.ui.util.TaskCategoryViewBase;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TaskCategoryView extends TaskCategoryViewBase {

    private final Button newTaskButton = new Button(null, new FontIcon(FontAwesomeSolid.PLUS));
    private final Consumer<TaskCategory> onNewTask;

    public TaskCategoryView(Consumer<TaskCategory> onNewTask, Consumer<Task> onNewSubTask, BiConsumer<MouseEvent, TaskInterface> taskClickHandler) {
        super(() -> new DraggableListBox<>(
                task -> {
                    TaskBox taskBox = new TaskBox(task, onNewSubTask, taskClickHandler);
                    taskBox.setOnMouseClicked(event -> taskClickHandler.accept(event, task));
                    return taskBox;
                },
                TaskBox::unbind,
                Pos.TOP_LEFT
        ));
        this.onNewTask = onNewTask;
        newTaskButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT);
        nameBox.getChildren().add(newTaskButton);

        VBox.setMargin(taskList.getNode(), new Insets(0, 0, 0, 20));
    }

    public TaskCategoryView(TaskCategory category, Consumer<TaskCategory> onNewTask, Consumer<Task> onNewSubTask, BiConsumer<MouseEvent, TaskInterface> taskClickHandler) {
        this(onNewTask, onNewSubTask, taskClickHandler);
        setCategory(category, null);
    }

    public void setCategory(TaskCategory category, Predicate<Task> filter) {
        super.setCategory(category, filter);
        newTaskButton.setOnAction(event -> onNewTask.accept(category));
    }

    protected void updateTaskList() {
        getChildren().remove(newTaskButton);
        super.updateTaskList();

        if (expanded.get() && newTaskButton != null) {
            getChildren().add(newTaskButton);
        }
    }
}
