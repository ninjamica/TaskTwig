package ninjamica.tasktwig.ui.util;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import ninjamica.tasktwig.core.TaskCategory;

public class TaskCategoryList extends VBox {

    private ObservableList<TaskCategory> categories;

    public void setCategories(ObservableList<TaskCategory> categories) {
        unbind();

        this.categories = categories;
        if (this.categories != null) {
            fillCategoryList();
            this.categories.addListener(this::categoryListChange);
        }
    }

    private void unbind() {
        if (categories != null) {
            categories.removeListener(this::categoryListChange);
        }
        getChildren().clear();
    }

    private void fillCategoryList() {
        getChildren().clear();
        for (TaskCategory category : categories) {
            getChildren().add(new TaskCategoryBox(category));
        }
    }

    private void categoryListChange(ListChangeListener.Change<? extends TaskCategory> change) {
        while (change.next()) {
            if (change.wasPermutated()) {
                for (int i = change.getFrom(); i <= change.getTo(); ++i) {
                    Node cell = getChildren().remove(i);
                    getChildren().add(change.getPermutation(i), cell);
                }
            }
            else if (change.wasReplaced()) {
                for (int i = change.getFrom(); i < change.getTo(); ++i) {
                    TaskCategoryBox catBox = (TaskCategoryBox) getChildren().get(i);
                    TaskCategory newCat = change.getList().get(i);
                    catBox.setCategory(newCat);
                }
            }
            else if (change.wasRemoved()) {
                for (int i = change.getFrom(); i < change.getTo(); ++i) {
                    TaskCategoryBox catBox = (TaskCategoryBox) getChildren().remove(i);
                    catBox.unbind();
                }
            }
            else if (change.wasAdded()) {
                for (int i = change.getFrom(); i < change.getTo(); i++) {
                    getChildren().add(i, new TaskCategoryBox(change.getList().get(i)));
                }
            }
        }
    }
}
