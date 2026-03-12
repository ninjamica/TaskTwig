package ninjamica.tasktwig.ui.PropertySheetItems;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.PropertyEditor;

import java.util.Objects;
import java.util.Optional;

public class LabelItem implements PropertySheet.Item {
    private final ReadOnlyStringProperty textProperty;
    private final String name, category, description;

    public LabelItem(ReadOnlyStringProperty property, String name, String category, String description) {
        this.textProperty = property;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    @Override
    public Class<?> getType() {
        return String.class;
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
        return textProperty.getValue();
    }

    @Override
    public void setValue(Object value) {

    }

    @Override
    public Optional<ObservableValue<?>> getObservableValue() {
        return Optional.of(textProperty);
    }

    @Override
    public Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
        return Optional.of(LabelEditor.class);
    }

    public ReadOnlyStringProperty getTextProperty() {
        return textProperty;
    }

    public static class LabelEditor implements PropertyEditor<String> {
        private final Label label = new Label();

        public LabelEditor(PropertySheet.Item property) {
            if (Objects.requireNonNull(property) instanceof LabelItem labelItem) {
                label.textProperty().bind(labelItem.getTextProperty());
                System.out.println(label.getText());
            }
            else {
                throw new IllegalStateException("Unsupported type for LabelEditor: " + property.getClass());
            }
        }

        @Override
        public Node getEditor() {
            return label;
        }

        @Override
        public String getValue() {
            return label.getText();
        }

        @Override
        public void setValue(String value) {

        }
    }
}
