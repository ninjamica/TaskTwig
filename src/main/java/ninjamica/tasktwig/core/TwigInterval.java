package ninjamica.tasktwig.core;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.util.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TwigInterval.NoRepeat.class, name = "noRepeat"),
        @JsonSubTypes.Type(value = TwigInterval.DailyInterval.class, name = "daily"),
        @JsonSubTypes.Type(value = TwigInterval.PeriodInterval.class, name = "period"),
        @JsonSubTypes.Type(value = TwigInterval.WeekInterval.class, name = "week"),
        @JsonSubTypes.Type(value = TwigInterval.MonthInterval.class, name = "month")
})
@JsonIncludeProperties({"reference"})
public sealed abstract class TwigInterval {

    private final ObjectProperty<LocalDate> reference = new SimpleObjectProperty<>();
    private final ObjectExpression<LocalDate> occurrence;
    private final ObjectExpression<LocalDate> prevOccurrence;

    protected TwigInterval(LocalDate reference, boolean bindReference, Observable... observables) {
        this(bindReference, observables);
        this.reference.set(reference);
    }

    protected TwigInterval(boolean bindReference, Observable... observables) {
        occurrence = new ObjectBinding<>() {
            {
                if (bindReference)
                    bind(reference);
                bind(observables);
            }

            @Override
            protected LocalDate computeValue() {
                return calcOccurrence();
            }
        };
        prevOccurrence = new ObjectBinding<>() {
            {
                if (bindReference)
                    bind(reference);
                bind(observables);
            }

            @Override
            protected LocalDate computeValue() {
                return calcPrevOccurrence(calcOccurrence());
            }
        };
    }

    static TwigInterval parseFromJson(JsonNode node, int version) throws TaskTwig.TwigJsonAssertException {
        String type = node.required("@type").asString();

        switch (type) {
            case "noRepeat" -> {
                return new NoRepeat(LocalDate.parse(node.required("reference").asString()));
            }
            case "daily" -> {
                return new DailyInterval();
            }
            case "period" -> {
                Period period = Period.parse(node.required("period").asString());
                RepeatPattern repeatTo = RepeatPattern.valueOf(node.required("repeatTo").asString());
                boolean autoRepeat = node.required("autoRepeat").asBoolean();

                JsonNode referenceNode = node.required("reference");
                LocalDate reference = referenceNode.isNull() ? null : LocalDate.parse(referenceNode.asString());

                return new PeriodInterval(period, repeatTo, autoRepeat, reference);
            }
            case "week" -> {
                byte map = (byte) node.required("map").asInt();
                int interval = node.required("interval").asInt();
                RepeatPattern repeatTo = RepeatPattern.valueOf(node.required("repeatTo").asString());
                boolean autoRepeat = node.required("autoRepeat").asBoolean();

                JsonNode referenceNode = node.required("reference");
                LocalDate reference = referenceNode.isNull() ? null : LocalDate.parse(referenceNode.asString());

                return new WeekInterval(interval, map, repeatTo, autoRepeat, reference);
            }
            case "month" -> {
                int interval = node.required("interval").asInt();
                RepeatPattern repeatTo = RepeatPattern.valueOf(node.required("repeatTo").asString());
                boolean autoRepeat = node.required("autoRepeat").asBoolean();

                Set<Integer> datesSet = new HashSet<>();
                for (JsonNode dateNode : node.required("dates").asArray()) {
                    datesSet.add(dateNode.asInt());
                }

                JsonNode referenceNode = node.required("reference");
                LocalDate reference = referenceNode.isNull() ? null : LocalDate.parse(referenceNode.asString());

                return new MonthInterval(interval, repeatTo, autoRepeat, reference, datesSet.toArray(Integer[]::new));
            }
            default -> throw new TaskTwig.TwigJsonAssertException("unknown type in TwigInterval: " + type);
        }
    }

    /**
     * Gets the next occurrence date of this interval. The value is `null` if there is not an occurrence after or on
     * the current (effective) date (for example a non-repeating interval that has already been completed).
     * @return next occurrence of interval or `null` if there is not one
     */
    public LocalDate getNextOccurrence() {
        return TaskTwig.supplyWithFXSafety(occurrence::get);
    }

    public LocalDate getPrevOccurrence() {
        return TaskTwig.supplyWithFXSafety(prevOccurrence::get);
    }

    /**
     * An `ObjectExpression` which evaluates to the next occurrence date of this interval occurring on or after the
     * current (effective) date. If the interval currently doesn't have a next occurrence, this is evaluated as `null`.
     * @return `ObjectExpression` evaluating to the next occurrence date
     */
    public ObjectExpression<LocalDate> occurrenceObservable() {
        return occurrence;
    }

    public ObjectExpression<LocalDate> prevOccurrenceObservable() {
        return prevOccurrence;
    }

    public void hashContents(MessageDigest digest) {
        digest.update(toString().getBytes(StandardCharsets.UTF_8));
    }

    protected abstract LocalDate calcOccurrence();
    protected abstract LocalDate calcPrevOccurrence(LocalDate occurrenceDate);

    @JsonGetter("reference")
    public LocalDate getReference() {
        return TaskTwig.supplyWithFXSafety(reference::get);
    }
    public void setReference(LocalDate lastOccurred) {
        TaskTwig.setWithFXSafety(reference::set, lastOccurred);
    }
    public ObjectProperty<LocalDate> referenceProperty() {
        return reference;
    }

    public abstract String toString();


    public enum RepeatPattern {
        FROM_REF,
        REPEAT_ON_BEFORE,
        REPEAT_ON_AFTER
    }

    @JsonIncludeProperties({"repeatTo", "autoRepeat"})
    public static abstract sealed class ConfigRepeatInterval extends TwigInterval {
        private final ObjectProperty<RepeatPattern> repeatPattern;
        private final BooleanProperty autoRepeat;

        protected ConfigRepeatInterval(boolean bindRepeat, boolean bindReference, Observable... observables) {
            var repeatProp = new SimpleObjectProperty<RepeatPattern>();
            var autoProp = new SimpleBooleanProperty();

            Observable[] newObservables;
            if (bindRepeat) {
                newObservables = Arrays.copyOf(observables, observables.length + 2);
                newObservables[observables.length] = repeatProp;
                newObservables[observables.length + 1] = autoProp;
            }
            else {
                newObservables = observables;
            }

            super(bindReference, newObservables);
            repeatPattern = repeatProp;
            autoRepeat = autoProp;
        }

        protected ConfigRepeatInterval(RepeatPattern repeatPattern, boolean autoRepeat, boolean bindRepeat,
                                       LocalDate reference, boolean bindReference, Observable... observables) {
            this(bindRepeat, bindReference, observables);
            this.repeatPattern.set(repeatPattern);
            this.autoRepeat.set(autoRepeat);
            setReference(reference);
        }

        public void startFromNext() {
            if (!getAutoRepeat()) {
                setReference(calcPrevOccurrence(calcNextOccurrenceAround(TaskTwig.today().plusDays(1))));
            }
        }

        /**
         * Sets the interval to start at a specific date such that the next occurrence happens after that date. A value
         * of `null` is ignored.
         * @param start the date to start interval from
         */
        public void startFrom(@NotNull LocalDate start) {
            setReference(start);
        }

        protected abstract LocalDate calcNextOccurrenceAround(LocalDate date);

        public ObjectProperty<RepeatPattern> repeatPatternProperty() {
            return repeatPattern;
        }
        public BooleanProperty autoRepeatProperty() {
            return autoRepeat;
        }

        @JsonGetter("repeatTo")
        public RepeatPattern getRepeatPattern() {
            return TaskTwig.supplyWithFXSafety(repeatPattern::get);
        }
        @JsonGetter("autoRepeat")
        public boolean getAutoRepeat() {
            return TaskTwig.supplyWithFXSafety(autoRepeat::get);
        }

        public void setRepeatPattern(RepeatPattern repeatPattern) {
            TaskTwig.setWithFXSafety(this.repeatPattern::set, repeatPattern);
        }
        public void setAutoRepeat(boolean autoRepeat) {
            TaskTwig.setWithFXSafety(this.autoRepeat::set, autoRepeat);
        }
    }


    /**
     * A `TwigInterval` which represents a single, non-repeating occurrence on a specified date.
     * Set this date to `NO_DATE` to represent there being no fixed end date (e.g. a task with no due date)
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonIncludeProperties({"reference"})
    public static final class NoRepeat extends TwigInterval {

        public static final LocalDate NO_DATE = LocalDate.MAX;

        public NoRepeat(LocalDate eventDate) {
            super(eventDate, true);
        }

        public NoRepeat() {
            this(NO_DATE);
        }

        @Override
        protected LocalDate calcOccurrence() {
            return getReference();
        }

        @Override
        protected LocalDate calcPrevOccurrence(LocalDate occurrenceDate) {
            return null;
        }

        public boolean hasNoDate() {
            return NO_DATE.equals(getReference());
        }

        public String toString() {
            return "NoRepeat[" + getReference() + "]";
        }
    }

    /**
     * A `TwigInterval` that represents a recurring, daily interval (occurs every day)
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonIncludeProperties({})
    public static final class DailyInterval extends TwigInterval {

        public DailyInterval() {
            super(false, TaskTwig.todayObservable());
            setReference(TaskTwig.today().minusDays(1));
        }

        @Override
        public LocalDate calcOccurrence() {
            return TaskTwig.today();
        }

        @Override
        protected LocalDate calcPrevOccurrence(LocalDate occurrenceDate) {
            return occurrenceDate.minusDays(1);
        }

        public String toString() {
            return "DailyInterval";
        }
    }

    /**
     * A `TwigInterval` that represents an interval repeating on a constant period (represented by a `java.time.Period`
     * object)
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonIncludeProperties({"period", "reference", "repeatTo", "autoRepeat"})
    public static final class PeriodInterval extends ConfigRepeatInterval {
        private final ObjectProperty<Period> period;

        public PeriodInterval(Period period, RepeatPattern repeatPattern, boolean autoRepeat, LocalDate reference) {
            var periodProp = new SimpleObjectProperty<>(period);
            super(repeatPattern, autoRepeat, true, reference, true, periodProp, TaskTwig.todayObservable());
            this.period = periodProp;
        }

        public PeriodInterval() {
            this(Period.ofDays(1), RepeatPattern.REPEAT_ON_AFTER, false, TaskTwig.today());
        }

        public PeriodInterval(TwigTask referenceTask) {
            this();
            TwigTask.setIntervalPatterns(this, referenceTask.getOccurrencePattern(), referenceTask.getExtendPattern());
        }

        @Override
        public LocalDate calcOccurrence() {
            if (getAutoRepeat())
                return calcNextOccurrenceAround(TaskTwig.today());
            else
                return getReference().plus(getPeriod());
        }

        @Override
        protected LocalDate calcPrevOccurrence(LocalDate occurrenceDate) {
            return occurrenceDate.minus(getPeriod());
        }

        @Override
        public LocalDate calcNextOccurrenceAround(LocalDate date) {
            Period period = getPeriod();
            LocalDate reference = getReference();

            if (reference == null)
                return null;

            switch (getRepeatPattern()) {
                case FROM_REF -> reference = reference.plus(period);
                case REPEAT_ON_BEFORE -> {
                    while (!reference.isAfter(date))
                        reference = reference.plus(period);

                    reference = reference.minus(period);
                }
                case REPEAT_ON_AFTER -> {
                    while (reference.isBefore(date))
                        reference = reference.plus(period);
                }
            }

            return reference;
        }

        public ObjectProperty<Period> periodProperty() {
            return period;
        }

        @JsonGetter("period")
        public Period getPeriod() {
            return TaskTwig.supplyWithFXSafety(period::get);
        }

        public void setPeriod(Period period) {
            TaskTwig.setWithFXSafety(this.period::set, period);
        }

        public String toString() {
            return "PeriodInterval[" +
                    "period=" + getPeriod() +
                    ", reference=" + getReference() +
                    ", repeatTo=" + getRepeatPattern() +
                    ", autoRepeat=" + getAutoRepeat() +
                    "]";
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonIncludeProperties({"map", "interval", "reference", "repeatTo", "autoRepeat"})
    public static final class WeekInterval extends ConfigRepeatInterval {
        private final ReadOnlyObjectWrapper<Byte> dayOfWeekMap;
        private final IntegerProperty weekInterval;

        public WeekInterval(TwigTask referenceTask) {
            this(1, RepeatPattern.REPEAT_ON_AFTER, false);
            TwigTask.setIntervalPatterns(this, referenceTask.getOccurrencePattern(), referenceTask.getExtendPattern());
        }

        public WeekInterval(int weekInterval, RepeatPattern repeatPattern, boolean autoRepeat, DayOfWeek... days) {
            this(weekInterval, parseDaysOfWeek(days), repeatPattern, autoRepeat, TaskTwig.today());
        }

        public WeekInterval(int weekInterval, byte dayMap, RepeatPattern repeatPattern, boolean autoRepeat, LocalDate reference) {
            if (weekInterval < 1)
                throw new IllegalArgumentException("weekInterval must be >= 1");

            if (dayMap < 0)
                throw new IllegalArgumentException(
                        "dayMap must be only have the lowest 7 bits set (highest bit cannot be set) and must have at least one bit set");

            var dayMapProp = new ReadOnlyObjectWrapper<>(dayMap);
            var intervalProp = new SimpleIntegerProperty(weekInterval);

            super(repeatPattern, autoRepeat, true, reference, true, intervalProp, dayMapProp, TaskTwig.todayObservable());

            dayOfWeekMap = dayMapProp;
            this.weekInterval = intervalProp;
        }

        @Override
        public LocalDate calcOccurrence() {
            if (getAutoRepeat())
                return calcNextOccurrenceAround(TaskTwig.today());
            else
                return getNextFromDate(getReference());
        }

        @Override
        protected LocalDate calcPrevOccurrence(LocalDate occurrenceDate) {
            int dayOfWeek = occurrenceDate.getDayOfWeek().ordinal();
            int prevDayOfWeek = getPreviousDayOfWeek(dayOfWeek);

            if (prevDayOfWeek == -1)
                return null;

            LocalDate prevDay = occurrenceDate.plusDays(prevDayOfWeek - dayOfWeek);
            if (prevDayOfWeek >= dayOfWeek)
                prevDay = prevDay.minusWeeks(getWeekInterval());

            return prevDay;
        }

        @Override
        protected LocalDate calcNextOccurrenceAround(LocalDate date) {
            LocalDate reference = getReference();

            if (reference == null)
                return null;

            switch (getRepeatPattern()) {
                case FROM_REF -> reference = getNextFromDate(reference);
                case REPEAT_ON_BEFORE -> {
                    while (reference != null && !reference.isAfter(date))
                        reference = getNextFromDate(reference);

                    if (reference != null)
                        reference = calcPrevOccurrence(reference);
                }
                case REPEAT_ON_AFTER -> {
                    while (reference != null && reference.isBefore(date))
                        reference = getNextFromDate(reference);
                }
            }
            return reference;
        }

        public IntegerProperty weekIntervalProperty() {
            return weekInterval;
        }

        public ReadOnlyObjectProperty<Byte> dayOfWeekMapProperty() {
            return dayOfWeekMap.getReadOnlyProperty();
        }

        @JsonGetter("map")
        public byte getDayOfWeekMap() {
            return TaskTwig.supplyWithFXSafety(dayOfWeekMap::get);
        }

        @JsonGetter("interval")
        public int getWeekInterval() {
            return TaskTwig.supplyWithFXSafety(weekInterval::get);
        }

        public void setDaysOfWeek(DayOfWeek... days) {
            setDayOfWeekMap(parseDaysOfWeek(days));
        }

        private void setDayOfWeekMap(byte map) {
            TaskTwig.setWithFXSafety(dayOfWeekMap::set, map);
        }

        public boolean isOnDay(DayOfWeek day) {
            return isOnDay(day.ordinal());
        }

        public void setOnDay(DayOfWeek day, boolean on) {
            if (on) {
                setDayOfWeekMap((byte) (getDayOfWeekMap() | (1 << day.ordinal())));
            }
            else {
                setDayOfWeekMap((byte) (getDayOfWeekMap() & ~(1 << day.ordinal())));
            }
        }

        private boolean isOnDay(int dayOrdinal) {
            return (dayOfWeekMap.get() & (1 << dayOrdinal)) != 0;
        }

        private int getNextDayOfWeek(int day) {
            for (int dayPlus = 1; dayPlus <= 7; dayPlus++) {
                int nextDay = (day + dayPlus) % 7;
                if (isOnDay(nextDay))
                    return nextDay;
            }
            return -1;
        }

        private int getPreviousDayOfWeek(int day) {
            for (int dayMinus = 1; dayMinus <= 7; dayMinus++) {
                int prevDay = (day - dayMinus) % 7;
                if (isOnDay(prevDay))
                    return prevDay;
            }
            return -1;
        }

        private LocalDate getNextFromDate(LocalDate date) {
            int dayOfWeek = date.getDayOfWeek().ordinal();
            int nextDayOfWeek = getNextDayOfWeek(dayOfWeek);

            if (nextDayOfWeek == -1)
                return null;

            LocalDate nextDay = date.plusDays(nextDayOfWeek - dayOfWeek);
            if (nextDayOfWeek <= dayOfWeek)
                nextDay = nextDay.plusWeeks(getWeekInterval());

            return nextDay;
        }

        private static byte parseDaysOfWeek(DayOfWeek... days) {
            byte bitmap = 0;
            for (DayOfWeek day : days) {
                bitmap |= (byte) (1 << day.ordinal());
            }
            return bitmap;
        }

        public String toString() {
            return "WeekInterval[" +
                    "map=" + getDayOfWeekMap() +
                    ", interval=" + getWeekInterval() +
                    ", reference=" + getReference() +
                    ", repeatTo=" + getRepeatPattern() +
                    ", autoRepeat=" + getAutoRepeat() +
                    "]";
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonIncludeProperties({"dates", "interval", "reference", "repeatTo", "autoRepeat"})
    public static final class MonthInterval extends ConfigRepeatInterval {

        private final ObservableSet<@Range(from = 1, to = 31) Integer> dates;
        private final IntegerProperty interval;

        public MonthInterval(int monthInterval, RepeatPattern repeatPattern, boolean autoRepeat, LocalDate reference, @Range(from = 1, to = 31) Integer... dates) {
            ObservableSet<Integer> datesProp = FXCollections.observableSet(new HashSet<>());
            IntegerProperty intervalProp = new SimpleIntegerProperty(monthInterval);

            super(repeatPattern, autoRepeat, true, reference, true, datesProp, intervalProp);
            this.dates = datesProp;
            this.interval = intervalProp;
            setDates(dates);
        }

        public MonthInterval(int monthInterval, RepeatPattern repeatPattern, boolean autoRepeat, @Range(from = 1, to = 31) Integer... dates) {
            this(monthInterval, repeatPattern, autoRepeat, TaskTwig.today(), dates);
        }

        public MonthInterval(TwigTask referenceTask) {
            this(1, RepeatPattern.REPEAT_ON_AFTER, false);
            TwigTask.setIntervalPatterns(this, referenceTask.getOccurrencePattern(), referenceTask.getExtendPattern());
        }

        @Override
        protected LocalDate calcOccurrence() {
            if (getAutoRepeat())
                return calcNextOccurrenceAround(TaskTwig.today());
            else
                return getNextFromDate(getReference());
        }

        @Override
        protected LocalDate calcPrevOccurrence(LocalDate date) {
            TreeSet<Integer> dates = TaskTwig.supplyWithFXSafety(() -> new TreeSet<>(this.dates));

            if (this.dates.isEmpty())
                return null;

            int refDate = date.getDayOfMonth();
            Integer prevDate = dates.lower(refDate);

            if (prevDate == null) {
                date = date.minusDays(getMonthInterval());
                prevDate = dates.last();
            }

            return date.withDayOfMonth(Math.min(prevDate, date.lengthOfMonth()));
        }

        @Override
        protected LocalDate calcNextOccurrenceAround(LocalDate date) {
            LocalDate reference = getReference();

            if (reference == null)
                return null;

            return switch (getRepeatPattern()) {
                case FROM_REF -> getNextFromDate(reference);
                case REPEAT_ON_BEFORE -> {
                    while (reference != null && !reference.isAfter(date))
                        reference = getNextFromDate(reference);

                    if (reference != null)
                        reference = calcPrevOccurrence(reference);

                    yield reference;
                }
                case REPEAT_ON_AFTER -> {
                    while (reference != null && reference.isBefore(date))
                        reference = getNextFromDate(reference);

                    yield reference;
                }
            };
        }

        private LocalDate getNextFromDate(LocalDate date) {
            TreeSet<Integer> dates = TaskTwig.supplyWithFXSafety(() -> new TreeSet<>(this.dates));

            if (this.dates.isEmpty())
                return null;

            int refDate = date.getDayOfMonth();
            Integer nextDate = dates.higher(refDate);

            if (nextDate == null || Math.min(nextDate, date.lengthOfMonth()) == refDate) {
                date = date.plusMonths(getMonthInterval());
                nextDate = dates.first();
            }

            return date.withDayOfMonth(Math.min(nextDate, date.lengthOfMonth()));
        }

        public IntegerProperty intervalProperty() {
            return interval;
        }
        public void addListener(SetChangeListener<Integer> listener) {
            dates.addListener(listener);
        }
        public void removeListener(SetChangeListener<Integer> listener) {
            dates.removeListener(listener);
        }
        public void addListener(InvalidationListener listener) {
            dates.addListener(listener);
        }
        public void removeListener(InvalidationListener listener) {
            dates.removeListener(listener);
        }
        public Subscription subscribe(Runnable invalidationSubscriber) {
            return dates.subscribe(invalidationSubscriber);
        }

        @JsonGetter("dates")
        public Set<Integer> getDates() {
            return TaskTwig.supplyWithFXSafety(() -> Set.copyOf(this.dates));
        }
        @JsonGetter("interval")
        public int getMonthInterval() {
            return TaskTwig.supplyWithFXSafety(interval::get);
        }

        public void setDates(@Range(from = 1, to = 31) Integer... dates) {
            TaskTwig.runWithFXSafety(() -> {
                this.dates.clear();
                for (int date : dates) {
                    if (date > 0 && date <= 31)
                        this.dates.add(date);
                }
            });
        }
        public void setDates(@Range(from = 1, to = 31) Set<Integer> dates) {
            TaskTwig.runWithFXSafety(() -> {
                this.dates.clear();
                for (int date : dates) {
                    if (date > 0 && date <= 31)
                        this.dates.add(date);
                }
            });
        }
        public void setMonthInterval(int monthInterval) {
            TaskTwig.setWithFXSafety(this.interval::set, monthInterval);
        }

        public String toString() {
            return "MonthInterval[" +
                    "dates=" + getDates() +
                    ", interval=" + getMonthInterval() +
                    ", reference=" + getReference() +
                    ", repeatTo=" + getRepeatPattern() +
                    ", autoRepeat=" + getAutoRepeat() +
                    "]";
        }
    }
}