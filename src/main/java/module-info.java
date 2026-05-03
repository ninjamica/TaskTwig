module ninjamica.tasktwig.tasktwig {
    requires javafx.controls;
    requires transitive tools.jackson.databind;
    requires org.jetbrains.annotations;
    requires transitive org.controlsfx.controls;
    requires atlantafx.base;

    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires dropbox.core.sdk;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome6;
    requires com.gluonhq.attach.storage;


    opens ninjamica.tasktwig.ui to javafx.fxml;
    opens ninjamica.tasktwig.ui.util to javafx.fxml;
    opens ninjamica.tasktwig.core to javafx.fxml;
    exports ninjamica.tasktwig.core;
    exports ninjamica.tasktwig.ui;
    exports ninjamica.tasktwig.ui.util;
}