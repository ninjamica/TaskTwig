package ninjamica.tasktwig.ui.util;

import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.TaskInterface;

public abstract class TaskBoxBase<T extends TaskInterface> extends VBox {

    protected final Text nameText = new Text();
    protected final DoneCheckBox doneCheckBox = new DoneCheckBox();
    protected final HBox taskSection = new HBox(5, doneCheckBox, nameText);

    protected Subscription subscriptions = Subscription.EMPTY;

    protected TaskBoxBase() {
        nameText.getStyleClass().add(Styles.TEXT);
        nameText.strikethroughProperty().bind(doneCheckBox.selectedProperty());
        doneCheckBox.selectedProperty().subscribe(done -> setOpacity(done ? 0.5 : 1.0));

        taskSection.setOnMouseClicked(event -> doneCheckBox.fire());
        taskSection.setOnMouseEntered(event -> nameText.setUnderline(true));
        taskSection.setOnMouseExited(event -> nameText.setUnderline(false));

        super.getChildren().add(taskSection);
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

    @Override
    public ObservableList<Node> getChildren() {
        return FXCollections.unmodifiableObservableList(super.getChildren());
    }

    protected ObservableList<Node> getVBoxChildren() {
        return super.getChildren();
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
