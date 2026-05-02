package ninjamica.tasktwig.ui.util;

import atlantafx.base.controls.Card;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Tile;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Tweaks;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AlertModalBox extends ModalBox {

    private final Card alertCard = new Card();
    protected Consumer<ModalButtonType> buttonCallback;
    private final boolean callbackAfterClose;
    private final CompletableFuture<ModalButtonType> buttonTypeFuture = new CompletableFuture<>();

    protected AlertModalBox(@NotNull ModalPane modalPane, String title, String description, boolean callbackAfterClose, ModalButtonType @NotNull ... buttons) {
        super(modalPane);
        this.callbackAfterClose = callbackAfterClose;

        ButtonBar buttonBar = new ButtonBar();
        for (ModalButtonType buttonType : buttons) {
            Button button = new Button(buttonType.text());
            buttonType.icon().ifPresent(button::setGraphic);
            ButtonBar.setButtonData(button, buttonType.buttonData());

            button.setOnAction(event -> onButtonAction(buttonType));
            button.setDefaultButton(buttonType.buttonData().isDefaultButton());
            buttonBar.getButtons().add(button);
        }

        alertCard.setHeader(new Tile(title, description));
        alertCard.setFooter(buttonBar);
        alertCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);

        addContent(alertCard);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    }

    public AlertModalBox(@NotNull ModalPane modalPane, String title, String description, @Nullable Node content, boolean callbackAfterClose, Consumer<ModalButtonType> buttonCallback, ModalButtonType @NotNull ... buttons) {
        this(modalPane, title, description, callbackAfterClose, buttons);
        this.buttonCallback = buttonCallback;

        if (content != null)
            alertCard.setBody(content);
    }

    public AlertModalBox(@NotNull ModalPane modalPane, String title, String description, boolean callbackAfterClose, Consumer<ModalButtonType> buttonCallback, ModalButtonType @NotNull ... buttons) {
        this(modalPane, title, description, null, callbackAfterClose, buttonCallback, buttons);
    }

    public CompletableFuture<ModalButtonType> responseFuture() {
        return buttonTypeFuture;
    }

    public Optional<ModalButtonType> waitForResponse() {
        return Optional.ofNullable(buttonTypeFuture.join());
    }

    public void show() {
        if (super.modalPane != null) {
            Platform.runLater(() -> super.modalPane.show(this));
        }
    }

    public Optional<ModalButtonType> showAndWait() {
        if (super.modalPane != null) {
            show();
            return waitForResponse();
        }
        else
            return Optional.empty();
    }

    @Override
    protected void handleClose() {
        super.handleClose();
        buttonTypeFuture.complete(ModalButtonType.CLOSE);
    }

    private void onButtonAction(ModalButtonType buttonType) {
        buttonTypeFuture.complete(buttonType);

        if (!callbackAfterClose)
            super.close();

        if (buttonCallback != null)
            buttonCallback.accept(buttonType);

        if (callbackAfterClose)
            super.close();
    }

    protected void setContent(Node content) {
        alertCard.setBody(content);
    }

    public static AlertModalBox yesNoAlert(ModalPane modalPane, String title, String description, boolean callbackAfterClose, Consumer<ModalButtonType> buttonCallback) {
        return new AlertModalBox(modalPane, title, description, callbackAfterClose, buttonCallback, ModalButtonType.YES, ModalButtonType.NO);
    }
}
