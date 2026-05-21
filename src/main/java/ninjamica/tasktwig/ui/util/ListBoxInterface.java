package ninjamica.tasktwig.ui.util;

import javafx.collections.ObservableList;
import javafx.scene.Node;

public interface ListBoxInterface<E> {
    void setItems(ObservableList<E> items);
    ObservableList<E> getItems();
    void setAfterChangeRunnable(Runnable afterChangeRunnable);
    void unbind();
    Node getNode();
}
