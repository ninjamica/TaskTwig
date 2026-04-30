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
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AlertModalBox extends ModalBox {

    private final Card alertCard = new Card();
    protected Consumer<ButtonType> buttonCallback;
    private final boolean callbackAfterClose;
    private final CompletableFuture<ButtonType> buttonTypeFuture = new CompletableFuture<>();

    protected AlertModalBox(@NotNull ModalPane modalPane, String title, String description, boolean callbackAfterClose, ButtonType @NotNull ... buttons) {
        super(modalPane);
        this.callbackAfterClose = callbackAfterClose;

        ButtonBar buttonBar = new ButtonBar();
        for (ButtonType buttonType : buttons) {
            Button button = new Button(buttonType.getText());
            button.setOnAction(event -> onButtonAction(buttonType));
            button.setDefaultButton(buttonType.getButtonData().isDefaultButton());
            ButtonBar.setButtonData(button, buttonType.getButtonData());
            buttonBar.getButtons().add(button);
        }

        alertCard.setHeader(new Tile(title, description));
        alertCard.setFooter(buttonBar);
        alertCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);

        addContent(alertCard);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    }

    public AlertModalBox(@NotNull ModalPane modalPane, String title, String description, @Nullable Node content, boolean callbackAfterClose, Consumer<ButtonType> buttonCallback, ButtonType @NotNull ... buttons) {
        this(modalPane, title, description, callbackAfterClose, buttons);
        this.buttonCallback = buttonCallback;

        if (content != null)
            alertCard.setBody(content);
    }

    public AlertModalBox(@NotNull ModalPane modalPane, String title, String description, boolean callbackAfterClose, Consumer<ButtonType> buttonCallback, ButtonType @NotNull ... buttons) {
        this(modalPane, title, description, null, callbackAfterClose, buttonCallback, buttons);
    }

    public CompletableFuture<ButtonType> responseFuture() {
        return buttonTypeFuture;
    }

//    public Optional<ButtonType> waitForResponse() {
//        return buttonTypeFuture.join();
//    }

    public void show() {
        if (super.modalPane != null) {
            Platform.runLater(() -> super.modalPane.show(this));
        }
    }

    public Optional<ButtonType> showAndWait() {
        if (super.modalPane != null) {
            show();
            return Optional.ofNullable(buttonTypeFuture.join());
        }
        else
            return Optional.empty();
    }

    @Override
    public void close() {
        super.close();
        buttonTypeFuture.complete(ButtonType.CLOSE);
    }

    private void onButtonAction(ButtonType buttonType) {
        if (!callbackAfterClose)
            super.close();

        buttonTypeFuture.complete(buttonType);
        if (buttonCallback != null)
            buttonCallback.accept(buttonType);

        if (callbackAfterClose)
            super.close();
    }

    protected void setContent(Node content) {
        alertCard.setBody(content);
    }
}
