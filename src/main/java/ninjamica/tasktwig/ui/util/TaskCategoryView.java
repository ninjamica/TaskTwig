package ninjamica.tasktwig.ui.util;

import atlantafx.base.theme.Styles;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableIntegerValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.TaskCategory;
import ninjamica.tasktwig.core.TwigTask;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Predicate;

public class TaskCategoryView extends VBox {

    private final FontIcon expandIcon = new FontIcon();
    private final Text catNameText = new Text();
    private final Text countText = new Text();
    private final ListBox<TwigTask, TaskBox> taskList;

    private final BooleanProperty expanded = new SimpleBooleanProperty(true);
    private Subscription subs = Subscription.EMPTY;

    public TaskCategoryView() {
        super(10);

        HBox expandIconPane = new HBox(expandIcon);
        expandIconPane.setPrefWidth(15);
        expandIconPane.setMinWidth(USE_PREF_SIZE);
        expandIconPane.setMaxWidth(USE_PREF_SIZE);

        expandIconPane.setOnMouseEntered(event -> expandIcon.setStyle("-fx-icon-color: -color-fg-default;"));
        expandIconPane.setOnMouseExited(event -> expandIcon.setStyle("-fx-icon-color: -color-fg-muted;"));
        expandIcon.setStyle("-fx-icon-color: -color-fg-muted;");

        catNameText.getStyleClass().add(Styles.TITLE_3);
        countText.getStyleClass().add(Styles.TEXT_SUBTLE);
        countText.setVisible(false);
        HBox nameBox = new HBox(10, expandIconPane, catNameText, countText);
        nameBox.setAlignment(Pos.BASELINE_LEFT);

        taskList = new ListBox<>(TaskBox::new, TaskBox::unbind);
        taskList.setAfterChangeRunnable(this::updateTaskList);
        taskList.setPadding(new Insets(0, 0, 0, 20));

        getChildren().addAll(nameBox, taskList);

        nameBox.setOnMouseClicked(event -> {
            if (expandIcon.isVisible())
                expanded.set(!expanded.get());
        });
        expanded.subscribe(_ -> updateTaskList());
    }

    public TaskCategoryView(TaskCategory category, Predicate<TwigTask> filter) {
        this();
        setCategory(category, filter);
    }

    public void setCategory(TaskCategory category, Predicate<TwigTask> filter) {
        if (category != null) {
            setCategory(category.tasksProperty(), filter, category.nameProperty(), category.colorProperty());

            countText.setVisible(true);
            subs = subs.and(Subscription.combine(
                    category.doneTodayCountProperty().subscribe(
                            count -> updateCountText(category.doneTodayCountProperty(), category.todayCountProperty())),
                    category.todayCountProperty().subscribe(
                            count -> updateCountText(category.doneTodayCountProperty(), category.todayCountProperty())),
                    () -> countText.setVisible(false)
            ));
        }
        else {
            unbind();
        }
    }

    public void setCategory(ObservableList<TwigTask> tasks, Predicate<TwigTask> filter, StringProperty name, ObjectProperty<Color> color) {
        unbind();

        if (tasks != null) {
            ObservableList<TwigTask> filteredTasks = tasks.filtered(filter);

            catNameText.textProperty().bind(name);
            catNameText.fillProperty().bind(color);
            taskList.setItems(filteredTasks);

            updateTaskList();

            subs = Subscription.combine(
                    catNameText.textProperty()::unbind,
                    catNameText.fillProperty()::unbind,
                    taskList::unbind
            );
        }
    }

    public void unbind() {
        subs.unsubscribe();
    }

    private void updateCountText(ObservableIntegerValue doneToday, ObservableIntegerValue total) {
        updateCountText(doneToday.get(), total.get());
    }

    private void updateCountText(int doneToday, int total) {
        countText.setText(doneToday + "/" + total);
    }

    private void updateTaskList() {
        if (taskList.getItems() != null && expandIcon.isVisible()) {
            boolean isEmpty = taskList.getItems().isEmpty();
            expandIcon.setVisible(!isEmpty);
            if (isEmpty) {
                getChildren().remove(taskList);
            }
            else {
                if (expanded.get()) {
                    expandIcon.setIconCode(FontAwesomeSolid.CARET_DOWN);
                    if (!getChildren().contains(taskList))
                        getChildren().add(taskList);
                }
                else {
                    expandIcon.setIconCode(FontAwesomeSolid.CARET_RIGHT);
                    getChildren().remove(taskList);
                }
            }
        }
    }
}
