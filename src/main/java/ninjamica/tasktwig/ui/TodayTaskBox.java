package ninjamica.tasktwig.ui;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.Task;
import ninjamica.tasktwig.ui.util.DoneCheckBox;
import ninjamica.tasktwig.ui.util.ListBox;
import ninjamica.tasktwig.ui.util.TaskBoxBase;

public class TodayTaskBox extends TaskBoxBase {

    protected final DoneCheckBox doneCheckBox = new DoneCheckBox();
    protected Subscription subscriptions = Subscription.EMPTY;

    public TodayTaskBox() {
        super(() -> new ListBox<>(
                TodaySubTaskBox::new,
                TodaySubTaskBox::unbind
        ));
        nameText.strikethroughProperty().bind(doneCheckBox.selectedProperty());
        doneCheckBox.selectedProperty().subscribe(done -> setOpacity(done ? 0.5 : 1.0));

        nameBox.getChildren().add(1, doneCheckBox);
        nameBox.setSpacing(5);

        nameBox.setOnMouseClicked(event -> doneCheckBox.fire());
        nameBox.setOnMouseEntered(event -> nameText.setUnderline(true));
        nameBox.setOnMouseExited(event -> nameText.setUnderline(false));

        VBox.setMargin(subTaskBox.getNode(), new Insets(0, 0, 0, 40));
    }

    public TodayTaskBox(Task task) {
        this();
        setTask(task);
    }

    public void setTask(Task task) {
        super.setTask(task);

        if (task != null) {
            updateOverdue(task.isOverdue());

            doneCheckBox.selectedProperty().bind(task.isDoneObservable());
            doneCheckBox.setFireCallback(task::setDone);

            subscriptions = Subscription.combine(
                    task.isOverdueObservable().subscribe(this::updateOverdue),
                    doneCheckBox.selectedProperty()::unbind,
                    () -> doneCheckBox.setFireCallback(null)
            );
        }
    }

    public void unbind() {
        super.unbind();
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