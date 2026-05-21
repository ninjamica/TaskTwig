package ninjamica.tasktwig.ui;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Tweaks;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.SubTask;
import ninjamica.tasktwig.ui.util.TimeInput;

public class SubTaskPropertyPane extends VBox {
    private TextField nameTextField;
    private TimeInput dueTimeInput;

    private Subscription subscriptions = Subscription.EMPTY;

    public SubTaskPropertyPane() {
        initializeUI();
    }

    public SubTaskPropertyPane(SubTask subTask) {
        this();
        setSubTask(subTask);
    }

    private void initializeUI() {

        Card nameCard = new Card();
        nameCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        nameCard.setHeader(new Label("Name:"));
        nameCard.setFocusTraversable(false);
        nameTextField = new TextField();
        nameTextField.setPromptText("LegacyTask Name");
        nameTextField.setPrefWidth(230);
        nameCard.setBody(nameTextField);

        Card dueTimeCard = new Card();
        dueTimeCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        dueTimeCard.setHeader(new Label("Due Time:"));
        dueTimeCard.setFocusTraversable(false);
        dueTimeInput = new TimeInput();
        dueTimeInput.setMaxWidth(110);
        dueTimeCard.setBody(dueTimeInput);

        getChildren().addAll(nameCard, dueTimeCard);
    }

    public void setSubTask(SubTask subTask) {
        subscriptions.unsubscribe();
        subscriptions = Subscription.EMPTY;

        if (subTask != null) {
            nameTextField.textProperty().bindBidirectional(subTask.nameProperty());
            dueTimeInput.timeValueProperty().bindBidirectional(subTask.dueTimeProperty());

            subscriptions = Subscription.combine(
                    () -> nameTextField.textProperty().unbindBidirectional(subTask.nameProperty()),
                    () -> dueTimeInput.timeValueProperty().unbindBidirectional(subTask.dueTimeProperty())
            );
        }
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        Platform.runLater(() -> nameTextField.requestFocus());
    }
}
