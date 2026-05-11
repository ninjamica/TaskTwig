package ninjamica.tasktwig.ui.util;

import atlantafx.base.theme.Styles;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.TaskCategory;
import ninjamica.tasktwig.core.TwigTask;

public class TaskCategoryBox extends VBox {
    private final Text catNameText = new Text();
    private final VBox taskList = new VBox(10);
    private Subscription subs = Subscription.EMPTY;

    public TaskCategoryBox() {
        super(10);

        catNameText.getStyleClass().add(Styles.TITLE_3);
        taskList.setPadding(new Insets(0, 0, 0, 20));
        getChildren().addAll(catNameText, taskList);
    }

    public TaskCategoryBox(TaskCategory category) {
        this();
        setCategory(category);
    }

    public void setCategory(TaskCategory category) {
        subs.unsubscribe();

        if (category != null) {
            catNameText.textProperty().bind(category.nameProperty());
            catNameText.fillProperty().bind(category.paintProperty());

            fillTaskList(category);
            category.tasksProperty().addListener(this::taskListChange);

            subs = Subscription.combine(
                    catNameText.textProperty()::unbind,
                    catNameText.fillProperty()::unbind,
                    () -> category.tasksProperty().removeListener(this::taskListChange),
                    taskList.getChildren()::clear
            );
        }
    }

    public void unbind() {
        subs.unsubscribe();
    }

    private void fillTaskList(TaskCategory category) {
        taskList.getChildren().clear();
        for (TwigTask task : category.tasksProperty()) {
            taskList.getChildren().add(new TaskBox(task));
        }
    }

    private void taskListChange(ListChangeListener.Change<? extends TwigTask> change) {
        while (change.next()) {
            if (change.wasPermutated()) {
                for (int i = change.getFrom(); i <= change.getTo(); ++i) {
                    Node cell = taskList.getChildren().remove(i);
                    taskList.getChildren().add(change.getPermutation(i), cell);
                }
            }
            else if (change.wasReplaced()) {
                for (int i = change.getFrom(); i < change.getTo(); ++i) {
                    TaskBox subTaskBox = (TaskBox) taskList.getChildren().get(i);
                    TwigTask newTask = change.getList().get(i);
                    subTaskBox.setTask(newTask);
                }
            }
            else if (change.wasRemoved()) {
                for (int i = change.getFrom(); i < change.getTo(); ++i) {
                    TaskBox subTaskBox = (TaskBox) taskList.getChildren().remove(i);
                    subTaskBox.unbind();
                }
            }
            else if (change.wasAdded()) {
                for (int i = change.getFrom(); i < change.getTo(); i++) {
                    taskList.getChildren().add(i, new TaskBox(change.getList().get(i)));
                }
            }
        }
    }
}
