package ninjamica.tasktwig.ui.util;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class ListChangeMapper<T, U> implements ListChangeListener<T> {
    
    protected final ObservableList<U> mappedList;
    protected final Function<T, U> constructor;
    protected final Consumer<U> destructor;
    protected Runnable afterChangeRunnable;
    
    public ListChangeMapper(ObservableList<U> list, @NotNull Function<T, U> constructor, @Nullable Consumer<U> destructor) {
        mappedList = list;
        this.constructor = constructor;
        this.destructor = destructor;
    }
    
    public ListChangeMapper(ObservableList<U> list, @NotNull Function<T, U> constructor) {
        this(list, constructor, null);
    }
    
    public void constructFromList(ObservableList<T> list) {
        mappedList.clear();
        for (T item : list) {
            mappedList.add(constructor.apply(item));
        }
    }

    public void setAfterChangeRunnable(Runnable afterChangeRunnable) {
        this.afterChangeRunnable = afterChangeRunnable;
    }
    
    @Override
    public void onChanged(Change<? extends T> change) {
        while (change.next()) {
            if (change.wasPermutated()) {
                for (int i = change.getFrom(); i <= change.getTo(); i++) {
                    U item = mappedList.remove(i);
                    mappedList.add(change.getPermutation(i), item);
                }
            }
            else if (!change.wasUpdated()) {
                if (change.wasRemoved()) {
                    for (int i = change.getFrom(); i <= change.getTo(); i++) {
                        U item = mappedList.remove(i);

                        if (destructor != null)
                            destructor.accept(item);
                    }
                }
                else if (change.wasAdded()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        mappedList.add(i, constructor.apply(change.getList().get(i)));
                    }
                }
            }
        }

        if (afterChangeRunnable != null)
            afterChangeRunnable.run();
    }
}
