package ninjamica.tasktwig.ui.util;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;
import java.util.function.Function;

public class DraggableListBox<T, U extends Node> extends ListBox<T, DraggableNodeWrapper<T, U>> {

    private DraggableNodeWrapper<T, U> selectedNode;

    public DraggableListBox(Function<T, U> constructor, Consumer<U> destructor) {

        Node upButton = new FontIcon(FontAwesomeSolid.ARROW_UP);
        upButton.setStyle("-fx-icon-color: -color-fg-muted;");
        upButton.setOnMouseEntered(event -> upButton.setStyle("-fx-icon-color: -color-fg-default;"));
        upButton.setOnMouseExited(event -> upButton.setStyle("-fx-icon-color: -color-fg-muted;"));

        Node downButton = new FontIcon(FontAwesomeSolid.ARROW_DOWN);
        downButton.setStyle("-fx-icon-color: -color-fg-muted;");
        downButton.setOnMouseEntered(event -> downButton.setStyle("-fx-icon-color: -color-fg-default;"));
        downButton.setOnMouseExited(event -> downButton.setStyle("-fx-icon-color: -color-fg-muted;"));

        VBox buttonBox = new VBox(upButton, downButton);
        buttonBox.setAlignment(Pos.CENTER);

        Consumer<DraggableNodeWrapper<T, U>> mouseEntered = draggableNode -> {
            if (selectedNode != null) {
                selectedNode.getChildren().remove(buttonBox);
            }
            draggableNode.getChildren().add(buttonBox);
            setSelectedNode(draggableNode);
        };
        Runnable mouseExited = () -> {
            if (selectedNode != null) {
                selectedNode.getChildren().remove(buttonBox);
            }
            setSelectedNode(null);
        };

        upButton.setOnMousePressed(event -> {
            DraggableNodeWrapper<T, U> node = getSelectedNode();
            if (node != null) {
                int index = getItems().indexOf(node.getItem());

                if (index > 0) {
                    T tempItem = getItems().remove(index-1);
                    getItems().add(index, tempItem);
                }
            }
        });
        downButton.setOnMousePressed(event -> {
            DraggableNodeWrapper<T, U> node = getSelectedNode();
            if (node != null) {
                int index = getItems().indexOf(node.getItem());

                if (index < getItems().size() - 1) {
                    T tempItem = getItems().remove(index+1);
                    getItems().add(index, tempItem);
                }
            }
        });

        setupListBox(
                item -> new DraggableNodeWrapper<>(item, constructor, mouseEntered, mouseExited),
                node -> destructor.accept(node.getNode())
        );
    }

    private void setSelectedNode(DraggableNodeWrapper<T, U> node) {
        selectedNode = node;
    }

    private DraggableNodeWrapper<T, U> getSelectedNode() {
        return selectedNode;
    }
}

class DraggableNodeWrapper<T, U extends Node> extends HBox {
    private final T item;
    private final U node;

    public DraggableNodeWrapper(T item, Function<T, U> constructor,
                                Consumer<DraggableNodeWrapper<T, U>> onMouseEntered, Runnable onMouseExited) {
        super(10);
        setFillHeight(true);

        this.item = item;
        node = constructor.apply(item);

        setOnMouseEntered(event -> onMouseEntered.accept(this));
        setOnMouseExited(event -> onMouseExited.run());

        getChildren().add(node);
    }

    public T getItem() {
        return item;
    }

    public U getNode() {
        return node;
    }
}
