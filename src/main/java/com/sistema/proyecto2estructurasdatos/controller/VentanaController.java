package com.sistema.proyecto2estructurasdatos.controller;

import com.sistema.proyecto2estructurasdatos.modelo.*;
import com.sistema.proyecto2estructurasdatos.Formato.CSV;
import com.sistema.proyecto2estructurasdatos.Formato.ResultadoCSV;
import com.sistema.proyecto2estructurasdatos.Formato.JSON;
import com.sistema.proyecto2estructurasdatos.logica.*;

import javafx.concurrent.Task;
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
    @FXML private ProgressIndicator progress;

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

        if (progress != null) progress.setVisible(false);

        btnCargarCSV.setOnAction(e -> cargarCSV());
        btnGenerar.setOnAction(e -> generarDendrograma());
        btnExportar.setOnAction(e -> exportarJSON());
    }

    // --- CARGA CSV EN BACKGROUND CON FEEDBACK VISUAL ---
    private void cargarCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));
        File file = fc.showOpenDialog(btnCargarCSV.getScene().getWindow());
        if (file == null) return;

        btnCargarCSV.setDisable(true);
        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);
        if (progress != null) {
            progress.setVisible(true);
            progress.setProgress(-1);
        }
        lblArchivo.setText("Cargando: " + file.getName());

        Task<ResultadoCSV> task = new Task<>() {
            @Override
            protected ResultadoCSV call() throws Exception {
                return CSV.leer(file.getAbsolutePath());
            }
        };

        task.setOnSucceeded(e -> { // lambda
            ResultadoCSV r = task.getValue();
            this.datosCSV = r;
            lblArchivo.setText(file.getName());
            generarControlesColumnas();

            mostrarInfo("CSV cargado",
                    "Filas: " + r.numFilas + "  |  Columnas: " + r.numColumnas);

            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnExportar.setDisable(true);
            btnCargarCSV.setDisable(false);
        });

        task.setOnFailed(e -> {
            if (progress != null) progress.setVisible(false);
            btnCargarCSV.setDisable(false);
            btnGenerar.setDisable(true);
            btnExportar.setDisable(true);

            Throwable ex = task.getException();
            mostrarError("Error cargando CSV",
                    (ex != null ? ex.getMessage() : "Fallo desconocido"));
            if (ex != null) ex.printStackTrace();
            lblArchivo.setText("Sin seleccionar");
        });

        new Thread(task, "csv-loader").start();
    }

    /*// --- GENERA CHECKBOX + TEXTFIELD PESO POR COLUMNA ---
    private void generarControlesColumnas() {
        vboxColumnas.getChildren().clear();
        if (datosCSV == null || datosCSV.nombresColumnas == null) return;

        String[] numericasPreferidas = {
                "budget","popularity","revenue","runtime","vote_average","vote_count"
        };

        int count = 0;
        for (String nombre : numericasPreferidas) {
            int idx = indexOfHeader(nombre);
            if (idx != -1) {
                CheckBox chk = new CheckBox(nombre);
                chk.setSelected(true);
                chk.setStyle("-fx-font-size: 9px;");
                TextField txtPeso = new TextField("1.0");
                txtPeso.setPrefWidth(40);
                txtPeso.setStyle("-fx-font-size: 9px; -fx-alignment: center;");
                VBox box = new VBox(2, chk, txtPeso);
                vboxColumnas.getChildren().add(box);
                count++;
            }
        }
        System.out.println("Variables numéricas disponibles: " + count);
    }*/
    private void generarControlesColumnas() {
        vboxColumnas.getChildren().clear();
        if (datosCSV == null || datosCSV.nombresColumnas == null) return;

        int numColumnas = datosCSV.nombresColumnas.tamanio();
        int numFilas = Math.min(datosCSV.datos.tamanio(), 20); // solo revisar las primeras 20 filas para eficiencia

        for (int i = 0; i < numColumnas; i++) {
            String nombre = datosCSV.nombresColumnas.obtener(i);
            boolean esNumerica = true;

            // Revisar algunos valores de la columna
            for (int j = 0; j < numFilas; j++) {
                try {
                    Dato fila = datosCSV.datos.obtener(j);
                    Object valorObj = fila.getVectorOriginal().obtener(i);
                    if (valorObj == null) continue;

                    String valor = String.valueOf(valorObj).trim();
                    if (valor.isEmpty()) continue;

                    Double.parseDouble(valor); // si lanza excepción, no es numérica
                } catch (Exception e) {
                    esNumerica = false;
                    break;
                }
            }

            if (esNumerica) {
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

        System.out.println("Columnas numéricas detectadas: " + vboxColumnas.getChildren().size());
    }


    //CAMBIAR
    private int indexOfHeader(String nombre) {
        for (int i = 0; i < datosCSV.nombresColumnas.tamanio(); i++) {
            String h = datosCSV.nombresColumnas.obtener(i);
            if (h != null && h.equalsIgnoreCase(nombre)) return i;
        }
        return -1;
    }

    private static final int N_PREVIEW_MAX = 600;

    private void generarDendrograma() {
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0) {
            mostrarError("Sin datos", "Carga un CSV válido antes de generar.");
            return;
        }

        final int n = datosCSV.datos.tamanio();
        final int usarN = Math.min(n, N_PREVIEW_MAX);
        Lista<Dato> datosUsar = new Lista<>();
        for (int i = 0; i < usarN; i++) datosUsar.agregar(datosCSV.datos.obtener(i));

        if (n > N_PREVIEW_MAX) {
            System.out.println("Preview: usando " + usarN + " de " + n + " filas.");
            lblArchivo.setText(lblArchivo.getText() + "  (preview " + usarN + "/" + n + ")");
        }

        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);
        if (progress != null) { progress.setVisible(true); progress.setProgress(-1); }

        final String nomSel = comboNormalizacion.getValue();
        final String disSel = comboDistancia.getValue();
        final double[] pesos = obtenerPesos();
        final boolean[] ignoradas = obtenerColumnasIgnoradas();

        Task<ArbolBinario> task = new Task<>() {
            @Override
            protected ArbolBinario call() {
                INormalizacion normalizacion = FactoryNormalizacion.crear(nomSel);
                IDistancia distancia = FactoryDistancia.crear(disSel);
                AlgoritmoClustering algoritmo = new AlgoritmoClustering(normalizacion, distancia, pesos, ignoradas);
                long t0 = System.currentTimeMillis();
                ArbolBinario arbol = algoritmo.ejecutar(datosUsar);
                long t1 = System.currentTimeMillis();
                System.out.println("Clustering (" + usarN + " filas) tomó: " + (t1 - t0) + " ms");
                return arbol;
            }
        };

        task.setOnSucceeded(ev -> {
            this.dendrograma = task.getValue();
            dibujarDendrograma();
            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnExportar.setDisable(false);
            mostrarInfo("Éxito", "Dendrograma generado" + (n > N_PREVIEW_MAX ? " (vista previa)" : "") + ".");
        });
        task.setOnFailed(ev -> {
            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnExportar.setDisable(true);
            Throwable ex = task.getException();
            mostrarError("Error al generar", ex != null ? ex.getMessage() : "Fallo desconocido");
            if (ex != null) ex.printStackTrace();
        });

        new Thread(task, "cluster-worker").start();
    }

    private double[] obtenerPesos() {
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0)
            return new double[0];

        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        double[] pesos = new double[dimensionReal];
        int idx = 0;

        for (javafx.scene.Node nodo : vboxColumnas.getChildren()) {
            if (nodo instanceof VBox box && box.getChildren().size() >= 2) {
                TextField txtPeso = (TextField) box.getChildren().get(1);
                double peso = 1.0;
                try { peso = Double.parseDouble(txtPeso.getText()); }
                catch (NumberFormatException ignored) {}
                if (idx < pesos.length) pesos[idx++] = peso;
            }
        }
        while (idx < dimensionReal) pesos[idx++] = 1.0;
        return pesos;
    }

    private boolean[] obtenerColumnasIgnoradas() {
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0)
            return new boolean[0];

        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        boolean[] ignoradas = new boolean[dimensionReal];
        int idx = 0;

        for (javafx.scene.Node nodo : vboxColumnas.getChildren()) {
            if (nodo instanceof VBox box && box.getChildren().size() >= 1) {
                CheckBox chk = (CheckBox) box.getChildren().get(0);
                if (idx < ignoradas.length) ignoradas[idx++] = !chk.isSelected();
            }
        }
        while (idx < dimensionReal) ignoradas[idx++] = false;
        return ignoradas;
    }

    private void trazarRamasEnPath(NodoArbol nodo,
                                   Map<NodoArbol, Double> xmap,
                                   double yBase,
                                   double factorEscala,
                                   GraphicsContext gc) {
        if (nodo == null || nodo.esHoja()) return;

        Double xNodo = xmap.get(nodo);
        if (xNodo == null) return;
        double yNodo = yBase - (nodo.getDistancia() * factorEscala);

        NodoArbol izq = nodo.getIzquierdo();
        if (izq != null) {
            Double xIzq = xmap.get(izq);
            if (xIzq != null) {
                double yIzq = izq.esHoja() ? yBase : yBase - (izq.getDistancia() * factorEscala);
                gc.moveTo(xNodo, yNodo);
                gc.lineTo(xIzq, yNodo);
                gc.lineTo(xIzq, yIzq);
                trazarRamasEnPath(izq, xmap, yBase, factorEscala, gc);
            }
        }

        NodoArbol der = nodo.getDerecho();
        if (der != null) {
            Double xDer = xmap.get(der);
            if (xDer != null) {
                double yDer = der.esHoja() ? yBase : yBase - (der.getDistancia() * factorEscala);
                gc.moveTo(xNodo, yNodo);
                gc.lineTo(xDer, yNodo);
                gc.lineTo(xDer, yDer);
                trazarRamasEnPath(der, xmap, yBase, factorEscala, gc);
            }
        }
    }

    // --- DIBUJO DEL DENDROGRAMA ---
    private void dibujarDendrograma() {
        if (dendrograma == null || dendrograma.getRaiz() == null) return;

        GraphicsContext gc = canvasDendrograma.getGraphicsContext2D();
        double width  = canvasDendrograma.getWidth();
        double height = canvasDendrograma.getHeight();

        gc.setFill(Color.web("#f0f9ff"));
        gc.fillRect(0, 0, width, height);

        Lista<NodoArbol> hojas = new Lista<>();
        obtenerHojasEnOrden(dendrograma.getRaiz(), hojas);
        int numHojas = hojas.tamanio();
        if (numHojas == 0) {
            mostrarError("Error", "No hay datos para visualizar");
            return;
        }

        // Márgenes y área de dibujo
        double margenIzq = 60, margenDer = 40, margenSup = 40, margenInf = 220;
        double anchoUtil  = Math.max(1, width  - margenIzq - margenDer);
        double alturaUtil = Math.max(1, height - margenSup - margenInf);
        double espacioHorizontal = anchoUtil / numHojas;

        double distanciaMaxima = Math.max(1e-9, dendrograma.getRaiz().getDistancia());
        double factorEscala = alturaUtil / distanciaMaxima;
        double yBase = margenSup + alturaUtil;

        Map<NodoArbol, Double> xmap = new HashMap<>();
        for (int i = 0; i < numHojas; i++) {
            NodoArbol hoja = hojas.obtener(i);
            double x = margenIzq + (i + 0.5) * espacioHorizontal;
            xmap.put(hoja, x);
        }
        calcularPosicionesInternas(dendrograma.getRaiz(), xmap);

        // Cuadrícula
        gc.setStroke(Color.web("#e5e7eb"));
        gc.setLineWidth(1);
        gc.setFill(Color.web("#6b7280"));
        gc.setFont(javafx.scene.text.Font.font("Arial", 10));
        int numLineas = 5;
        for (int i = 0; i <= numLineas; i++) {
            double dist = (distanciaMaxima * i) / numLineas;
            double y = margenSup + alturaUtil - (dist * factorEscala);
            gc.strokeLine(margenIzq, y, width - margenDer, y);
            gc.fillText(String.format("%.2f", dist), margenIzq - 35, y + 4);
        }

        // Ramas
        gc.setStroke(Color.web("#2dd4bf"));
        gc.setLineWidth(1.5);
        gc.beginPath();
        trazarRamasEnPath(dendrograma.getRaiz(), xmap, yBase, factorEscala, gc);
        gc.stroke();

        // === ETIQUETAS VERTICALES ===
        gc.setFill(Color.web("#0f766e"));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 10));
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);

        int maxEtiquetas = (int)Math.floor(anchoUtil / 8.0);
        int salto = Math.max(1, (int)Math.ceil((double)numHojas / Math.max(1, maxEtiquetas)));

        for (int i = 0; i < numHojas; i += salto) {
            NodoArbol hoja = hojas.obtener(i);
            double x = xmap.get(hoja);
            double yTexto = yBase + 35;

            gc.save();
            gc.translate(x, yTexto);
            gc.rotate(90);
            gc.fillText(acortar(hoja.getEtiqueta(), 18), 0, 0);
            gc.restore();
        }
    }

    private String acortar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : (s.substring(0, max) + "…");
    }

    private void obtenerHojasEnOrden(NodoArbol nodo, Lista<NodoArbol> hojas) {
        if (nodo == null) return;
        if (nodo.esHoja()) { hojas.agregar(nodo); return; }
        obtenerHojasEnOrden(nodo.getIzquierdo(), hojas);
        obtenerHojasEnOrden(nodo.getDerecho(), hojas);
    }

    private void calcularPosicionesInternas(NodoArbol nodo, Map<NodoArbol, Double> posiciones) {
        if (nodo == null || nodo.esHoja()) return;
        calcularPosicionesInternas(nodo.getIzquierdo(), posiciones);
        calcularPosicionesInternas(nodo.getDerecho(), posiciones);
        Double xIzq = posiciones.get(nodo.getIzquierdo());
        Double xDer = posiciones.get(nodo.getDerecho());
        if (xIzq != null && xDer != null) posiciones.put(nodo, (xIzq + xDer) / 2.0);
    }

    // --- EXPORTAR JSON ---
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
                mostrarInfo("Éxito", "Dendrograma exportado correctamente a:\n" + archivo.getName());
            } catch (IOException ex) {
                mostrarError("Error al exportar", ex.getMessage());
            }
        }
    }

    // --- DIALOGS ---
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
