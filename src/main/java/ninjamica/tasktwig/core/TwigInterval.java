package ninjamica.tasktwig.core;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParseException;
import javafx.beans.Observable;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.*;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.JsonNode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TwigInterval.NoRepeat.class, name = "noRepeat"),
        @JsonSubTypes.Type(value = TwigInterval.DailyInterval.class, name = "daily"),
        @JsonSubTypes.Type(value = TwigInterval.PeriodInterval.class, name = "period"),
        @JsonSubTypes.Type(value = TwigInterval.WeekInterval.class, name = "week")
})
public sealed abstract class TwigInterval {

    private final ObjectProperty<LocalDate> reference = new SimpleObjectProperty<>();
    private final ObjectExpression<LocalDate> occurrence;

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
    }

    public static TwigInterval parseFromJson(JsonNode node, int version) throws JsonParseException {
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
            default -> throw new JsonParseException("unknown type in TwigInterval: " + type);
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
        return calcPrevOccurrence(getNextOccurrence());
    }

    /**
     * An `ObjectExpression` which evaluates to the next occurrence date of this interval occurring on or after the
     * current (effective) date. If the interval currently doesn't have a next occurrence, this is evaluated as `null`.
     * @return `ObjectExpression` evaluating to the next occurrence date
     */
    public ObjectExpression<LocalDate> occurrenceObservable() {
        return occurrence;
    }

    protected abstract LocalDate calcOccurrence();
    protected abstract LocalDate calcPrevOccurrence(LocalDate occurrenceDate);

    @JsonGetter("reference")
    protected LocalDate getReference() {
        return TaskTwig.supplyWithFXSafety(reference::get);
    }
    protected void setReference(LocalDate lastOccurred) {
        TaskTwig.setWithFXSafety(reference::set, lastOccurred);
    }



    public enum RepeatPattern {
        FROM_REF,
        REPEAT_ON_BEFORE,
        REPEAT_ON_AFTER
    }

    static abstract non-sealed class ConfigRepeatInterval extends TwigInterval {
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
     * A `TwigInterval` which represents a single, non-repeating occurrence.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonIncludeProperties({"reference"})
    public static final class NoRepeat extends TwigInterval {

        public NoRepeat(LocalDate eventDate) {
            super(eventDate, true);
        }

        @Override
        protected LocalDate calcOccurrence() {
            return getReference();
        }

        @Override
        protected LocalDate calcPrevOccurrence(LocalDate occurrenceDate) {
            return null;
        }

        public void setDate(LocalDate date) {
            setReference(date);
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
    }

    /**
     * A `TwigInterval` that represents an interval repeating on a constant period (represented by a `java.time.Period`
     * object)
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonIncludeProperties({"period, reference", "repeat"})
    public static final class PeriodInterval extends ConfigRepeatInterval {
        private final ObjectProperty<Period> period;

        public PeriodInterval(Period period, RepeatPattern repeatPattern, boolean autoRepeat, LocalDate reference) {
            var periodProp = new SimpleObjectProperty<>(period);
            super(repeatPattern, autoRepeat, true, reference, true, periodProp, TaskTwig.todayObservable());
            this.period = periodProp;
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
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonIncludeProperties({"map", "interval", "reference", "repeat"})
    public static final class WeekInterval extends ConfigRepeatInterval {
        private final ReadOnlyObjectWrapper<Byte> dayOfWeekMap;
        private final IntegerProperty weekInterval;

        public WeekInterval(int weekInterval, RepeatPattern repeatPattern, boolean autoRepeat, DayOfWeek... days) {
            this(weekInterval, parseDaysOfWeek(days), repeatPattern, autoRepeat, null);
        }

        public WeekInterval(int weekInterval, byte dayMap, RepeatPattern repeatPattern, boolean autoRepeat, LocalDate reference) {
            if (weekInterval < 1)
                throw new IllegalArgumentException("weekInterval must be >= 1");

            if (dayMap <= 0)
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
            RepeatPattern repeat = getRepeatPattern();
            LocalDate reference = getReference();

            switch (repeat) {
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
    }
}