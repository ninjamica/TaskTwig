package ninjamica.tasktwig.core;

import atlantafx.base.theme.Styles;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIncludeProperties({"name", "interval", "dueTime", "priority", "children", "expanded"})
@JsonPropertyOrder({"name", "interval", "dueTime", "priority", "children", "expanded"})
public class Task {
    public static final int VERSION = 8;
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<TaskInterval> interval = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> dueTime = new SimpleObjectProperty<>();
    private final IntegerProperty priority = new SimpleIntegerProperty();

    @JsonInclude(value=JsonInclude.Include.NON_EMPTY)
    private final ObservableList<Task> children = FXCollections.observableArrayList();
    private boolean hasChildren = false;
    @JsonInclude(value=JsonInclude.Include.NON_DEFAULT)
    private final BooleanProperty expanded = new SimpleBooleanProperty();


    public Task(String name, TaskInterval interval, LocalTime dueTime, int priority, List<Task> children, boolean expanded) {
        this(name, interval, dueTime, priority);
        this.children.addAll(children);
        this.expanded.set(expanded);
    }

    public Task(String name, TaskInterval interval, LocalTime dueTime, int priority) {
        this.name.set(name);
        this.interval.set(interval);
        this.dueTime.set(dueTime);
        this.priority.set(priority);

        this.children.subscribe(() -> {
           if (children.isEmpty()) {
               expanded.set(false);
               hasChildren = false;
           }
           else {
               if (!hasChildren) {
                   expanded.set(true);
               }
               hasChildren = true;
           }
        });
    }

    public Task(String name, TaskInterval interval, int priority) {
        this(name, interval, null, priority);
    }

    public Task(JsonNode node, int version) {
        String name;
        TaskInterval interval;
        LocalTime dueTime = null;
        int priority = 0;
        List<Task> children = new ArrayList<>();
        boolean expanded = false;

        switch (version) {
            case 8:
                JsonNode expandedNode = node.get("expanded");
                if (expandedNode != null)
                    expanded = expandedNode.asBoolean();

            case 7:
                priority = node.get("priority").asInt();

            case 3, 4, 5, 6:
                name = node.get("name").asString();
                interval = TaskInterval.parseFromJson(node.get("interval"), version);

                JsonNode endNode = node.get("dueTime");
                if (!endNode.isNull())
                    dueTime = LocalTime.parse(endNode.asString());

                JsonNode childrenNode = node.get("children");
                if (childrenNode != null) {
                    for (JsonNode child : childrenNode) {
                        children.add(new Task(child, version));
                    }
                }
                break;

            default:
                throw new TaskTwig.TwigJsonVersionException("Unsupported Task version: " + version);
        }

        this(name, interval, dueTime, priority, children, expanded);
    }

    public StringProperty nameProperty() {
        return name;
    }

    @JsonGetter("name")
    public String getName() {
        return TaskTwig.callWithFXSafety(name::get);
    }

    public ObjectProperty<TaskInterval> intervalProperty() {
        return interval;
    }

    @JsonGetter("interval")
    public TaskInterval getInterval() {
        return TaskTwig.callWithFXSafety(interval::get);
    }

    public ObjectProperty<LocalTime> dueTimeProperty() {
        return dueTime;
    }

    @JsonGetter("dueTime")
    public LocalTime getDueTime() {
        return TaskTwig.callWithFXSafety(dueTime::get);
    }

    public IntegerProperty priorityProperty() {
        return priority;
    }

    @JsonGetter("priority")
    public int getPriority() {
        return TaskTwig.callWithFXSafety(priority::get);
    }

    public ObservableList<Task> getChildren() {
        return children;
    }

    @JsonGetter("children")
    public List<Task> getChildrenJson() {
        if (TaskTwig.notFxThread())
            return TaskTwig.callWithFXSafety(() -> new ArrayList<>(children));
        else
            return children;
    }

    public BooleanProperty expandedProperty() {
        return expanded;
    }

    @JsonGetter("expanded")
    public boolean isExpanded() {
        return TaskTwig.callWithFXSafety(expanded::get);
    }

    public ObservableValue<Boolean> doneObservable() {
        return intervalProperty().flatMap(TaskInterval::doneObservable);
    }

    public boolean isDone() {
        return interval.get().isDone();
    }

    public void setDone(boolean done) {
        interval.get().setDone(done);

        List<String> journalTasks = TaskTwig.instance().todaysJournal().completedTasks();
        if (done) {
            if (!journalTasks.contains(this.getName()))
                journalTasks.add(this.getName());
        } else {
            journalTasks.remove(this.getName());
        }
    }

    public void toggleDone() {
        setDone(!isDone());
    }

    public ObservableValue<LocalDate> nextDueObservable() {
        return intervalProperty().flatMap(TaskInterval::nextDueObservable);
    }

    public ObservableValue<Boolean> inProgressObservable() {
        return intervalProperty().flatMap(TaskInterval::inProgressObservable);
    }

    public boolean inProgress() {
        return getInterval().inProgress();
    }

    public static String priorityStyleClass(int priority) {
        return switch (priority) {
            case 1 -> Styles.ACCENT;
            case 2 -> Styles.SUCCESS;
            case 3 -> Styles.WARNING;
            case 4 -> Styles.DANGER;
            default -> Styles.TEXT_NORMAL;
        };
    }

    public static String[] priorityStyleClassList() {
        return new String[] {Styles.TEXT_NORMAL, Styles.ACCENT, Styles.SUCCESS, Styles.WARNING, Styles.DANGER, Styles.TEXT_MUTED};
    }

    public void hashContents(MessageDigest digest) {
        digest.update(getName().getBytes(StandardCharsets.UTF_8));
        getInterval().hashContents(digest);

        LocalTime dueTime = getDueTime();
        if (dueTime != null)
            digest.update(getDueTime().toString().getBytes(StandardCharsets.UTF_8));

        digest.update((byte) getPriority());

        for (Task task : children) {
            task.hashContents(digest);
        }

        digest.update((byte) (isExpanded() ? 1 : 0));
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Task) obj;
        return Objects.equals(this.name.get(), that.name.get()) &&
                Objects.equals(this.interval.get(), that.interval.get()) &&
                Objects.equals(this.dueTime.get(), that.dueTime.get()) &&
                Objects.equals(this.priority.get(), that.priority.get()) &&
                Objects.equals(this.children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, interval, dueTime);
    }

    @Override
    public String toString() {
        return "Task[" +
                "name=" + name + ", " +
                "interval=" + interval + ", " +
                "dueTime=" + dueTime + ", " +
                "priority=" + priority + ", " +
                "children=" + children + ']';
    }


}