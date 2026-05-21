package ninjamica.tasktwig.ui.util;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;
import java.util.function.Function;

public class DraggableListBox<E, N extends Node> extends StackPane implements ListBoxInterface<E> {

    private final ListBox<E, DraggableNodeWrapper> listBox;
    private DraggableNodeWrapper draggedNode;
    private final ImageView dragNodeImage = new ImageView();
    private final Pos dragIconAlignment;

    private String nodeStyle;
    private String[] nodeStyleClasses;

    public DraggableListBox(Function<E, N> constructor, Consumer<N> destructor, Pos dragIconAlignment) {

        listBox = new ListBox<>(
                item -> new DraggableNodeWrapper(item, constructor),
                node -> destructor.accept(node.getNode())
        );
        listBox.setFillWidth(false);


        this.dragIconAlignment = dragIconAlignment;
        dragNodeImage.setVisible(false);
        AnchorPane dragNodePane = new AnchorPane(dragNodeImage);
        dragNodePane.setMouseTransparent(true);

        super.getChildren().addAll(listBox, dragNodePane);
    }

    public void setItems(ObservableList<E> items) {
        listBox.setItems(items);
    }

    public ObservableList<E> getItems() {
        return listBox.getItems();
    }

    public void setAfterChangeRunnable(Runnable afterChangeRunnable) {
        listBox.setAfterChangeRunnable(afterChangeRunnable);
    }

    public void setNodeStyle(String style, String... styleClasses) {
        nodeStyle = style;
        nodeStyleClasses = styleClasses;
    }

    public void unbind() {
        listBox.unbind();
    }

    public Node getNode() {
        return this;
    }

    private class DraggableNodeWrapper extends HBox {
        private final E item;
        private final N node;

        private final FontIcon dragIcon;

        public DraggableNodeWrapper(E item, Function<E, N> constructor) {
            super(10);
            setFillHeight(true);
            setAlignment(dragIconAlignment);

            this.item = item;
            node = constructor.apply(item);
            dragIcon = new FontIcon(FontAwesomeSolid.GRIP_LINES);
            dragIcon.setCursor(Cursor.OPEN_HAND);
            getChildren().addAll(dragIcon, node);

            if (nodeStyle != null) {
                setStyle(nodeStyle);
            }
            if (nodeStyleClasses != null) {
                getStyleClass().addAll(nodeStyleClasses);
            }

            dragIcon.setOnMousePressed(this::startDrag);
//            dragIcon.setOnDragDetected(this::startDrag);
            dragIcon.setOnMouseReleased(this::finishDrag);
        }

        private void startDrag(MouseEvent mouseEvent) {
            if (draggedNode == null) {
                draggedNode = this;

                double parentHeightOffset = this.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY()).getY()
                                            - this.getBoundsInLocal().getMinY();
                double parentWidthOffset = -this.getBoundsInParent().getMinX();

                dragNodeImage.setLayoutY(-parentHeightOffset);
                dragNodeImage.setLayoutX(-parentWidthOffset);

                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                dragNodeImage.setImage(snapshot(params, null));

                setVisible(false);
                dragNodeImage.setVisible(true);
                updateDragNodePos(mouseEvent);

                dragIcon.setCursor(Cursor.CLOSED_HAND);
                dragIcon.setOnMouseDragged(this::updateDrag);
            }
        }

        private void updateDrag(MouseEvent mouseEvent) {
            if (draggedNode != null) {
                updateDragNodePos(mouseEvent);

                double dragNodeHeight = dragNodeImage.getBoundsInParent().getCenterY();
                Bounds nodeBounds = draggedNode.getBoundsInParent();
                int itemIndex = getItems().indexOf(item);

                if (dragNodeHeight < nodeBounds.getMinY() - getSpacing() && itemIndex > 0) {
                    E tempItem = getItems().remove(itemIndex - 1);
                    getItems().add(itemIndex, tempItem);
                }
                else if (dragNodeHeight > nodeBounds.getMaxY() + getSpacing() && itemIndex < getItems().size() - 1) {
                    E tempItem = getItems().remove(itemIndex + 1);
                    getItems().add(itemIndex, tempItem);
                }
            }
        }

        private void updateDragNodePos(MouseEvent mouseEvent) {
            double offsetY = DraggableListBox.this.sceneToLocal(mouseEvent.getSceneX(),
                                                                mouseEvent.getSceneY()).getY();

            double layoutY = dragNodeImage.getLayoutY();
            dragNodeImage.setTranslateY(
                    Math.clamp(
                            offsetY,
                            -layoutY,
                            DraggableListBox.this.getHeight() - this.getHeight() - layoutY
                    )
            );
        }

        private void finishDrag(MouseEvent mouseEvent) {
            if (draggedNode != null) {
                draggedNode = null;
                setVisible(true);
                dragNodeImage.setVisible(false);
                dragIcon.setCursor(Cursor.OPEN_HAND);
                dragIcon.setOnMouseMoved(null);
            }
        }

        public E getItem() {
            return item;
        }

        public N getNode() {
            return node;
        }
    }
}
