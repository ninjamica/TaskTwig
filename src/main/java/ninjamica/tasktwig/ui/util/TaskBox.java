package ninjamica.tasktwig.ui.util;

import atlantafx.base.theme.Styles;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.TaskInterface;
import ninjamica.tasktwig.core.TwigSubTask;
import ninjamica.tasktwig.core.TwigTask;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

class TaskBoxBase<T extends TaskInterface> extends VBox {

    protected final Text nameText = new Text();
    protected final DoneCheckBox doneCheckBox = new DoneCheckBox();
    protected final HBox taskSection = new HBox(5, doneCheckBox, nameText);

    protected Subscription subscriptions = Subscription.EMPTY;

    protected TaskBoxBase() {
        super(10);

        nameText.getStyleClass().add(Styles.TEXT);
        nameText.strikethroughProperty().bind(doneCheckBox.selectedProperty());

        taskSection.setOnMouseClicked(event -> doneCheckBox.fire());
        taskSection.setOnMouseEntered(event -> nameText.setUnderline(true));
        taskSection.setOnMouseExited(event -> nameText.setUnderline(false));

        getChildren().add(taskSection);
    }

    protected TaskBoxBase(T task) {
        this();
        setTask(task);
    }

    protected void setTask(T task) {
        unbind();

        if (task != null) {
            nameText.textProperty().bind(task.nameProperty());
            updateOverdue(task.isOverdue());

            doneCheckBox.selectedProperty().bind(task.isDoneObservable());
            doneCheckBox.setFireCallback(task::setDone);

            subscriptions = Subscription.combine(
                    nameText.textProperty()::unbind,
                    task.isOverdueObservable().subscribe(this::updateOverdue),
                    doneCheckBox.selectedProperty()::unbind,
                    () -> doneCheckBox.setFireCallback(null)
            );
        }
    }

    void unbind() {
        subscriptions.unsubscribe();
    }

    private void updateOverdue(boolean isOverdue) {
        if (isOverdue) {
            if (!nameText.getStyleClass().contains(Styles.DANGER))
                nameText.getStyleClass().add(Styles.DANGER);
        }
        else {
            nameText.getStyleClass().remove(Styles.DANGER);
        }
    }
}

public class TaskBox extends TaskBoxBase<TwigTask> {

    private final FontIcon expandIcon = new FontIcon();
    private final VBox subTaskBox = new VBox(10);

    private final BooleanProperty expanded = new SimpleBooleanProperty();

    public TaskBox() {
        expandIcon.minWidth(100);
        expandIcon.setOnMouseClicked(event -> {
            if (expandIcon.isVisible())
                expanded.set(!expanded.get());
        });

        expanded.subscribe(this::updateExpanded);

        subTaskBox.setPadding(new Insets(0, 0, 0, 30));

        getChildren().setAll(new HBox(5, expandIcon, taskSection));
    }

    public TaskBox(TwigTask task) {
        this();
        setTask(task);
    }

    public void setTask(TwigTask task) {
        super.setTask(task);

        if (task != null) {

            expanded.bindBidirectional(task.subTasksExpandedProperty());

            fillSubTasks(task);
            updateExpanded(task.isExpanded());
            updateHasSubtasks(task.getSubTasks());
            task.getSubTasks().addListener(this::subTaskListChange);

            subscriptions = subscriptions.and(Subscription.combine(
                    () -> expanded.unbindBidirectional(task.subTasksExpandedProperty()),
                    () -> task.getSubTasks().removeListener(this::subTaskListChange),
                    subTaskBox.getChildren()::clear
            ));
        }
    }

    private void updateHasSubtasks(List<TwigSubTask> subTasks) {
        expandIcon.setVisible(!subTasks.isEmpty());
        if (subTasks.isEmpty()) {
            getChildren().remove(subTaskBox);
        }
        else if (!getChildren().contains(subTaskBox)) {
            getChildren().add(subTaskBox);
        }
    }

    private void updateExpanded(boolean isExpanded) {
        if (isExpanded) {
            expandIcon.setIconCode(FontAwesomeSolid.CARET_DOWN);
            if(!getChildren().contains(subTaskBox))
                getChildren().add(subTaskBox);
        }
        else {
            expandIcon.setIconCode(FontAwesomeSolid.CARET_RIGHT);
            getChildren().remove(subTaskBox);
        }
    }

    private void fillSubTasks(TwigTask task) {
        subTaskBox.getChildren().clear();
        for (TwigSubTask subTask : task.getSubTasks()) {
            subTaskBox.getChildren().add(new TaskBoxBase<>(subTask));
        }
    }

    private void subTaskListChange(ListChangeListener.Change<? extends TwigSubTask> change) {
        while (change.next()) {
            if (change.wasPermutated()) {
                for (int i = change.getFrom(); i <= change.getTo(); ++i) {
                    Node cell = subTaskBox.getChildren().remove(i);
                    subTaskBox.getChildren().add(change.getPermutation(i), cell);
                }
            }
            else if (change.wasReplaced()) {
                for (int i = change.getFrom(); i < change.getTo(); ++i) {
                    TaskBoxBase<TwigSubTask> subTaskBox = (TaskBoxBase<TwigSubTask>) this.subTaskBox.getChildren().get(i);
                    TwigSubTask newTask = change.getList().get(i);
                    subTaskBox.setTask(newTask);
                }
            }
            else if (change.wasRemoved()) {
                for (int i = change.getFrom(); i < change.getTo(); ++i) {
                    TaskBoxBase<TwigSubTask> subTaskBox = (TaskBoxBase<TwigSubTask>) this.subTaskBox.getChildren().remove(i);
                    subTaskBox.unbind();
                }
            }
            else if (change.wasAdded()) {
                for (int i = change.getFrom(); i < change.getTo(); i++) {
                    subTaskBox.getChildren().add(i, new TaskBoxBase<>(change.getList().get(i)));
                }
            }
        }

        if(subTaskBox.getChildren().isEmpty()) {
            expandIcon.setVisible(false);
            getChildren().remove(subTaskBox);
        }
        else {
            expandIcon.setVisible(true);
            if (!getChildren().contains(subTaskBox))
                getChildren().add(subTaskBox);
        }
    }
}