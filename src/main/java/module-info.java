module cs151.application {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    // Icons (FontAwesome 5 through Ikonli)
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    // Backend / SQLite persistence
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens cs151.application to javafx.fxml;
    opens cs151.frontend.controller to javafx.fxml;
    exports cs151.application;
}