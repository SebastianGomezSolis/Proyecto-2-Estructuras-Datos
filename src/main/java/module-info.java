module com.sistema.proyecto2estructurasdatos {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.sistema.proyecto2estructurasdatos to javafx.fxml;
    opens com.sistema.proyecto2estructurasdatos.controller to javafx.fxml;

    exports com.sistema.proyecto2estructurasdatos;
    exports com.sistema.proyecto2estructurasdatos.controller;
}