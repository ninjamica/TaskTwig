package ninjamica.tasktwig.ui;

import atlantafx.base.controls.Card;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Tweaks;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.Exercise;
import ninjamica.tasktwig.core.TaskTwig;
import ninjamica.tasktwig.ui.util.CloseTableCell;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

public class WorkoutModal extends ModalBox {
    private final ChoiceBox<Exercise> exerciseChoiceBox;
    private final Spinner<Integer> exerciseCountSpinner;
    private final TableView<ExerciseHolder> exerciseTable;
    private final Consumer<SortedMap<Exercise, Integer>> onSubmit;

    public WorkoutModal(ModalPane modalPane, TaskTwig twig, Consumer<SortedMap<Exercise, Integer>> onSubmit) {
        super(modalPane);
        this.onSubmit = onSubmit;

        exerciseChoiceBox = new ChoiceBox<>(twig.exerciseList());
        exerciseCountSpinner = new Spinner<>(1, Integer.MAX_VALUE, 1);
        exerciseCountSpinner.setEditable(true);
        exerciseCountSpinner.setPrefWidth(100);
        Label exerciseUnitLabel = new Label();
        exerciseUnitLabel.setMinWidth(0);
        Button addExerciseButton = new Button("Add");
        addExerciseButton.setOnAction(this::addExercise);

        exerciseChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Exercise exercise) {
                if(exercise == null) return "";
                return exercise.name();
            }

            @Override
            public Exercise fromString(String string) {
                for (Exercise exercise : twig.getExerciseList()) {
                    if (exercise.name().equals(string)) {
                        return exercise;
                    }
                }
                return null;
            }
        });
        exerciseChoiceBox.setOnAction(event -> {
            exerciseUnitLabel.setText(exerciseChoiceBox.getValue().unit().displayName);
        });

        InputGroup addExerciseGroup = new InputGroup(exerciseChoiceBox, exerciseCountSpinner, exerciseUnitLabel, addExerciseButton);

        TableColumn<ExerciseHolder, String> nameCol = new TableColumn<>("Exercise");
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().exercise().name()));

        TableColumn<ExerciseHolder, Integer> countCol = new TableColumn<>("Count/Length");
        countCol.setCellValueFactory(cell -> cell.getValue().count());
        countCol.setCellFactory(col -> new TableCell<>() {
            private final Spinner<Integer> spinner = new Spinner<>(1, Integer.MAX_VALUE, 1);
            private Subscription sub = Subscription.EMPTY;
            {
                spinner.setPrefWidth(100);
            }

            @Override
            public void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                sub.unsubscribe();

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                }
                else {
                    ExerciseHolder exercise = getTableRow().getItem();
                    spinner.getValueFactory().valueProperty().bindBidirectional(exercise.count());
                    sub = () -> spinner.getValueFactory().valueProperty().unbindBidirectional(exercise.count());

                    setText(exercise.exercise().unit().displayName);
                    setGraphic(spinner);
                }
            }
        });

        TableColumn<ExerciseHolder, Void> closeCol = new TableColumn<>();
        closeCol.setCellFactory(column -> new CloseTableCell<>());
        closeCol.setMaxWidth(30);
        closeCol.setResizable(false);

        exerciseTable = new TableView<>();
        exerciseTable.getColumns().addAll(nameCol, countCol, closeCol);
        exerciseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Button submitButton = new Button("Submit");
        submitButton.setDefaultButton(true);
        submitButton.setOnAction(this::onSubmitAction);
        submitButton.setMaxWidth(Double.MAX_VALUE);

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(event -> super.close());
        cancelButton.setMaxWidth(Double.MAX_VALUE);

        Card contentCard = new Card();
        contentCard.setHeader(new Label("Select Exercises"));
        contentCard.setBody(new VBox(15, addExerciseGroup, exerciseTable));
        contentCard.setFooter(new HBox(10, submitButton, cancelButton));
        contentCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);

        addContent(contentCard);
//        contentCard.setPrefWidth(320);
        AnchorPane.setTopAnchor(contentCard, 0.0);
        AnchorPane.setLeftAnchor(contentCard, 10.0);
        AnchorPane.setRightAnchor(contentCard, 10.0);
        AnchorPane.setBottomAnchor(contentCard, 0.0);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    }

    private void addExercise(ActionEvent actionEvent) {
        Exercise exercise = exerciseChoiceBox.getValue();
        Integer count = exerciseCountSpinner.getValue();

        if (exercise != null && count != null) {
            exerciseTable.getItems().add(new ExerciseHolder(exercise, count));
        }
    }

    private void onSubmitAction(ActionEvent actionEvent) {
        SortedMap<Exercise, Integer> exercises = new TreeMap<>();
        for (ExerciseHolder exercise : exerciseTable.getItems()) {
            exercises.put(exercise.exercise, exercise.count().getValue());
        }
        onSubmit.accept(exercises);
        super.close();
    }

    private record ExerciseHolder(Exercise exercise, Property<Integer> count) {
        public ExerciseHolder(Exercise exercise, int count) {
            this(exercise, new SimpleObjectProperty<>(count));
        }
    }
}
