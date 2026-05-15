package ninjamica.tasktwig.core;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@JsonIncludeProperties({"name", "lastDone", "dueTime"})
@JsonPropertyOrder({"name", "lastDone", "dueTime"})
public class TwigSubTask implements TaskInterface {
    private final ObjectProperty<TwigTask> parentTask = new SimpleObjectProperty<>();
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> lastDone = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> dueTime = new SimpleObjectProperty<>();
    private final BooleanBinding isTodayBinding;
    private final BooleanBinding isDoneBinding;
    private final BooleanBinding isOverdueBinding;

    public TwigSubTask(String name, LocalDate lastDone, LocalTime dueTime, TwigTask parentTask) {
        this.name.set(name);
        this.lastDone.set(lastDone);
        this.dueTime.set(dueTime);
        this.parentTask.set(parentTask);

        this.isTodayBinding = Bindings.createBooleanBinding(
                () -> Optional.ofNullable(getParentTask()).map(TwigTask::isToday).orElse(false),
                this.parentTask.flatMap(TwigTask::isTodayObservable)
        );
        this.isDoneBinding = Bindings.createBooleanBinding(
                () -> Optional.ofNullable(getParentTask()).map(task -> task.isDone(getLastDone())).orElse(false),
                this.parentTask.flatMap(TwigTask::isDoneObservable), this.lastDone
        );
        this.isOverdueBinding = Bindings.createBooleanBinding(
                () -> Optional.ofNullable(getParentTask()).map(TwigTask::isOverdue).orElse(false),
                this.parentTask.flatMap(TwigTask::isOverdueObservable)
        );
    }

    public TwigSubTask(String name, LocalTime dueTime, TwigTask parentTask) {
        this(name, null, dueTime, parentTask);
    }

    public TwigSubTask(JsonNode node, int version) {
        String name = node.required("name").asString();
        LocalDate lastDone = node.optional("lastDone")
                .map(lastDoneNode -> LocalDate.parse(lastDoneNode.asString())).orElse(null);
        LocalTime dueTime = node.optional("dueTime")
                .map(dueTimeNode -> LocalTime.parse(dueTimeNode.asString())).orElse(null);

        this(name, lastDone, dueTime, null);
    }

    public boolean isDone() {
        return Optional.ofNullable(getParentTask()).map(task -> task.isDone(getLastDone())).orElse(false);
    }
    public boolean isToday() {
        return Optional.ofNullable(getParentTask()).map(TwigTask::isToday).orElse(false);
    }
    public boolean isOverdue() {
        return Optional.ofNullable(getParentTask()).map(task -> task.isOverdue(getLastDone())).orElse(false);
    }
    public void setDone(boolean done) {
        LocalDate today = TaskTwig.today();
        setLastDone(done ? today : null);
    }

    public BooleanExpression isTodayObservable() {
        return isTodayBinding;
    }
    public BooleanExpression isDoneObservable() {
        return isDoneBinding;
    }
    public BooleanExpression isOverdueObservable() {
        return isOverdueBinding;
    }
    public ObservableList<TwigSubTask> getSubTasks() {
        return FXCollections.emptyObservableList();
    }

    @JsonGetter("name")
    public String getName() {
        return TaskTwig.supplyWithFXSafety(name::get);
    }
    @JsonGetter("dueTime")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public LocalTime getDueTime() {
        return TaskTwig.supplyWithFXSafety(dueTime::get);
    }
    @JsonGetter("lastDone")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public LocalDate getLastDone() {
        return TaskTwig.supplyWithFXSafety(lastDone::get);
    }
    public TwigTask getParentTask() {
        return TaskTwig.supplyWithFXSafety(parentTask::get);
    }

    public void setName(String name) {
        TaskTwig.setWithFXSafety(this.name::set, name);
    }
    public void setDueTime(LocalTime dueTime) {
        TaskTwig.setWithFXSafety(this.dueTime::set, dueTime);
    }
    private void setLastDone(LocalDate lastDone) {
        TaskTwig.setWithFXSafety(this.lastDone::set, lastDone);
    }
    void setParentTask(TwigTask parentTask) {
        this.parentTask.set(parentTask);
    }

    public StringProperty nameProperty() {
        return name;
    }
    public ObjectProperty<LocalTime> dueTimeProperty() {
        return dueTime;
    }

    public void hashContents(MessageDigest digest) {
        digest.update(getName().getBytes(StandardCharsets.UTF_8));
        Optional.ofNullable(getLastDone()).ifPresent(
                date -> digest.update(date.toString().getBytes(StandardCharsets.UTF_8)));
        Optional.ofNullable(getDueTime()).ifPresent(
                time -> digest.update(time.toString().getBytes(StandardCharsets.UTF_8)));
    }

    public String toString() {
        return "TwigSubTask[" +
                "name=" + getName() +
                ", lastDone=" + getLastDone() +
                ", dueTime=" + getDueTime() +
                "]";
    }
}
