package ninjamica.tasktwig.ui;

import atlantafx.base.theme.Styles;
import javafx.beans.value.ObservableIntegerValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.Task;
import ninjamica.tasktwig.core.TaskCategory;
import ninjamica.tasktwig.ui.util.ListBox;
import ninjamica.tasktwig.ui.util.TaskCategoryViewBase;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TodayTaskCategoryView<N extends Node> extends TaskCategoryViewBase {

    private final Text countText = new Text();
    private Subscription subs = Subscription.EMPTY;

    public TodayTaskCategoryView(Function<Task, N> taskNodeConstructor,
                                 Consumer<N> taskNodeDestructor) {
        super(() -> new ListBox<>(
                taskNodeConstructor,
                taskNodeDestructor
        ));

        countText.getStyleClass().add(Styles.TEXT_SUBTLE);
        nameBox.getChildren().add(countText);

        VBox.setMargin(taskList.getNode(), new Insets(0, 0, 0, 20));
    }

    public TodayTaskCategoryView(Function<Task, N> taskNodeConstructor, Consumer<N> taskNodeDestructor,
                                 TaskCategory category, Predicate<Task> filter) {
        this(taskNodeConstructor, taskNodeDestructor);
        setCategory(category, filter);
    }

    public void setCategory(TaskCategory category, Predicate<Task> filter) {
        super.setCategory(category, filter);

        if (category != null) {
            countText.setVisible(true);

            subs = Subscription.combine(
                    category.doneTodayCountProperty().subscribe(
                            count -> updateCountText(category.doneTodayCountProperty(), category.todayCountProperty())),
                    category.todayCountProperty().subscribe(
                            count -> updateCountText(category.doneTodayCountProperty(), category.todayCountProperty()))
            );
        }
    }

    public void unbind() {
        super.unbind();
        subs.unsubscribe();
    }

    private void updateCountText(ObservableIntegerValue doneToday, ObservableIntegerValue total) {
        updateCountText(doneToday.get(), total.get());
    }

    private void updateCountText(int doneToday, int total) {
        countText.setText(doneToday + "/" + total);
    }
}
