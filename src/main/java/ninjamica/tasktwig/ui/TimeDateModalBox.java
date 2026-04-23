package ninjamica.tasktwig.ui;

import atlantafx.base.controls.Card;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Tile;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Tweaks;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Consumer;

public class TimeDateModalBox extends ModalBox {

    private final TimeInput timeInput;
    private final DatePicker datePicker;
    private final Consumer<LocalDateTime> onSubmit;

    public TimeDateModalBox(ModalPane modalPane, String title, String description, Consumer<LocalDateTime> onSubmit) {
        super(modalPane);
        this.onSubmit = onSubmit;

        timeInput = new TimeInput(LocalTime.now(), false);
        timeInput.setPrefWidth(150);
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(150);

        Button confirmButton = new Button("Confirm");
        confirmButton.setDefaultButton(true);
        confirmButton.setPrefWidth(150);
        confirmButton.setOnAction(this::onOkayAction);

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(event -> close());
        cancelButton.setPrefWidth(150);

        Card contentCard = new Card();
        contentCard.setHeader(new Tile(title, description));
        contentCard.setFooter(new HBox(10, confirmButton, cancelButton));
        contentCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);

        GridPane contentPane = new GridPane(5,15);
        contentPane.add(new Label("Time"), 0, 0);
        contentPane.add(timeInput, 1, 0);
        contentPane.add(new Label("Date"), 0, 1);
        contentPane.add(datePicker, 1, 1);
//        GridPane.setHgrow(datePicker, Priority.ALWAYS);
        contentPane.setAlignment(Pos.CENTER);
        contentPane.setPadding(new Insets(0, 0, 10, 0));
        contentCard.setBody(contentPane);

        addContent(contentCard);
        contentCard.setPrefWidth(320);
        AnchorPane.setTopAnchor(contentCard, 0.0);
        AnchorPane.setLeftAnchor(contentCard, 10.0);
        AnchorPane.setRightAnchor(contentCard, 10.0);
        AnchorPane.setBottomAnchor(contentCard, 0.0);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    }

    private void onOkayAction(ActionEvent event) {
        onSubmit.accept(timeInput.getTime().atDate(datePicker.getValue()));
        this.close();
    }
}
