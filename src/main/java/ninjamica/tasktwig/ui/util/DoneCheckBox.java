package ninjamica.tasktwig.ui.util;

import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class DoneCheckBox extends CheckBox {
    private Consumer<Boolean> fireCallback;

    public DoneCheckBox() {
        this(null);
    }

    public DoneCheckBox(Consumer<Boolean> callback) {
        this(callback, false);
    }

    public DoneCheckBox(Consumer<Boolean> callback, boolean initialValue) {
        setFireCallback(callback);
        setSelected(initialValue);
    }

    public void setFireCallback(@Nullable Consumer<Boolean> callback) {
        fireCallback = (callback == null) ? (done -> {}) : callback;
    }

    @Override
    public void fire() {
        if (fireCallback != null)
            fireCallback.accept(!isSelected());
//        fireEvent(new ActionEvent());
    }

}