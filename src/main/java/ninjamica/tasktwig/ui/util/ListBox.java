package ninjamica.tasktwig.ui.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Function;

public class ListBox<E, N extends Node> extends VBox implements ListBoxInterface<E> {

    private final ListChangeMapper<E, Node> listChangeMapper;
    private ObservableList<E> items;

    public ListBox(Function<E, N> constructor, Consumer<N> destructor) {
        super(10);

        listChangeMapper = new ListChangeMapper<>(
                super.getChildren(),
                (Function<E, Node>) constructor,
                (Consumer<Node>) destructor
        );
    }

    public void setItems(ObservableList<E> items) {
        unbind();

        if (items != null) {
            this.items = items;
            listChangeMapper.constructFromList(items);
            items.addListener(listChangeMapper);
        }
    }

    public ObservableList<E> getItems() {
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

    public Node getNode() {
        return this;
    }

    @Override
    public ObservableList<Node> getChildren() {
        return FXCollections.unmodifiableObservableList(super.getChildren());
    }
}
