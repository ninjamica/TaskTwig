package ninjamica.tasktwig.ui;

import atlantafx.base.controls.Tile;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.TaskCategory;

public class TaskCategoryPropertyPane extends VBox {
    private final TextField nameField = new TextField();
    private final ColorPicker colorPicker = new ColorPicker();

    private Subscription subs = Subscription.EMPTY;

    public TaskCategoryPropertyPane() {
        super(10);

        Tile nameTile = new Tile("Name:", "describes the category");
        nameTile.setAction(nameField);
        nameTile.setActionHandler(nameField::requestFocus);

        Tile colorTile = new Tile("Color:", "add some visual flair");
        colorPicker.getStyleClass().add(ColorPicker.STYLE_CLASS_BUTTON);
        colorTile.setAction(colorPicker);
        colorTile.setActionHandler(colorPicker::show);

        getChildren().addAll(nameTile, colorTile);
    }

    public TaskCategoryPropertyPane(TaskCategory taskCategory) {
        this();
        setCategory(taskCategory);
    }

    public void setCategory(TaskCategory taskCategory) {
        subs.unsubscribe();

        nameField.textProperty().bindBidirectional(taskCategory.nameProperty());
        colorPicker.valueProperty().bindBidirectional(taskCategory.colorProperty());

        subs = Subscription.combine(
                () -> nameField.textProperty().unbindBidirectional(taskCategory.nameProperty()),
                () -> colorPicker.valueProperty().unbindBidirectional(taskCategory.colorProperty())
        );
    }
}
