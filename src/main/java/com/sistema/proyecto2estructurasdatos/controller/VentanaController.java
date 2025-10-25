package com.sistema.proyecto2estructurasdatos.controller;

import com.sistema.proyecto2estructurasdatos.algoritmos.*;
import com.sistema.proyecto2estructurasdatos.modelo.*;
import com.sistema.proyecto2estructurasdatos.Formato.CSV;
import com.sistema.proyecto2estructurasdatos.Formato.JSON;
import com.sistema.proyecto2estructurasdatos.Formato.ResultadoCSV;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
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
    @FXML private Canvas canvasDendrograma;
    @FXML private VBox vboxColumnas;

    private ResultadoCSV datosCSV;
    private ArbolBinario dendrograma;

    @FXML
    public void initialize() {
        comboNormalizacion.getItems().addAll("Min-Max", "Z-Score", "Logarítmica");
        comboNormalizacion.setValue("Min-Max");

        comboDistancia.getItems().addAll("Euclidiana", "Manhattan", "Coseno", "Hamming");
        comboDistancia.setValue("Euclidiana");

        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);

        btnCargarCSV.setOnAction(e -> cargarCSV());
        btnGenerar.setOnAction(e -> generarDendrograma());
        btnExportar.setOnAction(e -> exportarJSON());
    }

    private void cargarCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos CSV", "*.csv")
        );

        File archivo = fileChooser.showOpenDialog(btnCargarCSV.getScene().getWindow());
        if (archivo != null) {
            try {
                datosCSV = CSV.leer(archivo.getAbsolutePath());
                lblArchivo.setText(archivo.getName());
                btnGenerar.setDisable(false);
                mostrarInfo("Éxito", "Archivo cargado correctamente\nFilas: " +
                        datosCSV.numFilas + ", Columnas: " + datosCSV.numColumnas);
                generarControlesColumnas();
            } catch (IOException ex) {
                mostrarError("Error al cargar el archivo", ex.getMessage());
            }
        }
    }

    private void generarControlesColumnas() {
        vboxColumnas.getChildren().clear();
        if (datosCSV == null) return;

        for (int i = 0; i < datosCSV.nombresColumnas.tamanio(); i++) {
            String nombre = datosCSV.nombresColumnas.obtener(i);
            CheckBox chk = new CheckBox(nombre);
            chk.setSelected(true);
            chk.setStyle("-fx-font-size: 9px;");

            TextField txtPeso = new TextField("1.0");
            txtPeso.setPrefWidth(40);
            txtPeso.setStyle("-fx-font-size: 9px; -fx-alignment: center;");

            VBox box = new VBox(2, chk, txtPeso);
            vboxColumnas.getChildren().add(box);
        }
    }

    /*private void generarDendrograma() {
        try {
            INormalizacion normalizacion = FactoryNormalizacion.crear(comboNormalizacion.getValue());
            IDistancia distancia = FactoryDistancia.crear(comboDistancia.getValue());

            double[] pesos = obtenerPesos();
            boolean[] ignoradas = obtenerColumnasIgnoradas();

            AlgoritmoClustering algoritmo = new AlgoritmoClustering(
                    normalizacion, distancia, pesos, ignoradas
            );

            dendrograma = algoritmo.ejecutar(datosCSV.datos);

            dibujarDendrograma();

            ///btnExportar.setDisable(false);

            mostrarInfo("Éxito", "Dendrograma generado exitosamente");

        } catch (Exception ex) {
            mostrarError("Error al generar dendrograma", ex.getMessage());
            ex.printStackTrace();
            btnExportar.setDisable(true);
        }
    }
*/
    private void generarDendrograma() {
        try {
            System.out.println("=== INICIO GENERACIÓN ===");

            INormalizacion normalizacion = FactoryNormalizacion.crear(comboNormalizacion.getValue());
            IDistancia distancia = FactoryDistancia.crear(comboDistancia.getValue());

            double[] pesos = obtenerPesos();
            boolean[] ignoradas = obtenerColumnasIgnoradas();

            System.out.println("Ejecutando algoritmo...");
            AlgoritmoClustering algoritmo = new AlgoritmoClustering(
                    normalizacion, distancia, pesos, ignoradas
            );

            dendrograma = algoritmo.ejecutar(datosCSV.datos);
            System.out.println("Dendrograma generado: " + (dendrograma != null));

            dibujarDendrograma();
            System.out.println("Dendrograma dibujado");

            System.out.println("Habilitando botón exportar...");
            btnExportar.setDisable(false);
            System.out.println("Estado botón: " + !btnExportar.isDisable());

            mostrarInfo("Éxito", "Dendrograma generado exitosamente");

        } catch (Exception ex) {
            System.out.println("ERROR: " + ex.getMessage());
            mostrarError("Error al generar dendrograma", ex.getMessage());
            ex.printStackTrace();
            btnExportar.setDisable(true);
        }
    }
    private double[] obtenerPesos() {
        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        double[] pesos = new double[dimensionReal];
        int idx = 0;

        for (javafx.scene.Node nodo : vboxColumnas.getChildren()) {
            if (nodo instanceof VBox) {
                VBox box = (VBox) nodo;
                TextField txtPeso = (TextField) box.getChildren().get(1);
                double peso = 1.0;
                try {
                    peso = Double.parseDouble(txtPeso.getText());
                } catch (NumberFormatException ignored) {}
                pesos[idx++] = peso;
            }
        }

        while (idx < dimensionReal) pesos[idx++] = 1.0;
        return pesos;
    }

    private boolean[] obtenerColumnasIgnoradas() {
        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        boolean[] ignoradas = new boolean[dimensionReal];
        int idx = 0;

        for (javafx.scene.Node nodo : vboxColumnas.getChildren()) {
            if (nodo instanceof VBox) {
                VBox box = (VBox) nodo;
                CheckBox chk = (CheckBox) box.getChildren().get(0);
                ignoradas[idx++] = !chk.isSelected();
            }
        }

        while (idx < dimensionReal) ignoradas[idx++] = false;
        return ignoradas;
    }

    private void dibujarDendrograma() {
        if (dendrograma == null || dendrograma.getRaiz() == null) return;

        GraphicsContext gc = canvasDendrograma.getGraphicsContext2D();
        double width = canvasDendrograma.getWidth();
        double height = canvasDendrograma.getHeight();

        gc.setFill(Color.web("#fafafa"));
        gc.fillRect(0, 0, width, height);

        Lista<NodoArbol> hojas = new Lista<>();
        obtenerHojas(dendrograma.getRaiz(), hojas);
        int numHojas = hojas.tamanio();
        if (numHojas == 0) return;

        double distanciaMaxima = dendrograma.getRaiz().getDistancia();
        double espacioHorizontal = width / (numHojas + 1);
        double margen = 50;
        double alturaUtil = height - (2 * margen);

        Map<NodoArbol, Double> posicionesX = new HashMap<>();
        asignarPosicionesX(dendrograma.getRaiz(), posicionesX, 0, numHojas, espacioHorizontal);

        gc.setStroke(Color.web("#26a69a"));
        gc.setLineWidth(2.5);

        double factorEscala = distanciaMaxima > 0 ? alturaUtil / distanciaMaxima : 100;
        dibujarNodo(gc, dendrograma.getRaiz(), posicionesX, height - margen, height - margen, factorEscala);

        gc.setFill(Color.web("#00695c"));
        gc.setFont(javafx.scene.text.Font.font("Arial", 10));
        for (int i = 0; i < hojas.tamanio(); i++) {
            NodoArbol hoja = hojas.obtener(i);
            double x = posicionesX.get(hoja);

            gc.save();
            gc.translate(x, height - margen + 10);
            gc.rotate(-45);
            gc.fillText(hoja.getEtiqueta(), 0, 0);
            gc.restore();
        }
    }

    private void obtenerHojas(NodoArbol nodo, Lista<NodoArbol> hojas) {
        if (nodo == null) return;
        if (nodo.esHoja()) hojas.agregar(nodo);
        else {
            obtenerHojas(nodo.getIzquierdo(), hojas);
            obtenerHojas(nodo.getDerecho(), hojas);
        }
    }

    private int asignarPosicionesX(NodoArbol nodo, Map<NodoArbol, Double> posiciones,
                                   int contadorHojas, int totalHojas, double espacioHorizontal) {
        if (nodo == null) return contadorHojas;

        if (nodo.esHoja()) {
            posiciones.put(nodo, (contadorHojas + 1) * espacioHorizontal);
            return contadorHojas + 1;
        }

        int contador = asignarPosicionesX(nodo.getIzquierdo(), posiciones, contadorHojas,
                totalHojas, espacioHorizontal);
        contador = asignarPosicionesX(nodo.getDerecho(), posiciones, contador, totalHojas,
                espacioHorizontal);

        double xIzq = posiciones.getOrDefault(nodo.getIzquierdo(), 0.0);
        double xDer = posiciones.getOrDefault(nodo.getDerecho(), 0.0);
        posiciones.put(nodo, (xIzq + xDer) / 2);

        return contador;
    }

    private void dibujarNodo(GraphicsContext gc, NodoArbol nodo,
                             Map<NodoArbol, Double> posiciones,
                             double y, double yBase, double factorEscala) {
        if (nodo == null) return;

        double x = posiciones.get(nodo);
        double yNodo = yBase - (nodo.getDistancia() * factorEscala);

        if (!nodo.esHoja()) {
            gc.strokeLine(x, yNodo, x, y);

            if (nodo.getIzquierdo() != null) {
                double xIzq = posiciones.get(nodo.getIzquierdo());
                gc.strokeLine(x, yNodo, xIzq, yNodo);
                dibujarNodo(gc, nodo.getIzquierdo(), posiciones, yNodo, yBase, factorEscala);
            }

            if (nodo.getDerecho() != null) {
                double xDer = posiciones.get(nodo.getDerecho());
                gc.strokeLine(x, yNodo, xDer, yNodo);
                dibujarNodo(gc, nodo.getDerecho(), posiciones, yNodo, yBase, factorEscala);
            }
        }
    }

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
                mostrarInfo("Éxito", "Dendrograma exportado correctamente a:\n" +
                        archivo.getName());
            } catch (IOException ex) {
                mostrarError("Error al exportar", ex.getMessage());
            }
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}