package ninjamica.tasktwig.core;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;
import java.time.LocalTime;

public class TwigSubTask {
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<LocalTime> dueTime = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> lastDone = new SimpleObjectProperty<>();
}
