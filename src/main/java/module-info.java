module ninjamica.tasktwig.tasktwig {
    requires javafx.controls;
    requires javafx.fxml;
    requires tools.jackson.databind;
    requires org.jetbrains.annotations;
    requires org.controlsfx.controls;
    requires atlantafx.base;

    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires java.xml;
    requires dropbox.core.sdk;
    requires java.desktop;


    opens ninjamica.tasktwig to javafx.fxml;
    exports ninjamica.tasktwig;
    exports ninjamica.tasktwig.ui;
    opens ninjamica.tasktwig.ui to javafx.fxml;
    exports ninjamica.tasktwig.ui.PropertySheetItems;
    opens ninjamica.tasktwig.ui.PropertySheetItems to javafx.fxml;
}