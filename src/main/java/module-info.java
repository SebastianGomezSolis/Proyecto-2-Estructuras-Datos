module com.sistema.proyecto2estructurasdatos {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.sistema.proyecto2estructurasdatos to javafx.fxml;
    exports com.sistema.proyecto2estructurasdatos;
}