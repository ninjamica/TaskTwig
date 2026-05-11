package ninjamica.tasktwig.core;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

public interface TaskInterface {
    String getName();
    boolean isToday();
    boolean isDone();
    boolean isOverdue();
    void setDone(boolean done);

    StringProperty nameProperty();
    BooleanExpression isTodayObservable();
    BooleanExpression isDoneObservable();
    BooleanExpression isOverdueObservable();
    ObservableList<TwigSubTask> getSubTasks();
}
