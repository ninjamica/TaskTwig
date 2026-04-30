package ninjamica.tasktwig.ui;

import atlantafx.base.controls.Card;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Tweaks;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.Routine;
import ninjamica.tasktwig.core.RoutineInterval.DailyInterval;
import ninjamica.tasktwig.core.RoutineInterval.DayInterval;
import ninjamica.tasktwig.core.RoutineInterval.WeekInterval;
import ninjamica.tasktwig.ui.util.TimeInput;

import java.time.DayOfWeek;

public class RoutinePropertyPane extends VBox {
    private Label blankLabel;
    private Card nameCard;
    private Card intervalCard;
    private Card dayIntervalCard;
    private Card nextDueCard;
    private Card dayOfWeekCard;
    private Card dueTimeCard;

    private TextField nameTextField;
    private ChoiceBox<String> intervalChoiceBox;
    private Spinner<Integer> dayIntervalSpinner;
    private CheckBox dayIntervalRepeatCheckbox;
    private DatePicker nextDuePicker;
    private ToggleButton dayMButton;
    private ToggleButton dayTButton;
    private ToggleButton dayWButton;
    private ToggleButton dayThButton;
    private ToggleButton dayFButton;
    private ToggleButton daySaButton;
    private ToggleButton daySuButton;
    private TimeInput dueTimeInput;

    private final static ObservableList<String> types = FXCollections.observableArrayList("Daily", "Day Interval", "Week Interval");
    private Subscription subscriptions = Subscription.EMPTY;
    private Subscription typeSubs = Subscription.EMPTY;
    Routine routine;

    public RoutinePropertyPane() {
        initializeUI();
        updateType(null, null, false);
    }

    private void initializeUI() {
        blankLabel = new Label("Select a task to view\nits properties here");
        blankLabel.setTextAlignment(TextAlignment.CENTER);
        blankLabel.setAlignment(Pos.CENTER);
        blankLabel.setPrefWidth(230);

        nameCard = new Card();
        nameCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        nameCard.setHeader(new Label("Name:"));
        nameCard.setFocusTraversable(false);
        nameTextField = new TextField();
        nameTextField.setPromptText("Routine Name");
        nameTextField.setPrefWidth(230);
        nameCard.setBody(nameTextField);

        intervalCard = new Card();
        intervalCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        intervalCard.setHeader(new Label("Repeat Pattern:"));
        intervalCard.setFocusTraversable(false);
        intervalChoiceBox = new ChoiceBox<>(types);
        intervalCard.setBody(intervalChoiceBox);

        dayIntervalCard = new Card();
        dayIntervalCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        dayIntervalCard.setHeader(new Label("Repeat every:"));
        dayIntervalCard.setFocusTraversable(false);
        dayIntervalSpinner = new Spinner<>(1, Integer.MAX_VALUE, 1);
        dayIntervalSpinner.setEditable(true);
        dayIntervalSpinner.setPrefWidth(100);
        HBox daysBox = new HBox(5, dayIntervalSpinner, new Label("days"));
        daysBox.setAlignment(Pos.CENTER_LEFT);
        dayIntervalRepeatCheckbox = new CheckBox("Repeat from Last Completed");
        dayIntervalCard.setBody(new VBox(5, daysBox, dayIntervalRepeatCheckbox));

        nextDueCard = new Card();
        nextDueCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        nextDueCard.setHeader(new Label("Next Due:"));
        nextDueCard.setFocusTraversable(false);
        nextDuePicker = new DatePicker();
        nextDueCard.setBody(nextDuePicker);

        dayOfWeekCard = new Card();
        dayOfWeekCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        dayOfWeekCard.setHeader(new Label("Repeat on:"));
        dayOfWeekCard.setFocusTraversable(false);
        dayMButton = new ToggleButton("M");
        dayTButton = new ToggleButton("T");
        dayWButton = new ToggleButton("W");
        dayThButton = new ToggleButton("Th");
        dayFButton = new ToggleButton("F");
        daySaButton = new ToggleButton("Sa");
        daySuButton = new ToggleButton("Su");
        dayOfWeekCard.setBody(new InputGroup(dayMButton, dayTButton, dayWButton, dayThButton, dayFButton, daySaButton, daySuButton));

        dueTimeCard = new Card();
        dueTimeCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        dueTimeCard.setHeader(new Label("Due Time:"));
        dueTimeCard.setFocusTraversable(false);
        dueTimeInput = new TimeInput();
        dueTimeInput.setMaxWidth(110);
        dueTimeCard.setBody(dueTimeInput);
    }

    public void setRoutine(Routine routine) {
        this.routine = routine;
        subscriptions.unsubscribe();
        subscriptions = Subscription.EMPTY;

        if (routine != null) {
            nameTextField.textProperty().bindBidirectional(routine.name());
            dueTimeInput.timeValueProperty().bindBidirectional(routine.dueTime());
            subscriptions = Subscription.combine(
                    () -> nameTextField.textProperty().unbindBidirectional(routine.name()),
                    () -> dueTimeInput.timeValueProperty().unbindBidirectional(routine.dueTime())
            );

            String routineType = switch (routine.getInterval()) {
                case DailyInterval daily -> "Daily";
                case DayInterval day -> "Day Interval";
                case WeekInterval week -> "Week Interval";
            };
            intervalChoiceBox.setValue(routineType);
            subscriptions = intervalChoiceBox.getSelectionModel().selectedItemProperty().subscribe((oldItem, newItem) -> updateType(oldItem, newItem, true)).and(subscriptions);
            updateType(null,  routineType, false);
        }
        else {
            updateType(null, null, false);
        }
    }

    private void updateType(String oldValue, String newValue, boolean overrideInterval) {
        if (oldValue == null || !oldValue.equals(newValue)) {

            typeSubs.unsubscribe();
            typeSubs = Subscription.EMPTY;

            getChildren().clear();

            if (routine == null) {
                getChildren().add(blankLabel);
            }
            else {
                getChildren().addAll(nameCard, intervalCard, dueTimeCard);

                switch (newValue) {
                    case "Daily" -> {
                        if (overrideInterval)
                            routine.interval().set(new DailyInterval());
                    }
                    case "Day Interval" -> {
                        getChildren().add(2, dayIntervalCard);
                        getChildren().add(3, nextDueCard);

                        if (overrideInterval) {
                            routine.interval().set(new DayInterval(1, false));
                        }

                        DayInterval interval = (DayInterval) routine.getInterval();

                        dayIntervalSpinner.getValueFactory().setValue(interval.getInterval());
                        dayIntervalRepeatCheckbox.setSelected(interval.isRepeatFromLastDone());
                        nextDuePicker.setValue(interval.getNextDue());

                        typeSubs = Subscription.combine(
                                dayIntervalSpinner.valueProperty().subscribe(value -> interval.intervalProperty().set(value)),
                                dayIntervalRepeatCheckbox.selectedProperty().subscribe(value -> interval.repeatFromLastDoneProperty().set(value)),
                                nextDuePicker.valueProperty().subscribe(date -> {
                                    System.out.println("Updating next due to: " + date);
                                    if (date != null)
                                        interval.setNextDue(date);
                                })
                        );
                    }
                    case "Week Interval" -> {
                        getChildren().add(2, dayOfWeekCard);

                        if (overrideInterval) {
                            routine.interval().set(new WeekInterval());
                        }

                        WeekInterval interval = (WeekInterval) routine.getInterval();

                        dayMButton.setSelected(interval.isIntervalOn(DayOfWeek.MONDAY));
                        dayTButton.setSelected(interval.isIntervalOn(DayOfWeek.TUESDAY));
                        dayWButton.setSelected(interval.isIntervalOn(DayOfWeek.WEDNESDAY));
                        dayThButton.setSelected(interval.isIntervalOn(DayOfWeek.THURSDAY));
                        dayFButton.setSelected(interval.isIntervalOn(DayOfWeek.FRIDAY));
                        daySaButton.setSelected(interval.isIntervalOn(DayOfWeek.SATURDAY));
                        daySuButton.setSelected(interval.isIntervalOn(DayOfWeek.SUNDAY));

                        dayMButton.setOnAction(event -> interval.setOnDay(DayOfWeek.MONDAY, dayMButton.isSelected()));
                        dayTButton.setOnAction(event -> interval.setOnDay(DayOfWeek.TUESDAY, dayTButton.isSelected()));
                        dayWButton.setOnAction(event -> interval.setOnDay(DayOfWeek.WEDNESDAY, dayWButton.isSelected()));
                        dayThButton.setOnAction(event -> interval.setOnDay(DayOfWeek.THURSDAY, dayThButton.isSelected()));
                        dayFButton.setOnAction(event -> interval.setOnDay(DayOfWeek.FRIDAY, dayFButton.isSelected()));
                        daySaButton.setOnAction(event -> interval.setOnDay(DayOfWeek.SATURDAY, daySaButton.isSelected()));
                        daySuButton.setOnAction(event -> interval.setOnDay(DayOfWeek.SUNDAY, daySuButton.isSelected()));
                    }
                    default -> {}
                }
            }
        }
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        Platform.runLater(() -> nameTextField.requestFocus());
    }
}
