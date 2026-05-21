package ninjamica.tasktwig.ui;

import atlantafx.base.theme.Styles;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.SubTask;
import ninjamica.tasktwig.ui.util.DoneCheckBox;
import ninjamica.tasktwig.ui.util.TaskInterfaceBoxBase;

public class TodaySubTaskBox extends TaskInterfaceBoxBase<SubTask> {
    protected final DoneCheckBox doneCheckBox = new DoneCheckBox();
    protected Subscription subscriptions = Subscription.EMPTY;

    public TodaySubTaskBox() {
        nameText.getStyleClass().add(Styles.TEXT);
        nameText.strikethroughProperty().bind(doneCheckBox.selectedProperty());
        doneCheckBox.selectedProperty().subscribe(done -> setOpacity(done ? 0.5 : 1.0));

        nameBox.getChildren().setAll(doneCheckBox, nameText);
        nameBox.setSpacing(5);

        nameBox.setOnMouseClicked(event -> doneCheckBox.fire());
        nameBox.setOnMouseEntered(event -> nameText.setUnderline(true));
        nameBox.setOnMouseExited(event -> nameText.setUnderline(false));
    }

    public TodaySubTaskBox(SubTask subTask) {
        this();
        setTask(subTask);
    }

    public void setTask(SubTask subTask) {
        super.setTask(subTask);

        if (subTask != null) {
            updateOverdue(subTask.isOverdue());

            doneCheckBox.selectedProperty().bind(subTask.isDoneObservable());
            doneCheckBox.setFireCallback(subTask::setDone);

            subscriptions = Subscription.combine(
                    subTask.isOverdueObservable().subscribe(this::updateOverdue),
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
