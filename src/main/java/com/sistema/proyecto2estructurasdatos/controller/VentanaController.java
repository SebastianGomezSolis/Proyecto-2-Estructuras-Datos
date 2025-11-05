package com.sistema.proyecto2estructurasdatos.controller;

import com.sistema.proyecto2estructurasdatos.modelo.*;
import com.sistema.proyecto2estructurasdatos.Formato.CSV;
import com.sistema.proyecto2estructurasdatos.Formato.ResultadoCSV;
import com.sistema.proyecto2estructurasdatos.Formato.JSON;
import com.sistema.proyecto2estructurasdatos.logica.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;

/**
 * Controlador principal para clustering jerárquico
 * Genera JSON sin visualización
 */
public class VentanaController {

    @FXML private ComboBox<String> comboDistancia;
    @FXML private Button btnCargarCSV;
    @FXML private Button btnGenerar;
    @FXML private Button btnDescargar;
    @FXML private Label lblArchivo;
    @FXML private Label lblEstado;
    @FXML private VBox vboxColumnas;
    @FXML private ProgressIndicator progress;

    private ResultadoCSV datosCSV;
    private ArbolBinario dendrograma;
    private File ultimoArchivoJSON;

    // Listas para controles por columna
    private Lista<CheckBox> checkBoxesColumnas;
    private Lista<TextField> textFieldsPesos;
    private Lista<ComboBox<String>> combosNormalizacion;

    @FXML
    public void initialize() {
        // Configurar combo de distancia
        comboDistancia.getItems().addAll("Euclidiana", "Manhattan", "Coseno", "Hamming");
        comboDistancia.setValue("Euclidiana");

        // Estado inicial de botones
        btnGenerar.setDisable(true);
        btnDescargar.setDisable(true);

        if (progress != null) progress.setVisible(false);

        // Inicializar listas
        checkBoxesColumnas = new Lista<>();
        textFieldsPesos = new Lista<>();
        combosNormalizacion = new Lista<>();

        // Conectar eventos
        btnCargarCSV.setOnAction(e -> cargarCSV());
        btnGenerar.setOnAction(e -> generarDendrograma());
        btnDescargar.setOnAction(e -> descargarJSON());

        lblEstado.setText("Listo para cargar archivo CSV");
    }

    /**
     * Carga archivo CSV y genera controles para configurar variables
     */
    private void cargarCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo CSV");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos CSV", "*.csv")
        );

        File file = fc.showOpenDialog(btnCargarCSV.getScene().getWindow());
        if (file == null) return;

        // Bloquear controles durante carga
        btnCargarCSV.setDisable(true);
        btnGenerar.setDisable(true);
        btnDescargar.setDisable(true);

        if (progress != null) {
            progress.setVisible(true);
            progress.setProgress(-1);
        }

        lblArchivo.setText("Cargando: " + file.getName());
        lblEstado.setText("Leyendo archivo CSV...");

        // Tarea en segundo plano
        Task<ResultadoCSV> task = new Task<>() {
            @Override
            protected ResultadoCSV call() throws Exception {
                return CSV.leer(file.getAbsolutePath());
            }
        };

        task.setOnSucceeded(e -> {
            ResultadoCSV r = task.getValue();
            this.datosCSV = r;

            lblArchivo.setText(file.getName());
            lblEstado.setText(
                    String.format("CSV cargado: %d filas, %d columnas",
                            r.numFilas, r.numColumnas)
            );

            generarControlesColumnas();

            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnCargarCSV.setDisable(false);

            mostrarInfo("CSV cargado",
                    String.format("Filas: %d | Columnas: %d", r.numFilas, r.numColumnas));
        });

        task.setOnFailed(e -> {
            if (progress != null) progress.setVisible(false);
            btnCargarCSV.setDisable(false);

            Throwable ex = task.getException();
            lblEstado.setText("Error al cargar CSV");
            mostrarError("Error cargando CSV",
                    ex != null ? ex.getMessage() : "Fallo desconocido");
            if (ex != null) ex.printStackTrace();

            lblArchivo.setText("Sin seleccionar");
        });

        new Thread(task, "csv-loader").start();
    }

    /**
     * Genera controles dinámicos para cada columna del CSV
     * Muestra: Variable | Peso | Normalización
     */
    private void generarControlesColumnas() {
        vboxColumnas.getChildren().clear();
        checkBoxesColumnas = new Lista<>();
        textFieldsPesos = new Lista<>();
        combosNormalizacion = new Lista<>();

        if (datosCSV == null || datosCSV.nombresColumnas == null) return;

        int numColumnas = datosCSV.nombresColumnas.tamanio();
        String[] columnasEtiqueta = {"title", "name", "nombre", "id", "original_title"};

        // Encabezado de tabla
        HBox header = new HBox(10);
        header.setStyle("-fx-padding: 8; -fx-background-color: #00897b;");

        Label lblVar = new Label("Variable");
        lblVar.setPrefWidth(200);
        lblVar.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: white;");

        Label lblPeso = new Label("Peso");
        lblPeso.setPrefWidth(70);
        lblPeso.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: white;");

        Label lblNorm = new Label("Normalización");
        lblNorm.setPrefWidth(130);
        lblNorm.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: white;");

        header.getChildren().addAll(lblVar, lblPeso, lblNorm);
        vboxColumnas.getChildren().add(header);

        // Generar fila por cada columna
        for (int i = 0; i < numColumnas; i++) {
            String nombre = datosCSV.nombresColumnas.obtener(i);

            // Saltar columnas de etiqueta
            boolean esEtiqueta = false;
            for (String etiq : columnasEtiqueta) {
                if (nombre.equalsIgnoreCase(etiq)) {
                    esEtiqueta = true;
                    break;
                }
            }
            if (esEtiqueta) continue;

            // Crear fila
            HBox fila = new HBox(10);
            fila.setStyle("-fx-padding: 5; -fx-alignment: center-left; " +
                    "-fx-background-color: " + (i % 2 == 0 ? "#ffffff" : "#f5f5f5") + ";");

            // CheckBox con nombre de variable
            CheckBox chk = new CheckBox(nombre);
            chk.setSelected(true);
            chk.setPrefWidth(200);
            chk.setStyle("-fx-font-size: 10px;");

            // TextField para peso
            TextField txtPeso = new TextField("1.0");
            txtPeso.setPrefWidth(70);
            txtPeso.setStyle("-fx-font-size: 10px; -fx-alignment: center;");

            // ComboBox para normalización
            ComboBox<String> comboNorm = new ComboBox<>();
            comboNorm.setPrefWidth(130);
            comboNorm.setStyle("-fx-font-size: 10px;");

            // Determinar tipo de variable
            boolean esNumerica = (datosCSV.columnasNumericas != null &&
                    i < datosCSV.columnasNumericas.length &&
                    datosCSV.columnasNumericas[i]);

            if (esNumerica) {
                // Variables numéricas: opciones de normalización
                comboNorm.getItems().addAll("Min-Max", "Z-Score", "Logarítmica");
                comboNorm.setValue("Min-Max");
            } else {
                // Variables cualitativas: solo One-Hot
                comboNorm.getItems().add("One-Hot");
                comboNorm.setValue("One-Hot");
                comboNorm.setDisable(true);
            }

            fila.getChildren().addAll(chk, txtPeso, comboNorm);
            vboxColumnas.getChildren().add(fila);

            // Guardar referencias
            checkBoxesColumnas.agregar(chk);
            textFieldsPesos.agregar(txtPeso);
            combosNormalizacion.agregar(comboNorm);
        }
    }

    /**
     * Ejecuta clustering y genera JSON automáticamente
     */
    private void generarDendrograma() {
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0) {
            mostrarError("Sin datos", "Carga un CSV válido antes de generar.");
            return;
        }

        final int n = datosCSV.datos.tamanio();
        Lista<Dato> datosUsar = new Lista<>();
        for (int i = 0; i < n; i++) {
            datosUsar.agregar(datosCSV.datos.obtener(i));
        }

        // Bloquear controles
        btnGenerar.setDisable(true);
        btnDescargar.setDisable(true);
        if (progress != null) {
            progress.setVisible(true);
            progress.setProgress(-1);
        }

        lblEstado.setText("Procesando clustering...");

        // Leer configuración
        final String disSel = comboDistancia.getValue();
        final double[] pesos = obtenerPesos();
        final boolean[] ignoradas = obtenerColumnasIgnoradas();
        final INormalizacion[] normalizaciones = obtenerNormalizaciones();

        Task<ArbolBinario> task = new Task<>() {
            @Override
            protected ArbolBinario call() {
                IDistancia distancia =
                        FactoryDistancia.obtenerInstancia().crear(disSel);

                AlgoritmoClustering algoritmo =
                        new AlgoritmoClustering(normalizaciones, distancia, pesos, ignoradas);

                algoritmo.setColumnasNumericas(datosCSV.columnasNumericas);

                return algoritmo.ejecutar(datosUsar);
            }
        };

        task.setOnSucceeded(ev -> {
            this.dendrograma = task.getValue();

            // Generar JSON automáticamente
            try {
                // Crear archivo temporal
                File tempFile = File.createTempFile("dendrograma_", ".json");
                JSON.exportar(dendrograma, tempFile.getAbsolutePath());
                this.ultimoArchivoJSON = tempFile;

                if (progress != null) progress.setVisible(false);
                btnGenerar.setDisable(false);
                btnDescargar.setDisable(false);

                lblEstado.setText(
                        String.format("✓ Dendrograma generado (%d elementos) - Listo para descargar", n)
                );

                mostrarInfo("Éxito",
                        String.format("Dendrograma generado con %d elementos.\n" +
                                "Presiona 'Descargar JSON' para guardar el archivo.", n));

            } catch (IOException e) {
                mostrarError("Error al generar JSON", e.getMessage());
                lblEstado.setText("Error al generar JSON");
            }
        });

        task.setOnFailed(ev -> {
            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);

            Throwable ex = task.getException();
            lblEstado.setText("Error en clustering");
            mostrarError("Error al generar",
                    ex != null ? ex.getMessage() : "Fallo desconocido");
            if (ex != null) ex.printStackTrace();
        });

        new Thread(task, "cluster-worker").start();
    }

    /**
     * Descarga el archivo JSON generado
     */
    private void descargarJSON() {
        if (ultimoArchivoJSON == null || !ultimoArchivoJSON.exists()) {
            mostrarError("Sin archivo", "Genera el dendrograma primero.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar dendrograma JSON");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON", "*.json")
        );
        fc.setInitialFileName("dendrograma.json");

        File destino = fc.showSaveDialog(btnDescargar.getScene().getWindow());
        if (destino == null) return;

        try {
            // Copiar archivo temporal a destino elegido
            java.nio.file.Files.copy(
                    ultimoArchivoJSON.toPath(),
                    destino.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            lblEstado.setText("JSON descargado: " + destino.getName());
            mostrarInfo("Descargado",
                    "Archivo guardado en:\n" + destino.getAbsolutePath());

        } catch (IOException e) {
            mostrarError("Error al guardar", e.getMessage());
        }
    }

    /**
     * Obtiene array de normalizaciones según configuración por variable
     */
    private INormalizacion[] obtenerNormalizaciones() {
        if (datosCSV == null || datosCSV.datos == null ||
                datosCSV.datos.tamanio() == 0) return new INormalizacion[0];

        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        INormalizacion[] normalizaciones = new INormalizacion[dimensionReal];

        // Default: Min-Max
        for (int i = 0; i < dimensionReal; i++) {
            normalizaciones[i] = new NormalizacionMinMax();
        }

        // Mapear desde combos
        for (int i = 0; i < combosNormalizacion.tamanio() && i < dimensionReal; i++) {
            ComboBox<String> combo = combosNormalizacion.obtener(i);
            String seleccion = combo.getValue();

            if (seleccion != null && !seleccion.equals("One-Hot")) {
                normalizaciones[i] =
                        FactoryNormalizacion.obtenerInstancia().crear(seleccion);
            }
        }

        return normalizaciones;
    }

    /**
     * Obtiene pesos configurados para cada variable
     */
    private double[] obtenerPesos() {
        if (datosCSV == null || datosCSV.datos == null ||
                datosCSV.datos.tamanio() == 0) return new double[0];

        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        double[] pesos = new double[dimensionReal];

        for (int i = 0; i < textFieldsPesos.tamanio() && i < dimensionReal; i++) {
            TextField txtPeso = textFieldsPesos.obtener(i);
            double peso = 1.0;
            try {
                peso = Double.parseDouble(txtPeso.getText().trim());
            } catch (NumberFormatException ignored) {}
            pesos[i] = peso;
        }

        // Completar con 1.0
        for (int i = textFieldsPesos.tamanio(); i < dimensionReal; i++) {
            pesos[i] = 1.0;
        }

        return pesos;
    }

    /**
     * Obtiene variables marcadas como ignoradas
     */
    private boolean[] obtenerColumnasIgnoradas() {
        if (datosCSV == null || datosCSV.datos == null ||
                datosCSV.datos.tamanio() == 0) return new boolean[0];

        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        boolean[] ignoradas = new boolean[dimensionReal];

        for (int i = 0; i < checkBoxesColumnas.tamanio() && i < dimensionReal; i++) {
            CheckBox chk = checkBoxesColumnas.obtener(i);
            ignoradas[i] = !chk.isSelected();
        }

        // Resto: no ignorar
        for (int i = checkBoxesColumnas.tamanio(); i < dimensionReal; i++) {
            ignoradas[i] = false;
        }

        return ignoradas;
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}
