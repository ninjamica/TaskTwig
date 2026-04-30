package ninjamica.tasktwig.ui.util;

import atlantafx.base.controls.ModalPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Consumer;

public class TimeDateModalBox extends AlertModalBox {

    private static final ButtonType CONFIRM = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);

    private final TimeInput timeInput;
    private final DatePicker datePicker;
    private final Consumer<LocalDateTime> onSubmit;

    public TimeDateModalBox(ModalPane modalPane, String title, String description, boolean submitAfterClose, Consumer<LocalDateTime> onSubmit) {
        super(modalPane, title, description, submitAfterClose, CONFIRM, ButtonType.CANCEL);
        this.onSubmit = onSubmit;

        timeInput = new TimeInput(LocalTime.now(), false);
        timeInput.setPrefWidth(150);
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(150);

        GridPane contentPane = new GridPane(5,15);
        contentPane.add(new Label("Time"), 0, 0);
        contentPane.add(timeInput, 1, 0);
        contentPane.add(new Label("Date"), 0, 1);
        contentPane.add(datePicker, 1, 1);
        contentPane.setAlignment(Pos.CENTER);
        contentPane.setPadding(new Insets(0, 0, 10, 0));
        super.setContent(contentPane);

        super.buttonCallback = this::onSubmitAction;
    }

    private void onSubmitAction(ButtonType buttonType) {
        if (buttonType == CONFIRM) {
            onSubmit.accept(timeInput.getTime().atDate(datePicker.getValue()));
        }
    }
}
