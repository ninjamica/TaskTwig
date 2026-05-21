package ninjamica.tasktwig.ui;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.TaskCategory;

public class TaskCategoryBox extends Region {

    private final Text nameText = new Text();
    private final ColorPicker colorPicker = new ColorPicker();

    private Subscription subs = Subscription.EMPTY;

    public TaskCategoryBox(TaskCategory taskCategory) {
        colorPicker.getStyleClass().add(ColorPicker.STYLE_CLASS_BUTTON);
        colorPicker.setStyle("-fx-color-label-visible: false;");
        nameText.getStyleClass().add(Styles.TITLE_4);
        nameText.fillProperty().bind(colorPicker.valueProperty());

        HBox contentBox = new HBox(10, nameText);
        contentBox.setAlignment(Pos.BASELINE_LEFT);

//        Card taskCard = new Card();
//        taskCard.setBody(contentBox);
//        taskCard.getStyleClass().add(Styles.INTERACTIVE);

        getChildren().add(contentBox);

        setCategory(taskCategory);
    }

    public void setCategory(TaskCategory category) {
        unbind();

        if (category != null) {
            nameText.textProperty().bind(category.nameProperty());
            colorPicker.valueProperty().bindBidirectional(category.colorProperty());

            subs = Subscription.combine(
                    nameText.textProperty()::unbind,
                    () -> colorPicker.valueProperty().unbindBidirectional(category.colorProperty())
            );
        }
    }

    public void unbind() {
        subs.unsubscribe();
    }
}
