package ninjamica.tasktwig.ui.util;

import atlantafx.base.theme.Styles;
import javafx.scene.Cursor;
import javafx.scene.control.TableCell;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A table cell representing a delete button (as a trash can).
 * When clicked it simply deletes the row containing this cell.
 * Template types do not matter and are ignored
 */
public class CloseTableCell<S> extends TableCell<S, Void> {
    final FontIcon closeIcon = new FontIcon(FontAwesomeSolid.TRASH_ALT);

    public CloseTableCell() {
        setText(null);
        closeIcon.setCursor(Cursor.HAND);
        closeIcon.setOnMouseClicked(event -> getTableView().getItems().remove(getIndex()));
        closeIcon.setOnMouseEntered(event -> closeIcon.pseudoClassStateChanged(Styles.STATE_DANGER, true));
        closeIcon.setOnMouseExited(event -> closeIcon.pseudoClassStateChanged(Styles.STATE_DANGER, false));
    }

    @Override
    public void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty)
            setGraphic(null);
        else
            setGraphic(closeIcon);
    }
}