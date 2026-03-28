package ninjamica.tasktwig;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

@JsonIncludeProperties({"name", "interval", "dueTime", "children"})
@JsonPropertyOrder({"name", "interval", "dueTime", "children"})
public class Task {
    public static final int VERSION = 6;
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<TaskInterval> interval = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> dueTime = new SimpleObjectProperty<>();

    @JsonInclude(value=JsonInclude.Include.NON_EMPTY)
    private final ObservableList<Task> children = FXCollections.observableArrayList();

    public Task(String name, TaskInterval interval, LocalTime dueTime, List<Task> children) {
        this(name, interval, dueTime);
        this.children.addAll(children);
    }

    public Task(String name, TaskInterval interval, LocalTime dueTime) {
        this.name.set(name);
        this.interval.set(interval);
        this.dueTime.set(dueTime);
    }

    public Task(String name, TaskInterval interval) {
        this(name, interval, null);
    }

    public Task(JsonNode node, int version) {

        switch (version) {
            case 3, 4, 5, 6 -> {
                name.set(node.get("name").asString());
                interval.set(TaskInterval.parseFromJson(node.get("interval"), version));

                JsonNode endNode = node.get("dueTime");
                if (!endNode.isNull())
                    dueTime.set(LocalTime.parse(endNode.asString()));

                JsonNode childrenNode = node.get("children");
                if (childrenNode != null) {
                    for (JsonNode child : childrenNode) {
                        children.add(new Task(child, version));
                    }
                }
            }
            default -> throw new TaskTwig.JsonVersionException("Unsupported Task version: " + version);
        }
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

    public void hashContents(MessageDigest digest) {
        digest.update(getName().getBytes(StandardCharsets.UTF_8));
        getInterval().hashContents(digest);

        LocalTime dueTime = getDueTime();
        if (dueTime != null)
            digest.update(getDueTime().toString().getBytes(StandardCharsets.UTF_8));

        for (Task task : children) {
            task.hashContents(digest);
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Task) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.interval, that.interval) &&
                Objects.equals(this.dueTime, that.dueTime);
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
                "children=" + children + ']';
    }


}