package ninjamica.tasktwig.ui.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Function;

public class ListBox<T, U extends Node> extends VBox {

    private ListChangeMapper<T, Node> listChangeMapper;
    private ObservableList<T> items;

    public ListBox(Function<T, U> constructor, Consumer<U> destructor) {
        super(10);

        listChangeMapper = new ListChangeMapper<>(
                super.getChildren(),
                (Function<T, Node>) constructor,
                (Consumer<Node>) destructor
        );
    }

    protected ListBox() {}

    protected void setupListBox(Function<T, U> constructor, Consumer<U> destructor) {
        if (items == null) {
            listChangeMapper = new ListChangeMapper<>(
                    super.getChildren(),
                    (Function<T, Node>) constructor,
                    (Consumer<Node>) destructor
            );
        }
        else {
            throw new IllegalStateException("ListBox has already been initialized");
        }
    }

    public void setItems(ObservableList<T> items) {
        unbind();

        if (items != null) {
            this.items = items;
            listChangeMapper.constructFromList(items);
            items.addListener(listChangeMapper);
        }
    }

    public ObservableList<T> getItems() {
        return items;
    }

    public void setAfterChangeRunnable(Runnable afterChangeRunnable) {
        listChangeMapper.setAfterChangeRunnable(afterChangeRunnable);
    }

    public void unbind() {
        if (items != null)
            items.removeListener(listChangeMapper);

        items = null;
        super.getChildren().clear();
    }

    @Override
    public ObservableList<Node> getChildren() {
        return FXCollections.unmodifiableObservableList(super.getChildren());
    }
}
