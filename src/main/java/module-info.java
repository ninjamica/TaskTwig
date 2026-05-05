module ninjamica.tasktwig.tasktwig {
    requires javafx.controls;
    requires transitive tools.jackson.databind;
    requires org.jetbrains.annotations;
    requires atlantafx.base;

    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires dropbox.core.sdk;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome6;
    requires com.gluonhq.attach.storage;
    requires com.gluonhq.attach.settings;
    requires com.gluonhq.attach.browser;

    exports ninjamica.tasktwig.core;
    exports ninjamica.tasktwig.ui;
    exports ninjamica.tasktwig.ui.util;
}