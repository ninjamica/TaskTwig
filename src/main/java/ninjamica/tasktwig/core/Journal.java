package ninjamica.tasktwig.core;

import com.fasterxml.jackson.annotation.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@JsonIncludeProperties({"text", "weight", "routines", "tasks"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Journal(StringProperty text,
                      ObjectProperty<Float> weight,
                      ObservableList<String> completedRoutines,
                      ObservableList<String> completedTasks) {
    public static final int VERSION = 3;

    public Journal() {
        this(new SimpleStringProperty(""),
                new SimpleObjectProperty<>(),
                FXCollections.observableArrayList(),
                FXCollections.observableArrayList());
    }

    public Journal(String text, Float weight, List<String> routines, List<String> tasks) {
        this(new SimpleStringProperty(text),
             new SimpleObjectProperty<>(weight),
             FXCollections.observableArrayList(routines),
             FXCollections.observableArrayList(tasks));
    }

    public Journal(JsonNode node, int version) {
        String text;
        Float weight = null;
        List<String> routines = new ArrayList<>();
        List<String> tasks = new ArrayList<>();

        switch (version) {
            case 3:
                JsonNode weightNode = node.get("weight");
                if (weightNode != null)
                    weight = weightNode.asFloat();

            case 2:
                for (JsonNode routineNode : node.get("routines")) {
                    routines.add(routineNode.asString());
                }

                for (JsonNode taskNode : node.get("tasks")) {
                    tasks.add(taskNode.asString());
                }

            case 1:
                text = node.get("text").asString();
                break;

            default:
                throw new TaskTwig.TwigJsonVersionException("Unsupported Journal version: " + version);
        }

        this(text, weight, routines, tasks);
    }

    public StringProperty textProperty() {
        return text;
    }

    @JsonGetter("text")
    public String getText() {
        return TaskTwig.callWithFXSafety(text::getValue);
    }

    @JsonGetter("weight")
    public Float getWeight() {
        return TaskTwig.callWithFXSafety(weight::getValue);
    }

    @JsonGetter("tasks")
    public List<String> getTasksJson() {
        if (TaskTwig.notFxThread())
            return TaskTwig.callWithFXSafety(() -> new ArrayList<>(completedTasks));
        else
            return completedTasks;
    }

    @JsonGetter("routines")
    public List<String> getRoutinesJson() {
        if (TaskTwig.notFxThread())
            return TaskTwig.callWithFXSafety(() -> new ArrayList<>(completedRoutines));
        else
            return completedRoutines;
    }

    public boolean isEmpty() {
        return getText().isBlank() && getWeight() == null && getTasksJson().isEmpty() && getRoutinesJson().isEmpty();
    }

    public void hashContents(MessageDigest digest) {
        String text = getText();
        Float weight = getWeight();
        List<String> tasks = getTasksJson();
        List<String> routines = getRoutinesJson();

        if (!text.isBlank() || weight != null || !tasks.isEmpty() || !routines.isEmpty()) {
            digest.update(getText().getBytes(StandardCharsets.UTF_8));

            if (weight != null)
                digest.update(getWeight().toString().getBytes(StandardCharsets.UTF_8));

            for (String task : tasks) {
                digest.update(task.getBytes(StandardCharsets.UTF_8));
            }

            for (String routine : routines) {
                digest.update(routine.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

}
