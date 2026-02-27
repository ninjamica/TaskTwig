package ninjamica.tasktwig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

@JsonIncludeProperties({"text"})
public class Journal extends TaskTwig.HasVersion{
    public static final int VERSION = 1;

    private final StringProperty text = new SimpleStringProperty();

    public Journal() {}

    @JsonCreator
    public Journal(@JsonProperty("text") String text) {
        this.text.setValue(text);
    }

    public StringProperty textProperty() {
        return text;
    }

    @JsonGetter("text")
    public String getText() {
        return text.getValue();
    }
}
