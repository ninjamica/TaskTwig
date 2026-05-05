package ninjamica.tasktwig.core;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Paint;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@JsonIncludeProperties({"name", "paint"})
public class TaskCategory {

    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<Paint> paint = new SimpleObjectProperty<>();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    public TaskCategory(String name, Paint paint) {
        this.name.set(name);
        this.paint.set(paint);
    }
    public TaskCategory(JsonNode node, int version) {
        String name;
        Paint paint;
        switch (version) {
            case 9:
                name = node.get("name").asString();
                paint = Paint.valueOf(node.get("paint").asString());
                break;

            default:
                throw new TaskTwig.TwigJsonVersionException("Unsupported Task version for having categories: " + version);
        }
        this(name, paint);
    }

    public StringProperty nameProperty() {
        return name;
    }

    @JsonGetter("name")
    public String getName() {
        return TaskTwig.supplyWithFXSafety(name::get);
    }

    public void setName(String name) {
        TaskTwig.runWithFXSafety(() -> this.name.set(name));
    }

    public ObjectProperty<Paint> paintProperty() {
        return paint;
    }

    public Paint getPaint() {
        return TaskTwig.supplyWithFXSafety(paint::get);
    }

    public void setPaint(Paint paint) {
        TaskTwig.runWithFXSafety(() -> this.paint.set(paint));
    }

    @JsonGetter("paint")
    public String getPaintJson() {
        return getPaint().toString();
    }

    public ObservableList<Task> tasksProperty() {
        return tasks;
    }

    public List<Task> getTasks() {
        return TaskTwig.supplyWithFXSafety(() -> new ArrayList<>(tasks));
    }

    public void hashContents(MessageDigest digest) {
        digest.update(getName().getBytes(StandardCharsets.UTF_8));
        digest.update(getPaint().toString().getBytes(StandardCharsets.UTF_8));
        getTasks().forEach(task -> task.hashContents(digest));
    }
}
