package ninjamica.tasktwig.ui;

import atlantafx.base.controls.Card;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Tweaks;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.Subscription;
import ninjamica.tasktwig.core.Task;
import ninjamica.tasktwig.core.TaskCategory;
import ninjamica.tasktwig.core.TaskTwig;
import ninjamica.tasktwig.core.TwigInterval;
import ninjamica.tasktwig.core.TwigInterval.*;
import ninjamica.tasktwig.ui.util.TimeInput;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

public class TaskPropertyPane extends VBox {
    private GridPane commonPropsPane;
    private TextField nameTextField;
    private Spinner<Integer> pointsSpinner;
    private ChoiceBox<TaskCategory> categoryChoiceBox;
    private ChoiceBox<Task.OccurrencePattern> occurrenceChoiceBox;
    private ChoiceBox<Task.ExtendPattern> extendChoiceBox;
    private ChoiceBox<IntervalType> intervalChoiceBox;

    private Card noDueDateCard;
    private ToggleSwitch noDueDateSwitch;

    private Card periodCard;
    private ChoiceBox<ChronoUnit> periodUnitChoiceBox;
    private Spinner<Integer> periodAmountSpinner;

    private Card dayOfWeekCard;
    private ToggleButton dayMButton;
    private ToggleButton dayTButton;
    private ToggleButton dayWButton;
    private ToggleButton dayThButton;
    private ToggleButton dayFButton;
    private ToggleButton daySaButton;
    private ToggleButton daySuButton;
    private Spinner<Integer> weekIntervalSpinner;

    private Card dateOfMonthCard;
    private TextField dateOfMonthField;
    private Spinner<Integer> monthIntervalSpinner;

    private Card referenceDateCard;
    private DatePicker referenceDatePicker;
    private Label nextDateLabel;

    private Card dueTimeCard;
    private TimeInput dueTimeInput;

    private Subscription subscriptions = Subscription.EMPTY;
    private Subscription typeSubs =  Subscription.EMPTY;
    Task task;

    private enum IntervalType {
        NO_REPEAT,
        DAILY,
        PERIOD,
        WEEKLY,
        MONTHLY;

        static IntervalType getType(TwigInterval interval) {
            return switch(interval) {
                case NoRepeat noRepeat -> NO_REPEAT;
                case DailyInterval dailyInterval -> DAILY;
                case PeriodInterval periodInterval -> PERIOD;
                case WeekInterval weekInterval -> WEEKLY;
                case MonthInterval monthInterval -> MONTHLY;
            };
        }
    }

    public TaskPropertyPane(TaskTwig twig) {
        initializeUI(twig);
    }

    public TaskPropertyPane(TaskTwig twig, Task task) {
        this(twig);
        setTask(task);
    }

    private void initializeUI(TaskTwig twig) {

        Card nameCard = new Card();
        nameCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        nameCard.setHeader(new Label("Name:"));
        nameCard.setFocusTraversable(false);
        nameTextField = new TextField();
        nameTextField.setPromptText("LegacyTask Name");
        nameTextField.setPrefWidth(230);
        nameCard.setBody(nameTextField);

        Card categoryCard = new Card();
        categoryCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        categoryCard.setHeader(new Label("Category:"));
        categoryCard.setFocusTraversable(false);
        categoryChoiceBox = new ChoiceBox<>(twig.getTaskCategoryList());
        categoryChoiceBox.setConverter(new CategoryChoiceBoxConverter());
        categoryCard.setBody(categoryChoiceBox);

        Card pointsCard = new Card();
        pointsCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        pointsCard.setHeader(new Label("Points:"));
        pointsCard.setFocusTraversable(false);
        pointsSpinner = new Spinner<>(0, Integer.MAX_VALUE, 1);
        pointsSpinner.setEditable(true);
        pointsSpinner.setPrefWidth(80);
        pointsCard.setBody(pointsSpinner);

        Card repeatCard = new Card();
        repeatCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        repeatCard.setHeader(new Label("Repeat Pattern:"));
        repeatCard.setFocusTraversable(false);
        occurrenceChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(Task.OccurrencePattern.values()));
        extendChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(Task.ExtendPattern.values()));
        intervalChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(IntervalType.values()));
        repeatCard.setBody(new InputGroup(occurrenceChoiceBox, intervalChoiceBox, extendChoiceBox));

        commonPropsPane = new GridPane();
        commonPropsPane.add(nameCard, 0, 0, 2, 1);
        commonPropsPane.add(pointsCard, 0, 1);
        commonPropsPane.add(categoryCard, 1, 1);
        commonPropsPane.add(repeatCard, 0, 2, 2, 1);


        noDueDateCard = new Card();
        noDueDateCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        noDueDateCard.setFocusTraversable(false);
        noDueDateSwitch = new ToggleSwitch("No Due Date");
        noDueDateCard.setBody(noDueDateSwitch);

        periodCard = new Card();
        periodCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        periodCard.setHeader(new Label("Repeat every:"));
        periodCard.setFocusTraversable(false);
        periodUnitChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(ChronoUnit.DAYS, ChronoUnit.WEEKS, ChronoUnit.MONTHS, ChronoUnit.YEARS));
        periodAmountSpinner = new Spinner<>(1, Integer.MAX_VALUE, 1);
        periodAmountSpinner.setEditable(true);
        periodAmountSpinner.setPrefWidth(100);
        periodCard.setBody(new InputGroup(periodAmountSpinner, periodUnitChoiceBox));

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
        weekIntervalSpinner = new Spinner<>(1, Integer.MAX_VALUE, 1);
        weekIntervalSpinner.setPrefWidth(80);
        dayOfWeekCard.setBody(new HBox(
                10,
                new InputGroup(dayMButton, dayTButton, dayWButton, dayThButton, dayFButton, daySaButton, daySuButton),
                new InputGroup(new Label("Every"), weekIntervalSpinner, new Label("week(s)"))
        ));

        dateOfMonthCard = new Card();
        dateOfMonthCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        dateOfMonthCard.setHeader(new Label("Repeat on date(s) of the month:"));
        dateOfMonthCard.setFocusTraversable(false);
        dateOfMonthField = new TextField();
        dateOfMonthField.setPromptText("dates, comma separated");
        monthIntervalSpinner = new Spinner<>(1, Integer.MAX_VALUE, 1);
        monthIntervalSpinner.setPrefWidth(80);
        dateOfMonthCard.setBody(new VBox(
                10,
                dateOfMonthField,
                new InputGroup(new Label("Every"), monthIntervalSpinner, new Label("month(s)"))
        ));

        referenceDateCard = new Card();
        referenceDateCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        referenceDateCard.setHeader(new Label("Reference Date:"));
        referenceDateCard.setFocusTraversable(false);
        referenceDatePicker = new DatePicker();
        nextDateLabel = new Label();
        HBox referenceBox = new HBox(20, referenceDatePicker, nextDateLabel);
        referenceBox.setAlignment(Pos.BASELINE_LEFT);
        referenceDateCard.setBody(referenceBox);

        dueTimeCard = new Card();
        dueTimeCard.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        dueTimeCard.setHeader(new Label("Due Time:"));
        dueTimeCard.setFocusTraversable(false);
        dueTimeInput = new TimeInput();
        dueTimeInput.setMaxWidth(110);
        dueTimeCard.setBody(dueTimeInput);
    }

    public void setTask(Task task) {
        this.task = task;
        subscriptions.unsubscribe();
        subscriptions = Subscription.EMPTY;

        if (task != null) {
            nameTextField.textProperty().bindBidirectional(task.nameProperty());

            categoryChoiceBox.setValue(task.getCategory());

            ObjectProperty<Integer> pointsProperty = task.pointsProperty().asObject();
            pointsSpinner.getValueFactory().valueProperty().bindBidirectional(pointsProperty);

            intervalChoiceBox.setValue(IntervalType.getType(task.getInterval()));
            occurrenceChoiceBox.valueProperty().bindBidirectional(task.occurrencePatternProperty());
            extendChoiceBox.valueProperty().bindBidirectional(task.extendPatternProperty());

            referenceDatePicker.valueProperty().bindBidirectional(task.getInterval().referenceProperty());
            updateNextDateText(task.getNextDate());

            dueTimeInput.timeValueProperty().bindBidirectional(task.dueTimeProperty());

            subscriptions = Subscription.combine(
                    () -> nameTextField.textProperty().unbindBidirectional(task.nameProperty()),
                    categoryChoiceBox.valueProperty().subscribe(task::setCategory),
                    () -> pointsSpinner.getValueFactory().valueProperty().unbindBidirectional(pointsProperty),
                    intervalChoiceBox.getSelectionModel().selectedItemProperty().subscribe(this::updateType),
                    () -> occurrenceChoiceBox.valueProperty().unbindBidirectional(task.occurrencePatternProperty()),
                    () -> extendChoiceBox.valueProperty().unbindBidirectional(task.extendPatternProperty()),
                    () -> referenceDatePicker.valueProperty().unbindBidirectional(task.getInterval().referenceProperty()),
                    task.nextDateObservable().subscribe(this::updateNextDateText),
                    () -> dueTimeInput.timeValueProperty().unbindBidirectional(task.dueTimeProperty())
            );

            updateType(null, intervalChoiceBox.getValue());
        }
        else {
            updateType(null, null);
        }
    }

    private void updateType(IntervalType oldValue, IntervalType newValue) {
        if (oldValue == null || oldValue != newValue) {
            typeSubs.unsubscribe();
            typeSubs = Subscription.EMPTY;

            getChildren().setAll(commonPropsPane);

            switch (newValue) {
                case NO_REPEAT -> {
                    getChildren().addAll(noDueDateCard, referenceDateCard, dueTimeCard);

                    if (!(task.getInterval() instanceof NoRepeat)) {
                        task.intervalProperty().set(new NoRepeat());
                    }

                    NoRepeat interval = ((NoRepeat) task.getInterval());

                    noDueDateSwitch.setSelected(interval.hasNoDate());
                    referenceDatePicker.disableProperty().bindBidirectional(noDueDateSwitch.selectedProperty());
                    dueTimeInput.disableProperty().bind(noDueDateSwitch.selectedProperty());

                    typeSubs = typeSubs.and(Subscription.combine(
                            noDueDateSwitch.selectedProperty().subscribe(noDueDate -> {
                                if (noDueDate) {
                                    interval.setReference(NoRepeat.NO_DATE);
                                    task.setDueTime(null);
                                }
                                else
                                    interval.setReference(TaskTwig.today());
                            }),
                            () -> referenceDatePicker.disableProperty().unbindBidirectional(noDueDateSwitch.selectedProperty()),
                            dueTimeInput.disableProperty()::unbind,
                            () -> referenceDatePicker.setDisable(false),
                            () -> dueTimeInput.setDisable(false),
                            () -> {
                                if (interval.hasNoDate()) {
                                    interval.setReference(TaskTwig.today());
                                }
                            }
                    ));
                }
                case DAILY -> {
                    getChildren().add(dueTimeCard);

                    if (!(task.getInterval() instanceof DailyInterval)) {
                        task.intervalProperty().set(new DailyInterval());
                    }
                }
                case PERIOD -> {
                    getChildren().addAll(periodCard, referenceDateCard, dueTimeCard);

                    if (!(task.getInterval() instanceof PeriodInterval)) {
                        task.intervalProperty().set(new PeriodInterval(task));
                    }

                    PeriodInterval interval = (PeriodInterval) task.getInterval();
                    Period period = interval.getPeriod();

                    if (period.getYears() > 0) {
                        periodUnitChoiceBox.setValue(ChronoUnit.YEARS);
                        periodAmountSpinner.getValueFactory().setValue(period.getYears());
                    }
                    else if (period.getMonths() > 0) {
                        periodUnitChoiceBox.setValue(ChronoUnit.MONTHS);
                        periodAmountSpinner.getValueFactory().setValue(period.getMonths());
                    }
                    else if (period.getDays() > 0) {
                        periodUnitChoiceBox.setValue(ChronoUnit.DAYS);
                        periodAmountSpinner.getValueFactory().setValue(period.getDays());
                    }

                    BiConsumer<ChronoUnit, Integer> periodCallback = (periodUnit, amount) -> {
                        switch (periodUnit) {
                            case YEARS -> interval.setPeriod(Period.ofYears(amount));
                            case MONTHS -> interval.setPeriod(Period.ofMonths(amount));
                            case DAYS -> interval.setPeriod(Period.ofDays(amount));
                        }
                    };

                    typeSubs = typeSubs.and(Subscription.combine(
                            periodUnitChoiceBox.valueProperty().subscribe(
                                    periodUnit -> periodCallback.accept(periodUnit, periodAmountSpinner.getValue())),
                            periodAmountSpinner.valueProperty().subscribe(
                                    amount -> periodCallback.accept(periodUnitChoiceBox.getValue(), amount))
                    ));
                }
                case WEEKLY -> {
                    getChildren().addAll(dayOfWeekCard, referenceDateCard, dueTimeCard);

                    if (!(task.getInterval() instanceof WeekInterval)) {
                        task.intervalProperty().set(new WeekInterval(task));
                    }

                    WeekInterval interval = (WeekInterval) task.getInterval();

                    var weekIntervalProp = interval.weekIntervalProperty().asObject();
                    weekIntervalSpinner.getValueFactory().valueProperty().bindBidirectional(weekIntervalProp);

                    dayMButton.setSelected(interval.isOnDay(DayOfWeek.MONDAY));
                    dayTButton.setSelected(interval.isOnDay(DayOfWeek.TUESDAY));
                    dayWButton.setSelected(interval.isOnDay(DayOfWeek.WEDNESDAY));
                    dayThButton.setSelected(interval.isOnDay(DayOfWeek.THURSDAY));
                    dayFButton.setSelected(interval.isOnDay(DayOfWeek.FRIDAY));
                    daySaButton.setSelected(interval.isOnDay(DayOfWeek.SATURDAY));
                    daySuButton.setSelected(interval.isOnDay(DayOfWeek.SUNDAY));

                    dayMButton.setOnAction(event -> interval.setOnDay(DayOfWeek.MONDAY, dayMButton.isSelected()));
                    dayTButton.setOnAction(event -> interval.setOnDay(DayOfWeek.TUESDAY, dayTButton.isSelected()));
                    dayWButton.setOnAction(event -> interval.setOnDay(DayOfWeek.WEDNESDAY, dayWButton.isSelected()));
                    dayThButton.setOnAction(event -> interval.setOnDay(DayOfWeek.THURSDAY, dayThButton.isSelected()));
                    dayFButton.setOnAction(event -> interval.setOnDay(DayOfWeek.FRIDAY, dayFButton.isSelected()));
                    daySaButton.setOnAction(event -> interval.setOnDay(DayOfWeek.SATURDAY, daySaButton.isSelected()));
                    daySuButton.setOnAction(event -> interval.setOnDay(DayOfWeek.SUNDAY, daySuButton.isSelected()));

                    typeSubs = typeSubs.and(Subscription.combine(
                            () -> weekIntervalSpinner.getValueFactory().valueProperty().unbindBidirectional(weekIntervalProp),
                            () -> dayMButton.setOnAction(null),
                            () -> dayTButton.setOnAction(null),
                            () -> dayWButton.setOnAction(null),
                            () -> dayThButton.setOnAction(null),
                            () -> dayFButton.setOnAction(null),
                            () -> daySaButton.setOnAction(null),
                            () -> daySuButton.setOnAction(null)
                    ));
                }
                case MONTHLY -> {
                    getChildren().addAll(dateOfMonthCard, referenceDateCard, dueTimeCard);

                    if (!(task.getInterval() instanceof MonthInterval)) {
                        task.intervalProperty().set(new MonthInterval(task));
                    }

                    MonthInterval interval = (MonthInterval) task.getInterval();

                    var intervalProp = interval.intervalProperty().asObject();
                    monthIntervalSpinner.getValueFactory().valueProperty().bindBidirectional(intervalProp);
                    dateOfMonthField.setText(datesToString(interval.getDates()));

                    typeSubs = Subscription.combine(
                            () -> monthIntervalSpinner.getValueFactory().valueProperty().unbindBidirectional(intervalProp),
                            dateOfMonthField.textProperty().subscribe(dates -> interval.setDates(stringToDates(dates)))
                    );
                }
                case null -> {}
            }
        }
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        Platform.runLater(() -> nameTextField.requestFocus());
    }

    private String datesToString(Set<Integer> dates) {
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

    private Set<Integer> stringToDates(String datesStr) {
        if (datesStr == null || datesStr.isEmpty()) {
            return new HashSet<>();
        }

        String[] inputText = datesStr.split(",");
        Set<Integer> dates = new HashSet<>();

        for (String date : inputText) {
            dates.add(Integer.parseInt(date.strip()));
        }

        return dates;
    }

    private void updateNextDateText(LocalDate nextDate) {
        nextDateLabel.setText(
                "Next date: " + Optional.ofNullable(nextDate).map(TaskTwigController.dateFormat::format).orElse("null") +
                "\nPrev date: " + Optional.ofNullable(task.getInterval().getPrevOccurrence()).map(TaskTwigController.dateFormat::format).orElse("null") +
                "\nLast done: " + Optional.ofNullable(task.getLastDone()).map(TaskTwigController.dateFormat::format).orElse("null")
        );
    }

    public static class CategoryChoiceBoxConverter extends StringConverter<TaskCategory> {
        @Override
        public String toString(TaskCategory object) {
            if (object == null)
                return "";
            else
                return object.getName();
        }

        @Override
        public TaskCategory fromString(String string) {
            return TaskCategory.getCategoryFromName(string);
        }
    }
}
