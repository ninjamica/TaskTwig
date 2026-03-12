package ninjamica.tasktwig.ui.PropertySheetItems;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.PropertyEditor;

import java.util.Objects;
import java.util.Optional;

public class ButtonItem implements PropertySheet.Item {
    public record ButtonItemState(StringProperty text, BooleanProperty enabled, EventHandler<ActionEvent> onAction) {}

    private final ButtonItemState state;
    private final String name, category, description;

    public ButtonItem(ButtonItemState state, String name, String category, String description) {
        this.state = state;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    @Override
    public Class<?> getType() {
        return ButtonItemState.class;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Object getValue() {
        return state;
    }

    @Override
    public void setValue(Object value) {

    }

    @Override
    public Optional<ObservableValue<? extends Object>> getObservableValue() {
        return Optional.empty();
    }

    @Override
    public Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
        return Optional.of(ButtonEditor.class);
    }

    @Override
    public boolean isEditable() {
        return state.enabled().get();
    }


    public static class ButtonEditor implements PropertyEditor<ButtonItemState> {
        private final Button button = new Button();
        private final ButtonItemState state;

        public ButtonEditor(PropertySheet.Item property) {
            if (Objects.requireNonNull(property) instanceof ButtonItem buttonItem) {
                state = ((ButtonItemState) buttonItem.getValue());
                button.textProperty().bind(state.text());
                button.setOnAction(state.onAction);
            }
            else {
                throw new IllegalStateException("Unsupported type for ButtonEditor: " + property.getClass());
            }
        }

        @Override
        public Node getEditor() {
            return button;
        }

        @Override
        public ButtonItemState getValue() {
            return state;
        }

        @Override
        public void setValue(ButtonItemState value) {

        }
    }
}
