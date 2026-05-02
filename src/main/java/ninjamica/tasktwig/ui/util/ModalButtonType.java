package ninjamica.tasktwig.ui.util;

import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public record ModalButtonType(String text, ButtonBar.ButtonData buttonData, Optional<Node> icon) {

    public ModalButtonType(String text, ButtonBar.ButtonData buttonData, Node icon) {
        this(text, buttonData, Optional.of(icon));
    }

    public ModalButtonType(String text, ButtonBar.ButtonData buttonData) {
        this(text, buttonData, Optional.empty());
    }

    public ModalButtonType(ButtonType buttonType, Node icon) {
        this(buttonType.getText(), buttonType.getButtonData(), Optional.ofNullable(icon));
    }

    public ModalButtonType(ButtonType buttonType) {
        this(buttonType.getText(), buttonType.getButtonData(), Optional.empty());
    }

    public static final ModalButtonType APPLY = new ModalButtonType(ButtonType.APPLY);
    public static final ModalButtonType OK = new ModalButtonType(ButtonType.OK);
    public static final ModalButtonType CANCEL = new ModalButtonType(ButtonType.CANCEL);
    public static final ModalButtonType CLOSE = new ModalButtonType(ButtonType.CLOSE);
    public static final ModalButtonType YES = new ModalButtonType(ButtonType.YES);
    public static final ModalButtonType NO = new ModalButtonType(ButtonType.NO);
    public static final ModalButtonType FINISH = new ModalButtonType(ButtonType.FINISH);
    public static final ModalButtonType NEXT = new ModalButtonType(ButtonType.NEXT);
    public static final ModalButtonType PREVIOUS = new ModalButtonType(ButtonType.PREVIOUS);
}