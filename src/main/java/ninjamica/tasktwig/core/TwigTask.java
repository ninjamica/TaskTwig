package ninjamica.tasktwig.core;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.Objects;
import java.util.Optional;

@JsonIncludeProperties({"name", "category", "occurrencePattern", "extend", "interval", "dueTime", "lastDone"})
public class TwigTask {

    public enum OccurrencePattern {
        OCCUR_ON,
        DUE_BY,
        START_ON
    }

    public enum ExtendPattern {
        NO_EXTEND,
        ON_COMPLETION,
        FROM_COMPLETION,
        AUTO
    }

    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<TaskCategory> category = new SimpleObjectProperty<>();
    private final ObjectProperty<OccurrencePattern> occurrencePattern = new SimpleObjectProperty<>();
    private final ObjectProperty<ExtendPattern> extendPattern = new SimpleObjectProperty<>();
    private final ObjectProperty<TwigInterval> interval = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> dueTime = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> lastDone = new SimpleObjectProperty<>();

    public TwigTask(String name, TaskCategory category, @NotNull OccurrencePattern occurrencePattern, @NotNull ExtendPattern extendPattern, @NotNull TwigInterval interval, @Nullable LocalTime dueTime, LocalDate lastDone) {
        this.name.set(name);
        this.category.set(category);
        this.occurrencePattern.set(occurrencePattern);
        this.extendPattern.set(extendPattern);
        this.interval.set(interval);
        this.dueTime.set(dueTime);
        this.lastDone.set(lastDone);

        this.occurrencePattern.subscribe(occurrence -> updateIntervalPatterns(occurrence, getExtendPattern()));
        this.extendPattern.subscribe(extend -> updateIntervalPatterns(getOccurrencePattern(), extend));
    }

    public TwigTask(String name, TaskCategory category, @NotNull OccurrencePattern occurrencePattern, @NotNull ExtendPattern extendPattern, @NotNull TwigInterval interval, @Nullable LocalTime dueTime) {
        this(name, category, occurrencePattern, extendPattern, interval, dueTime, null);
    }

    public TwigTask(JsonNode node, int version) {
        String name;
        TaskCategory category = null;
        OccurrencePattern occurrencePattern;
        ExtendPattern extendPattern;
        TwigInterval interval;
        LocalTime dueTime = null;
        LocalDate lastDone = null;

        switch (version) {
            case 10 -> {
                name = node.required("name").asString();
                occurrencePattern = OccurrencePattern.valueOf(node.required("occurrencePattern").asString());
                extendPattern = ExtendPattern.valueOf(node.required("extend").asString());
                interval = TwigInterval.parseFromJson(node.required("interval"), version);

                Optional<JsonNode> catNode = node.optional("category");
                if (catNode.isPresent())
                    category = TaskCategory.getCategoryFromName(catNode.get().asString());

                Optional<JsonNode> dueTimeNode = node.optional("dueTime");
                if (dueTimeNode.isPresent())
                    dueTime = LocalTime.parse(dueTimeNode.get().asString());

                Optional<JsonNode> lastDoneNode = node.optional("lastDone");
                if (lastDoneNode.isPresent())
                    lastDone = LocalDate.parse(lastDoneNode.get().asString());
            }
            case 1,2,3,4,5,6,7,8,9 -> {
                if (node.has("priority")) {
                    Task task = new Task(node, version);
                    var taskInterval = task.getInterval();

                    name =  task.getName();
                    category = task.getCategory();
                    occurrencePattern = OccurrencePattern.DUE_BY;
                    dueTime = task.getDueTime();
                    lastDone = taskInterval.getLastDone();

                    switch (taskInterval) {
                        case TaskInterval.NoInterval noInterval -> {
                            extendPattern = ExtendPattern.AUTO;
                            interval = new TwigInterval.NoRepeat(TwigInterval.NoRepeat.NO_DATE);
                        }
                        case TaskInterval.SingleDateInterval singleDateInterval -> {
                            extendPattern = ExtendPattern.AUTO;
                            interval = new TwigInterval.NoRepeat(singleDateInterval.getDueDate());
                        }
                        case TaskInterval.DayInterval dayInterval -> {
                            if (dayInterval.isRepeatFromLastDone()) {
                                extendPattern = ExtendPattern.FROM_COMPLETION;
                                interval = new TwigInterval.PeriodInterval(
                                        Period.ofDays(dayInterval.getInterval()),
                                        TwigInterval.RepeatPattern.FROM_REF,
                                        false, lastDone
                                );
                            }
                            else {
                                extendPattern = ExtendPattern.AUTO;
                                interval = new TwigInterval.PeriodInterval(
                                        Period.ofDays(dayInterval.getInterval()),
                                        TwigInterval.RepeatPattern.REPEAT_ON_AFTER,
                                        true, lastDone
                                );
                            }
                        }
                        case TaskInterval.WeekInterval weekInterval -> {
                            extendPattern = ExtendPattern.AUTO;
                            interval = new TwigInterval.WeekInterval(
                                    1, weekInterval.getDayOfWeekBitmap(),
                                    TwigInterval.RepeatPattern.REPEAT_ON_AFTER,
                                    true, lastDone
                            );
                        }
                        case TaskInterval.MonthInterval monthInterval -> {
                            extendPattern = ExtendPattern.AUTO;
                            interval = new TwigInterval.MonthInterval(
                                    1, TwigInterval.RepeatPattern.REPEAT_ON_AFTER,
                                    true, lastDone, monthInterval.getDates().toArray(Integer[]::new)
                            );
                        }
                    }
                }
                else {
                    if (version > 3)
                        throw new TaskTwig.TwigJsonVersionException("Invalid version for Routine upgrading to TwigTask: " + version);

                    Routine routine = new Routine(node, version);

                    name = routine.getName();
                    occurrencePattern = OccurrencePattern.OCCUR_ON;
                    extendPattern = ExtendPattern.AUTO;
                    dueTime = routine.getDueTime();

                    switch (routine.getInterval()) {
                        case RoutineInterval.DailyInterval dailyInterval -> {
                            lastDone = dailyInterval.getLastDone();
                            interval = new TwigInterval.DailyInterval();
                        }
                        case RoutineInterval.DayInterval dayInterval -> {
                            lastDone = dayInterval.getLastDone();
                            interval = new TwigInterval.PeriodInterval(
                                    Period.ofDays(dayInterval.getInterval()),
                                    TwigInterval.RepeatPattern.REPEAT_ON_AFTER,
                                    true, lastDone
                            );
                        }
                        case RoutineInterval.WeekInterval weekInterval -> {
                            lastDone = weekInterval.getLastDone();
                            interval = new TwigInterval.WeekInterval(
                                    1, weekInterval.getBitmap(),
                                    TwigInterval.RepeatPattern.REPEAT_ON_AFTER, true, lastDone
                            );
                        }
                    }
                }
            }
            default -> throw new TaskTwig.TwigJsonVersionException("Invalid version for TwigTask: " + version);
        }

        this(name, category, occurrencePattern, extendPattern, interval, dueTime, lastDone);
    }

    private void updateIntervalPatterns(OccurrencePattern occurrence, ExtendPattern extend) {
        if (getInterval() instanceof TwigInterval.ConfigRepeatInterval repeatInterval) {

            repeatInterval.setAutoRepeat(extend == ExtendPattern.AUTO);

            if (Objects.requireNonNull(extend) == ExtendPattern.NO_EXTEND) {
                repeatInterval.setRepeatPattern(TwigInterval.RepeatPattern.FROM_REF);
            }
            else {
                repeatInterval.setRepeatPattern(
                        switch (occurrence) {
                            case OCCUR_ON, DUE_BY -> TwigInterval.RepeatPattern.REPEAT_ON_AFTER;
                            case START_ON -> TwigInterval.RepeatPattern.REPEAT_ON_BEFORE;
                        }
                );
            }
        }
    }

    public boolean isToday() {
        LocalDate today = TaskTwig.today();

        if (today.equals(getLastDone()))
            return true;

        LocalDate next = getInterval().getNextOccurrence();
        if (next == null)
            return false;

        switch (getOccurrencePattern()) {
            case OCCUR_ON -> {
                return today.equals(next);
            }
            case DUE_BY -> {
                return !today.isAfter(next);
            }
            case START_ON -> {
                return today.isAfter(next);
            }
            case null -> throw new IllegalStateException("Illegal occurrence pattern (likely null)");
        }
    }

    public boolean isDone() {
        LocalDate lastDone = getLastDone();

        if (lastDone == null)
            return false;

        if (lastDone.equals(TaskTwig.today()))
            return true;

        LocalDate prevOccurrence = getInterval().getPrevOccurrence();
        if (prevOccurrence == null)
            return true;

        return lastDone.isAfter(prevOccurrence);
    }

    public void setDone(boolean done) {
        LocalDate today = TaskTwig.today();
        setLastDone(done ? today : null);

        TwigInterval interval = getInterval();
        if (done && interval instanceof TwigInterval.ConfigRepeatInterval repeatInterval) {
            switch (getExtendPattern()) {
                case NO_EXTEND, ON_COMPLETION -> repeatInterval.startFromNext();
                case FROM_COMPLETION -> repeatInterval.startFrom(today);
            }
        }
    }

    public StringProperty nameProperty() {
        return name;
    }
    public ObjectProperty<TaskCategory> categoryProperty() {
        return category;
    }
    public ObjectProperty<OccurrencePattern> occurrencePatternProperty() {
        return occurrencePattern;
    }
    public ObjectProperty<ExtendPattern> extendPatternProperty() {
        return extendPattern;
    }
    public ObjectProperty<TwigInterval> intervalProperty() {
        return interval;
    }
    public ObjectProperty<LocalTime> dueTimeProperty() {
        return dueTime;
    }

    @JsonGetter("name")
    public String getName() {
        return TaskTwig.supplyWithFXSafety(name::get);
    }

    @JsonGetter("category")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public TaskCategory getCategory() {
        return TaskTwig.supplyWithFXSafety(category::get);
    }

    @JsonGetter("occurrencePattern")
    public OccurrencePattern getOccurrencePattern() {
        return TaskTwig.supplyWithFXSafety(occurrencePattern::get);
    }

    @JsonGetter("extend")
    public ExtendPattern getExtendPattern() {
        return TaskTwig.supplyWithFXSafety(extendPattern::get);
    }

    @JsonGetter("interval")
    public TwigInterval getInterval() {
        return TaskTwig.supplyWithFXSafety(interval::get);
    }

    @JsonGetter("dueTime")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public LocalTime getDueTime() {
        return TaskTwig.supplyWithFXSafety(dueTime::get);
    }

    public void setName(String name) {
        TaskTwig.setWithFXSafety(this.name::set, name);
    }
    public void setCategory(@Nullable TaskCategory category) {
        TaskTwig.setWithFXSafety(this.category::set, category);
    }
    public void setOccurrencePattern(@NotNull TwigTask.OccurrencePattern occurrencePattern) {
        TaskTwig.setWithFXSafety(this.occurrencePattern::set, occurrencePattern);
    }
    public void setExtendPattern(@NotNull ExtendPattern extendPattern) {
        TaskTwig.setWithFXSafety(this.extendPattern::set, extendPattern);
    }
    public void setInterval(@NotNull TwigInterval interval) {
        TaskTwig.setWithFXSafety(this.interval::set, interval);
    }
    public void setDueTime(@Nullable LocalTime dueTime) {
        TaskTwig.setWithFXSafety(this.dueTime::set, dueTime);
    }

    @JsonGetter("lastDone")
    public LocalDate getLastDone() {
        return TaskTwig.supplyWithFXSafety(lastDone::get);
    }
    private void setLastDone(@Nullable LocalDate lastDone) {
        TaskTwig.setWithFXSafety(this.lastDone::set, lastDone);
    }
}
