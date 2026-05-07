package ninjamica.tasktwig.core;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import javafx.beans.property.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@JsonIgnoreProperties({"name", "category", "occurrencePattern", "extend", "interval", "dueTime", "lastDone"})
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

    public TwigTask(String name, @NotNull TwigTask.OccurrencePattern occurrencePattern, @NotNull ExtendPattern extendPattern, @NotNull TwigInterval interval, @Nullable LocalTime dueTime) {
        this.name.set(name);
        this.occurrencePattern.set(occurrencePattern);
        this.extendPattern.set(extendPattern);
        this.interval.set(interval);
        this.dueTime.set(dueTime);

        this.occurrencePattern.subscribe(occurrence -> updateIntervalPatterns(occurrence, getExtendPattern()));
        this.extendPattern.subscribe(extend -> updateIntervalPatterns(getOccurrencePattern(), extend));
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

    public TwigTask(JsonNode node, int version) throws JsonParseException {
        name.set(node.required("name").asString());

        node.optional("category").ifPresent(category -> {
            this.category.set(TaskCategory.getCategoryFromName(category.asString()));
        });

        occurrencePattern.set(OccurrencePattern.valueOf(node.required("occurrencePattern").asString()));
        extendPattern.set(ExtendPattern.valueOf(node.required("extend").asString()));
        interval.set(TwigInterval.parseFromJson(node.required("interval"), version));

        node.optional("dueTime").ifPresent(dueTime -> {
            this.dueTime.set(LocalTime.parse(dueTime.toString()));
        });

        node.optional("lastDone").ifPresent(lastDone -> {
            this.lastDone.set(LocalDate.parse(lastDone.asString()));
        });
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
    private LocalDate getLastDone() {
        return TaskTwig.supplyWithFXSafety(lastDone::get);
    }
    private void setLastDone(@Nullable LocalDate lastDone) {
        TaskTwig.setWithFXSafety(this.lastDone::set, lastDone);
    }
}
