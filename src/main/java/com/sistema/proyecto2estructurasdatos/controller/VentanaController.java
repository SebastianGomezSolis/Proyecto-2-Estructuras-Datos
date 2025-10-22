package com.sistema.proyecto2estructurasdatos.controller;

import com.sistema.proyecto2estructurasdatos.algoritmos.*;
import com.sistema.proyecto2estructurasdatos.configuracion.ConfiguracionSingleton;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import com.sistema.proyecto2estructurasdatos.modelo.*;
import com.sistema.proyecto2estructurasdatos.Formato.CSV;
import com.sistema.proyecto2estructurasdatos.Formato.JSON;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VentanaController {

    @FXML private ComboBox<String> comboNormalizacion;
    @FXML private ComboBox<String> comboDistancia;
    @FXML private Button btnCargarCSV;
    @FXML private Button btnGenerar;
    @FXML private Button btnExportar;
    @FXML private Label lblArchivo;
    @FXML private VBox vboxColumnas;
    @FXML private Canvas canvasDendrograma;
    @FXML private TextArea txtLog;
    @FXML private ProgressBar progressBar;

    private CSV.ResultadoCSV datosCSV;
    private ArbolBinario dendrograma;
    private Map<Integer, CheckBox> checkBoxesColumnas;
    private Map<Integer, TextField> textFieldsPesos;

    /**
     * Inicialización del controlador - O(1)
     */
    @FXML
    public void initialize() {
        checkBoxesColumnas = new HashMap<>();
        textFieldsPesos = new HashMap<>();

        // Configurar ComboBoxes
        comboNormalizacion.getItems().addAll("Min-Max", "Z-Score", "Logarítmica");
        comboNormalizacion.setValue("Min-Max");

        comboDistancia.getItems().addAll("Euclidiana", "Manhattan", "Coseno", "Hamming");
        comboDistancia.setValue("Euclidiana");

        // Configurar botones
        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);

        // Configurar eventos
        btnCargarCSV.setOnAction(e -> cargarCSV());
        btnGenerar.setOnAction(e -> generarDendrograma());
        btnExportar.setOnAction(e -> exportarJSON());

        log("Sistema iniciado. Cargue un archivo CSV para comenzar.");
    }

    /**
     * Carga un archivo CSV - O(n*m)
     */
    private void cargarCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos CSV", "*.csv")
        );

        File archivo = fileChooser.showOpenDialog(btnCargarCSV.getScene().getWindow());

        if (archivo != null) {
            try {
                log("Cargando archivo: " + archivo.getName());
                datosCSV = CSV.leer(archivo.getAbsolutePath());

                lblArchivo.setText(archivo.getName());
                log("Archivo cargado exitosamente.");
                log("Filas: " + datosCSV.numFilas + ", Columnas: " + datosCSV.numColumnas);

                // Mostrar dimensión real del vector después de one-hot
                int dimensionReal = obtenerDimensionVectores();
                log("Dimensión del vector (con one-hot): " + dimensionReal);

                // Crear controles para cada columna
                crearControlesColumnas();

                btnGenerar.setDisable(false);

            } catch (IOException ex) {
                mostrarError("Error al cargar el archivo", ex.getMessage());
            }
        }
    }

    /**
     * Obtiene la dimensión real de los vectores después del one-hot encoding
     * Complejidad: O(1) - solo accede al primer elemento
     */
    private int obtenerDimensionVectores() {
        if (datosCSV == null || datosCSV.datos.tamanio() == 0) {
            return 0;
        }
        return datosCSV.datos.obtener(0).getVector().tamanio();
    }

    /**
     * Crea controles para configurar columnas - O(n)
     * donde n es el número de columnas ORIGINALES del CSV
     */
    private void crearControlesColumnas() {
        vboxColumnas.getChildren().clear();
        checkBoxesColumnas.clear();
        textFieldsPesos.clear();

        Label titulo = new Label("Configuración de Columnas:");
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #00897b;");
        vboxColumnas.getChildren().add(titulo);

        for (int i = 0; i < datosCSV.columnas.tamanio(); i++) {
            CSV.InfoColumna col = datosCSV.columnas.obtener(i);

            VBox vboxColumna = new VBox(5);
            vboxColumna.setStyle(
                    "-fx-padding: 8; " +
                            "-fx-border-color: #b2dfdb; " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 5; " +
                            "-fx-background-color: #f1f8f6; " +
                            "-fx-background-radius: 5;"
            );

            // CheckBox para usar/ignorar columna
            CheckBox cbUsar = new CheckBox("Usar: " + col.nombre);
            cbUsar.setSelected(true);
            cbUsar.setStyle("-fx-font-weight: bold; -fx-text-fill: #00695c;");
            checkBoxesColumnas.put(i, cbUsar);

            // TextField para peso
            Label lblPeso = new Label("Peso:");
            lblPeso.setStyle("-fx-font-size: 11px; -fx-text-fill: #546e7a;");

            TextField tfPeso = new TextField("1.0");
            tfPeso.setPrefWidth(100);
            tfPeso.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-border-color: #80cbc4; " +
                            "-fx-border-radius: 3; " +
                            "-fx-background-radius: 3;"
            );
            textFieldsPesos.put(i, tfPeso);

            // Info sobre tipo y dimensiones que genera
            String tipo;
            if (col.esNumerica) {
                tipo = "Numérica (1 dimensión)";
            } else {
                tipo = "Categórica → " + col.categorias.tamanio() + " dimensiones";
            }
            Label lblTipo = new Label("Tipo: " + tipo);
            lblTipo.setStyle("-fx-font-size: 10px; -fx-text-fill: #78909c;");

            vboxColumna.getChildren().addAll(cbUsar, lblPeso, tfPeso, lblTipo);
            vboxColumnas.getChildren().add(vboxColumna);
        }
    }

    /**
     * Genera el dendrograma - O(n³)
     */
    private void generarDendrograma() {
        try {
            log("Iniciando generación del dendrograma...");
            progressBar.setProgress(0.1);

            // Obtener configuración
            INormalizacion normalizacion = FactoryNormalizacion.crear(comboNormalizacion.getValue());
            IDistancia distancia = FactoryDistancia.crear(comboDistancia.getValue());

            log("Normalización: " + normalizacion.getNombre());
            log("Distancia: " + distancia.getNombre());

            // Obtener pesos y columnas ignoradas (CORREGIDO)
            double[] pesos = obtenerPesos();
            boolean[] ignoradas = obtenerColumnasIgnoradas();

            // Log de configuración
            log("Pesos configurados: " + pesos.length + " dimensiones");
            int columnasUsadas = 0;
            for (boolean ignorada : ignoradas) {
                if (!ignorada) columnasUsadas++;
            }
            log("Dimensiones activas: " + columnasUsadas + "/" + ignoradas.length);

            progressBar.setProgress(0.3);

            // Ejecutar algoritmo
            AlgoritmoClusteringJerarquico algoritmo = new AlgoritmoClusteringJerarquico(
                    normalizacion, distancia, pesos, ignoradas
            );

            log("Procesando datos...");
            dendrograma = algoritmo.ejecutar(datosCSV.datos);

            progressBar.setProgress(0.7);

            // Visualizar
            log("Dibujando dendrograma...");
            dibujarDendrograma();

            progressBar.setProgress(1.0);
            log("¡Dendrograma generado exitosamente!");

            btnExportar.setDisable(false);

        } catch (Exception ex) {
            mostrarError("Error al generar dendrograma", ex.getMessage());
            ex.printStackTrace();
            progressBar.setProgress(0);
        }
    }

    /**
     * Obtiene los pesos configurados expandidos según one-hot encoding
     * Complejidad: O(n*m) donde n es número de columnas y m es promedio de categorías
     *
     * CORRECCIÓN CRÍTICA: Ahora maneja correctamente la expansión one-hot
     */
    private double[] obtenerPesos() {
        // Obtener dimensión real del vector (después de one-hot)
        int dimensionReal = obtenerDimensionVectores();
        double[] pesosExpandidos = new double[dimensionReal];

        int indiceExpandido = 0;

        // Iterar sobre columnas ORIGINALES
        for (int i = 0; i < datosCSV.columnas.tamanio(); i++) {
            CSV.InfoColumna col = datosCSV.columnas.obtener(i);

            // Obtener peso configurado por el usuario
            double pesoUsuario = obtenerPesoDeTextField(i);

            if (col.esNumerica) {
                // Columna numérica: 1 dimensión en el vector
                pesosExpandidos[indiceExpandido] = pesoUsuario;
                indiceExpandido++;
            } else {
                // Columna categórica: N dimensiones (one-hot)
                // Aplicar el mismo peso a todas las dimensiones de esta columna
                int numCategorias = col.categorias.tamanio();
                for (int j = 0; j < numCategorias; j++) {
                    pesosExpandidos[indiceExpandido] = pesoUsuario;
                    indiceExpandido++;
                }
            }
        }

        return pesosExpandidos;
    }

    /**
     * Obtiene el peso de un TextField específico
     * Complejidad: O(1)
     */
    private double obtenerPesoDeTextField(int indiceColumna) {
        TextField tf = textFieldsPesos.get(indiceColumna);
        if (tf == null) {
            return 1.0;
        }

        try {
            double peso = Double.parseDouble(tf.getText());
            // Validar que el peso sea positivo
            if (peso <= 0) {
                log("ADVERTENCIA: Peso inválido en columna " + indiceColumna +
                        ". Usando 1.0");
                return 1.0;
            }
            return peso;
        } catch (NumberFormatException e) {
            log("ADVERTENCIA: Formato inválido en peso de columna " + indiceColumna +
                    ". Usando 1.0");
            return 1.0;
        }
    }

    /**
     * Obtiene las columnas ignoradas expandidas según one-hot encoding
     * Complejidad: O(n*m) donde n es número de columnas y m es promedio de categorías
     *
     * CORRECCIÓN CRÍTICA: Ahora maneja correctamente la expansión one-hot
     */
    private boolean[] obtenerColumnasIgnoradas() {
        // Obtener dimensión real del vector (después de one-hot)
        int dimensionReal = obtenerDimensionVectores();
        boolean[] ignoradasExpandidas = new boolean[dimensionReal];

        int indiceExpandido = 0;

        // Iterar sobre columnas ORIGINALES
        for (int i = 0; i < datosCSV.columnas.tamanio(); i++) {
            CSV.InfoColumna col = datosCSV.columnas.obtener(i);
            CheckBox cb = checkBoxesColumnas.get(i);

            // Si checkbox está desmarcado, ignorar esta columna
            boolean ignorar = (cb == null || !cb.isSelected());

            if (col.esNumerica) {
                // Columna numérica: 1 dimensión en el vector
                ignoradasExpandidas[indiceExpandido] = ignorar;
                indiceExpandido++;
            } else {
                // Columna categórica: N dimensiones (one-hot)
                // Si se ignora la columna, ignorar TODAS sus dimensiones
                int numCategorias = col.categorias.tamanio();
                for (int j = 0; j < numCategorias; j++) {
                    ignoradasExpandidas[indiceExpandido] = ignorar;
                    indiceExpandido++;
                }
            }
        }

        return ignoradasExpandidas;
    }

    /**
     * Dibuja el dendrograma en el canvas - O(n)
     */
    private void dibujarDendrograma() {
        if (dendrograma == null || dendrograma.getRaiz() == null) return;

        GraphicsContext gc = canvasDendrograma.getGraphicsContext2D();
        double width = canvasDendrograma.getWidth();
        double height = canvasDendrograma.getHeight();

        // Limpiar canvas con fondo
        gc.setFill(Color.web("#fafafa"));
        gc.fillRect(0, 0, width, height);

        // Obtener hojas para calcular espaciado
        Lista<ArbolBinario.Nodo> hojas = new Lista<>();
        obtenerHojas(dendrograma.getRaiz(), hojas);

        int numHojas = hojas.tamanio();
        if (numHojas == 0) return;

        // Calcular espaciado
        double espacioHorizontal = width / (numHojas + 1);
        double margen = 50;

        // Dibujar árbol
        Map<ArbolBinario.Nodo, Double> posicionesX = new HashMap<>();
        asignarPosicionesX(dendrograma.getRaiz(), posicionesX, 0, numHojas, espacioHorizontal);

        // Dibujar líneas del árbol
        gc.setStroke(Color.web("#00897b"));
        gc.setLineWidth(2);
        dibujarNodo(gc, dendrograma.getRaiz(), posicionesX, height - margen, height, 0);

        // Dibujar etiquetas de hojas
        gc.setFill(Color.web("#00695c"));
        gc.setFont(javafx.scene.text.Font.font("Arial", 10));
        for (int i = 0; i < hojas.tamanio(); i++) {
            ArbolBinario.Nodo hoja = hojas.obtener(i);
            double x = posicionesX.get(hoja);

            // Rotar texto para mejor visualización
            gc.save();
            gc.translate(x, height - 5);
            gc.rotate(-45);
            gc.fillText(hoja.getEtiqueta(), 0, 0);
            gc.restore();
        }
    }

    /**
     * Obtiene todas las hojas del árbol - O(n)
     */
    private void obtenerHojas(ArbolBinario.Nodo nodo, Lista<ArbolBinario.Nodo> hojas) {
        if (nodo == null) return;

        if (nodo.esHoja()) {
            hojas.agregar(nodo);
        } else {
            obtenerHojas(nodo.getIzquierdo(), hojas);
            obtenerHojas(nodo.getDerecho(), hojas);
        }
    }

    /**
     * Asigna posiciones X a cada nodo - O(n)
     */
    private int asignarPosicionesX(ArbolBinario.Nodo nodo, Map<ArbolBinario.Nodo, Double> posiciones,
                                   int contadorHojas, int totalHojas, double espacioHorizontal) {
        if (nodo == null) return contadorHojas;

        if (nodo.esHoja()) {
            posiciones.put(nodo, (contadorHojas + 1) * espacioHorizontal);
            return contadorHojas + 1;
        }

        int contador = asignarPosicionesX(nodo.getIzquierdo(), posiciones, contadorHojas, totalHojas, espacioHorizontal);
        contador = asignarPosicionesX(nodo.getDerecho(), posiciones, contador, totalHojas, espacioHorizontal);

        double xIzq = posiciones.getOrDefault(nodo.getIzquierdo(), 0.0);
        double xDer = posiciones.getOrDefault(nodo.getDerecho(), 0.0);
        posiciones.put(nodo, (xIzq + xDer) / 2);

        return contador;
    }

    /**
     * Dibuja un nodo y sus hijos recursivamente - O(n)
     */
    private void dibujarNodo(GraphicsContext gc, ArbolBinario.Nodo nodo,
                             Map<ArbolBinario.Nodo, Double> posiciones,
                             double y, double yBase, double distanciaMaxima) {
        if (nodo == null) return;

        double x = posiciones.get(nodo);
        double factorEscala = 100; // Factor para escalar distancias
        double yNodo = yBase - (nodo.getDistancia() * factorEscala);

        if (!nodo.esHoja()) {
            // Dibujar línea vertical
            gc.strokeLine(x, yNodo, x, y);

            // Dibujar líneas a hijos
            if (nodo.getIzquierdo() != null) {
                double xIzq = posiciones.get(nodo.getIzquierdo());
                gc.strokeLine(x, yNodo, xIzq, yNodo);
                dibujarNodo(gc, nodo.getIzquierdo(), posiciones, yNodo, yBase, distanciaMaxima);
            }

            if (nodo.getDerecho() != null) {
                double xDer = posiciones.get(nodo.getDerecho());
                gc.strokeLine(x, yNodo, xDer, yNodo);
                dibujarNodo(gc, nodo.getDerecho(), posiciones, yNodo, yBase, distanciaMaxima);
            }
        }
    }

    /**
     * Exporta el dendrograma a JSON - O(n)
     */
    private void exportarJSON() {
        if (dendrograma == null) {
            mostrarError("Error", "No hay dendrograma para exportar");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar dendrograma");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo JSON", "*.json")
        );
        fileChooser.setInitialFileName("dendrograma.json");

        File archivo = fileChooser.showSaveDialog(btnExportar.getScene().getWindow());

        if (archivo != null) {
            try {
                JSON.exportar(dendrograma, archivo.getAbsolutePath());
                log("Dendrograma exportado exitosamente a: " + archivo.getName());
                mostrarInfo("Éxito", "Dendrograma exportado correctamente");
            } catch (IOException ex) {
                mostrarError("Error al exportar", ex.getMessage());
            }
        }
    }

    /**
     * Agrega mensaje al log - O(1)
     */
    private void log(String mensaje) {
        txtLog.appendText(mensaje + "\n");
    }

    /**
     * Muestra diálogo de error
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
        log("ERROR: " + mensaje);
    }

    /**
     * Muestra diálogo de información
     */
    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}