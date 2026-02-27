package ninjamica.tasktwig;

import com.fasterxml.jackson.annotation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;

public final class TwigList extends TaskTwig.HasVersion {
    public static final int VERSION = 1;

    private final StringProperty name;
    private final ObservableList<TwigListItem> items;
    private final BooleanProperty expanded;

    public TwigList(StringProperty name, ObservableList<TwigListItem> items, BooleanProperty expanded) {
        this.name = name;
        this.items = items;
        this.expanded = expanded;
    }

    public record TwigListItem(StringProperty name, BooleanProperty done) {
        @JsonCreator
        public TwigListItem(@JsonProperty("name") String name, @JsonProperty("done") boolean done) {
            this(new SimpleStringProperty(name), new SimpleBooleanProperty(done));
        }

        public TwigListItem(String name) {
            this(name, false);
        }

        public TwigListItem() {
            this("", false);
        }

        @JsonGetter("name")
        public String getName() {
            return name.get();
        }

        @JsonGetter("done")
        public boolean isDone() {
            return done.get();
        }
    }

    @JsonCreator
    public TwigList(@JsonProperty("name") String name, @JsonProperty("items") List<TwigListItem> items, @JsonProperty("expanded") boolean expanded) {
        this(new SimpleStringProperty(name), FXCollections.observableList(items), new SimpleBooleanProperty(expanded));
    }

    public TwigList(String name) {
        this(new SimpleStringProperty(name), FXCollections.observableArrayList(), new SimpleBooleanProperty(true));
    }

    @JsonGetter("name")
    public String getName() {
        return name.get();
    }

    @JsonGetter("expanded")
    public boolean isExpanded() {
        return expanded.get();
    }

    public StringProperty name() {
        return name;
    }

    @JsonGetter("items")
    public ObservableList<TwigListItem> items() {
        return items;
    }

    public BooleanProperty expanded() {
        return expanded;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TwigList) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.items, that.items) &&
                Objects.equals(this.expanded, that.expanded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, items, expanded);
    }

    @Override
    public String toString() {
        return "TwigList[" +
                "name=" + name + ", " +
                "items=" + items + ", " +
                "expanded=" + expanded + ']';
    }


}
