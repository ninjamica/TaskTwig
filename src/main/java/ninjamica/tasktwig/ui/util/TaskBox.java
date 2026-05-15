package ninjamica.tasktwig.ui.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.TwigSubTask;
import ninjamica.tasktwig.core.TwigTask;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

public class TaskBox extends TaskBoxBase<TwigTask> {

    private final FontIcon expandIcon = new FontIcon();
    private final ListBox<TwigSubTask, SubTaskBox> subTaskBox;

    private final BooleanProperty expanded = new SimpleBooleanProperty();

    public TaskBox() {
        setSpacing(10);

        HBox expandIconPane = new HBox(expandIcon);
        expandIconPane.setPrefWidth(15);
        expandIconPane.setMinWidth(USE_PREF_SIZE);
        expandIconPane.setMaxWidth(USE_PREF_SIZE);

        expandIconPane.setOnMouseClicked(event -> {
            if (expandIcon.isVisible())
                expanded.set(!expanded.get());
        });
        expandIconPane.setOnMouseEntered(event -> expandIcon.setStyle("-fx-icon-color: -color-fg-default;"));
        expandIconPane.setOnMouseExited(event -> expandIcon.setStyle("-fx-icon-color: -color-fg-muted;"));
        expandIcon.setStyle("-fx-icon-color: -color-fg-muted;");

        subTaskBox = new ListBox<>(SubTaskBox::new, SubTaskBox::unbind);
        subTaskBox.setAfterChangeRunnable(this::updateSubtaskBox);
        subTaskBox.setPadding(new Insets(0, 0, 0, 30));

        getVBoxChildren().setAll(new HBox(5, expandIconPane, taskSection));
    }

    public TaskBox(TwigTask task) {
        this();
        setTask(task);
    }

    public void setTask(TwigTask task) {
        super.setTask(task);

        if (task != null) {

            expanded.bindBidirectional(task.subTasksExpandedProperty());

            subTaskBox.setItems(task.getSubTasks());
            updateSubtaskBox();

            subscriptions = subscriptions.and(Subscription.combine(
                    () -> expanded.unbindBidirectional(task.subTasksExpandedProperty()),
                    subTaskBox::unbind,
                    expanded.subscribe(_ -> updateSubtaskBox())
            ));
        }
    }

    @Override
    public ObservableList<Node> getChildren() {
        return FXCollections.unmodifiableObservableList(super.getChildren());
    }

    private void updateSubtaskBox() {
        boolean isEmpty = subTaskBox.getItems().isEmpty();
        expandIcon.setVisible(!isEmpty);
        if (isEmpty) {
            getVBoxChildren().remove(subTaskBox);
        }
        else {
            if (expanded.get()) {
                expandIcon.setIconCode(FontAwesomeSolid.CARET_DOWN);
                if(!getVBoxChildren().contains(subTaskBox))
                    getVBoxChildren().add(subTaskBox);
            }
            else {
                expandIcon.setIconCode(FontAwesomeSolid.CARET_RIGHT);
                getVBoxChildren().remove(subTaskBox);
            }
        }
    }
}