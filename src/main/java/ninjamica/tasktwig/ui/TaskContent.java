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
import ninjamica.tasktwig.Task;
import ninjamica.tasktwig.TaskInterval.*;
import ninjamica.tasktwig.TaskTwig;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class TaskContent extends VBox {
    private Label blankLabel;
    private Card nameCard;
    private Card priorityCard;
    private Card repeatCard;
    private Card dueDateCard;
    private Card dayIntervalCard;
    private Card dayOfWeekCard;
    private Card dateOfMonthCard;
    private Card nextDueCard;
    private Card dueTimeCard;

    private TextField nameTextField;
    private Spinner<Integer> prioritySpinner;
    private ChoiceBox<String> repeatChoiceBox;
    private DatePicker dueDatePicker;
    private Spinner<Integer> dayIntervalSpinner;
    private DatePicker nextDuePicker;
    private CheckBox dayIntervalRepeatCheckbox;
    private ToggleButton dayMButton;
    private ToggleButton dayTButton;
    private ToggleButton dayWButton;
    private ToggleButton dayThButton;
    private ToggleButton dayFButton;
    private ToggleButton daySaButton;
    private ToggleButton daySuButton;
    private TextField dateOfMonthField;
    private TimeInput dueTimeInput;

    private final static ObservableList<String> types = FXCollections.observableArrayList("No Due Date", "Single Date", "Day Interval", "Week Interval", "Month Interval");
    private Subscription subscription = Subscription.EMPTY;
    private Subscription typeSubs =  Subscription.EMPTY;
    Task task;

    public TaskContent() {
//        try {
//            FXMLLoader loader = new FXMLLoader();
//            loader.setLocation(getClass().getResource("fxml/task-dialog.fxml"));
//            loader.setController(this);
//            getChildren().add(loader.load());
//        }
//        catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        initializeUI();
        updateType(null, null, false);
    }

    public TaskContent(Task task) {
        this();
        setTask(task);
    }

    private void initializeUI() {
//        setFillWidth(false);

        blankLabel = new Label("Select a task to view\nits properties here");
        blankLabel.setTextAlignment(TextAlignment.CENTER);
        blankLabel.setAlignment(Pos.CENTER);
        blankLabel.setPrefWidth(230);

        nameCard = new Card();
        nameCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        nameCard.setHeader(new Label("Name:"));
        nameCard.setFocusTraversable(false);
        nameTextField = new TextField();
        nameTextField.setPromptText("Task Name");
        nameTextField.setPrefWidth(230);
        nameCard.setBody(nameTextField);

        priorityCard = new Card();
        priorityCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        priorityCard.setHeader(new Label("Priority:"));
        priorityCard.setFocusTraversable(false);
        prioritySpinner = new Spinner<>(0, 4, 0);
        prioritySpinner.setEditable(true);
        prioritySpinner.setPrefWidth(80);
        priorityCard.setBody(prioritySpinner);

        repeatCard = new Card();
        repeatCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        repeatCard.setHeader(new Label("Repeat Pattern:"));
        repeatCard.setFocusTraversable(false);
        repeatChoiceBox = new ChoiceBox<>(types);
        repeatCard.setBody(repeatChoiceBox);

        dueDateCard = new Card();
        dueDateCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        dueDateCard.setHeader(new Label("Due Date:"));
        dueDateCard.setFocusTraversable(false);
        dueDatePicker = new DatePicker();
        dueDateCard.setBody(dueDatePicker);

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

        dateOfMonthCard = new Card();
        dateOfMonthCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        dateOfMonthCard.setHeader(new Label("Repeat every date of month:"));
        dateOfMonthCard.setFocusTraversable(false);
        dateOfMonthField = new TextField();
        dateOfMonthField.setPromptText("dates, comma separated");
        dateOfMonthCard.setBody(dateOfMonthField);

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

        dueTimeCard = new Card();
        dueTimeCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        dueTimeCard.setHeader(new Label("Due Time:"));
        dueTimeCard.setFocusTraversable(false);
        dueTimeInput = new TimeInput(null, true);
        dueTimeInput.setMaxWidth(110);
        dueTimeCard.setBody(dueTimeInput);
    }

    public void setTask(Task task) {
        this.task = task;
        subscription.unsubscribe();
        subscription = Subscription.EMPTY;

        if (task != null) {
            nameTextField.textProperty().bindBidirectional(task.nameProperty());
            var taskPriority = task.priorityProperty().asObject();
            prioritySpinner.getValueFactory().valueProperty().bindBidirectional(taskPriority);
            dueTimeInput.timeValueProperty().bindBidirectional(task.dueTimeProperty());
            subscription = Subscription.combine(
                    () -> nameTextField.textProperty().unbindBidirectional(task.nameProperty()),
                    () -> prioritySpinner.getValueFactory().valueProperty().unbindBidirectional(taskPriority),
                    () -> dueTimeInput.timeValueProperty().unbindBidirectional(task.dueTimeProperty())
            );

            String taskType = switch (task.getInterval()) {
                case NoInterval none -> "No Due Date";
                case SingleDateInterval single -> "Single Date";
                case DayInterval day -> "Day Interval";
                case WeekInterval week -> "Week Interval";
                case MonthInterval month -> "Month Interval";
                default -> null;
            };
            repeatChoiceBox.setValue(taskType);
            subscription = repeatChoiceBox.getSelectionModel().selectedItemProperty().subscribe((oldItem, newItem) -> updateType(oldItem, newItem, true)).and(subscription);
            updateType(null, taskType, false);

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

            if (task == null) {
                getChildren().add(blankLabel);
            }
            else {
                getChildren().addAll(nameCard, priorityCard, repeatCard);

                switch (newValue) {
                    case "No Due Date" -> {
                        if (overrideInterval) {
                            task.intervalProperty().set(new NoInterval());
                        }
                    }
                    case "Single Date" -> {
                        getChildren().addAll(dueDateCard, dueTimeCard);
                        if (overrideInterval) {
                            task.intervalProperty().set(new SingleDateInterval(TaskTwig.today()));
                        }

                        SingleDateInterval interval = (SingleDateInterval) task.getInterval();
                        dueDatePicker.setValue(interval.getDueDate());
                        typeSubs = dueDatePicker.valueProperty().subscribe(date -> interval.dueDateProperty().set(date));
                    }
                    case "Day Interval" -> {
                        getChildren().addAll(dayIntervalCard, nextDueCard, dueTimeCard);

                        if (overrideInterval) {
                            task.intervalProperty().set(new DayInterval(1, false));
                        }

                        DayInterval interval = (DayInterval) task.getInterval();

                        dayIntervalSpinner.getValueFactory().setValue(interval.getInterval());
                        nextDuePicker.setValue(interval.nextDue());
                        dayIntervalRepeatCheckbox.setSelected(interval.isRepeatFromLastDone());

                        typeSubs = dayIntervalSpinner.valueProperty().subscribe(value -> interval.intervalProperty().set(value)).and(typeSubs);
                        typeSubs = dayIntervalRepeatCheckbox.selectedProperty().subscribe(value -> interval.repeatFromLastDoneProperty().set(value)).and(typeSubs);
                        typeSubs = nextDuePicker.valueProperty().subscribe(date -> {
                            System.out.println("Updating next due to: " + date);
                            if (date != null)
                                interval.nextDueProperty().set(date);
                        }).and(typeSubs);
                    }
                    case "Week Interval" -> {
                        getChildren().addAll(dayOfWeekCard, dueTimeCard);

                        if (overrideInterval) {
                            task.intervalProperty().set(new WeekInterval());
                        }

                        WeekInterval interval = (WeekInterval) task.getInterval();

                        dayMButton.setSelected(interval.isDueOn(DayOfWeek.MONDAY));
                        dayTButton.setSelected(interval.isDueOn(DayOfWeek.TUESDAY));
                        dayWButton.setSelected(interval.isDueOn(DayOfWeek.WEDNESDAY));
                        dayThButton.setSelected(interval.isDueOn(DayOfWeek.THURSDAY));
                        dayFButton.setSelected(interval.isDueOn(DayOfWeek.FRIDAY));
                        daySaButton.setSelected(interval.isDueOn(DayOfWeek.SATURDAY));
                        daySuButton.setSelected(interval.isDueOn(DayOfWeek.SUNDAY));

                        dayMButton.setOnAction(event -> interval.setDueOn(DayOfWeek.MONDAY, dayMButton.isSelected()));
                        dayTButton.setOnAction(event -> interval.setDueOn(DayOfWeek.TUESDAY, dayTButton.isSelected()));
                        dayWButton.setOnAction(event -> interval.setDueOn(DayOfWeek.WEDNESDAY, dayWButton.isSelected()));
                        dayThButton.setOnAction(event -> interval.setDueOn(DayOfWeek.THURSDAY, dayThButton.isSelected()));
                        dayFButton.setOnAction(event -> interval.setDueOn(DayOfWeek.FRIDAY, dayFButton.isSelected()));
                        daySaButton.setOnAction(event -> interval.setDueOn(DayOfWeek.SATURDAY, daySaButton.isSelected()));
                        daySuButton.setOnAction(event -> interval.setDueOn(DayOfWeek.SUNDAY, daySuButton.isSelected()));
                    }
                    case "Month Interval" -> {
                        getChildren().addAll(dateOfMonthCard, dueTimeCard);

                        if (overrideInterval) {
                            task.intervalProperty().set(new MonthInterval());
                        }

                        MonthInterval interval = (MonthInterval) task.getInterval();
                        dateOfMonthField.setText(datesToString(interval.getDates()));
                        typeSubs = dateOfMonthField.textProperty().subscribe(dates -> interval.getDatesObservable().setAll(stringToDates(dates)));
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

    private String datesToString(List<Integer> dates) {
        if (dates.isEmpty()) {
            return "";
        }

        StringBuilder dateStr = new StringBuilder();

        for (Integer date : dates) {
            dateStr.append(date);
            dateStr.append(", ");
        }

        return dateStr.substring(0, dateStr.length() - 2);
    }

    private List<Integer> stringToDates(String datesStr) {
        if (datesStr == null || datesStr.isEmpty()) {
            return new ArrayList<>();
        }

        String[] inputText = datesStr.split(",");
        List<Integer> dates = new ArrayList<>();

        for (String date : inputText) {
            dates.add(Integer.parseInt(date.strip()));
        }

        return dates;
    }
}
