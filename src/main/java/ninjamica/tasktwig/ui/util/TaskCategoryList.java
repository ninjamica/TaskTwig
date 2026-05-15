package ninjamica.tasktwig.ui.util;

import ninjamica.tasktwig.core.TaskCategory;
import ninjamica.tasktwig.core.TwigTask;

import java.util.function.Predicate;

public class TaskCategoryList extends ListBox<TaskCategory, TaskCategoryView> {

    public TaskCategoryList(Predicate<TwigTask> filter) {
        super(
                item -> new TaskCategoryView(item, filter),
                TaskCategoryView::unbind
        );
    }
}
