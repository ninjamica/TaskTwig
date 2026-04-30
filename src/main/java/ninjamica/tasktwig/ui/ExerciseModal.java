package ninjamica.tasktwig.ui;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ninjamica.tasktwig.core.Exercise;
import ninjamica.tasktwig.ui.util.CloseTableCell;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

public class ExerciseModal extends ModalBox {

    private final TextField nameTextField;
    private final ComboBox<Exercise.ExerciseUnit> unitChoiceBox;
    private final TableView<Exercise> exerciseTable;

    public ExerciseModal(ModalPane modalPane, ObservableList<Exercise> exercises) {
        super(modalPane);

        Label titleLabel = new Label("Edit Exercises");
        titleLabel.getStyleClass().add(Styles.TITLE_4);

        exerciseTable = new TableView<>();
        TableColumn<Exercise, String> nameCol = new TableColumn<>("Name");
        TableColumn<Exercise, String> unitCol = new TableColumn<>("Unit");
        TableColumn<Exercise, Void> closeCol = new TableColumn<>();

        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        unitCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().unit().name()));
        closeCol.setCellFactory(column -> new CloseTableCell<>());
        exerciseTable.getColumns().addAll(nameCol, unitCol, closeCol);
        exerciseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        closeCol.setMaxWidth(30);
        closeCol.setResizable(false);
        exerciseTable.setItems(exercises);

        nameTextField = new TextField();
        nameTextField.setPromptText("exercise name");
        HBox.setHgrow(nameTextField, Priority.ALWAYS);

        unitChoiceBox = new ComboBox<>(FXCollections.observableArrayList(Exercise.ExerciseUnit.values()));
        unitChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Exercise.ExerciseUnit unit) {
                if (unit == null) {
                    return null;
                }
                return unit.name();
            }

            @Override
            public Exercise.ExerciseUnit fromString(String s) {
                return Exercise.ExerciseUnit.valueOf(s);
            }
        });
        unitChoiceBox.setPromptText("choose unit");

        Button addButton = new Button(null, new FontIcon(FontAwesomeSolid.PLUS));
        addButton.setOnAction(this::createExercise);

        Button doneButton = new Button("Done");
        doneButton.setDefaultButton(true);
        doneButton.setMaxWidth(Double.MAX_VALUE);
        doneButton.setOnAction(event -> super.close());

        VBox contentPane = new VBox(
                10,
                titleLabel,
                new Separator(),
                exerciseTable,
                new Separator(),
                new Label("Create New Exercise"),
                new InputGroup(nameTextField, unitChoiceBox, addButton),
                new Separator(),
                doneButton
        );
        contentPane.setFillWidth(true);
        contentPane.setAlignment(Pos.TOP_CENTER);

        addContent(contentPane);
        contentPane.setPrefWidth(400);
        AnchorPane.setTopAnchor(contentPane, 10.0);
        AnchorPane.setLeftAnchor(contentPane, 20.0);
        AnchorPane.setRightAnchor(contentPane, 20.0);
        AnchorPane.setBottomAnchor(contentPane, 20.0);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    }

    private void createExercise(ActionEvent actionEvent) {
        String name = nameTextField.getText();
        Exercise.ExerciseUnit unit = unitChoiceBox.getSelectionModel().getSelectedItem();

        if (name == null || name.isBlank()) {
            nameTextField.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            nameTextField.setOnKeyPressed(keyEvent -> {
                nameTextField.pseudoClassStateChanged(Styles.STATE_DANGER, false);
                nameTextField.setOnKeyPressed(null);
            });
        }

        if (unit == null) {
            unitChoiceBox.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            unitChoiceBox.setOnAction(keyEvent -> {
                unitChoiceBox.pseudoClassStateChanged(Styles.STATE_DANGER, false);
                unitChoiceBox.setOnAction(null);
            });
        }

        if (name != null && !name.isBlank() && unit != null) {
            Exercise exercise = new Exercise(nameTextField.getText(), unitChoiceBox.getValue());
            exerciseTable.getItems().add(exercise);
        }
    }
}
