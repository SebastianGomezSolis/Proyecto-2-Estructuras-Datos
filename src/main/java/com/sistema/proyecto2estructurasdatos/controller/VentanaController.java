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

        task.setOnSucceeded(e -> {
            ResultadoCSV r = task.getValue();
            this.datosCSV = r;
            lblArchivo.setText(file.getName());
            generarControlesColumnas();
            mostrarInfo("CSV cargado",
                    "Filas: " + r.numFilas + " | Columnas: " + r.numColumnas);

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

    private void generarControlesColumnas() {
        vboxColumnas.getChildren().clear();
        if (datosCSV == null || datosCSV.nombresColumnas == null) return;

        int numColumnas = datosCSV.nombresColumnas.tamanio();
        int numFilas = Math.min(datosCSV.datos.tamanio(), 50); // solo para detectar tipos

        String[] columnasEtiqueta = {"title", "name", "nombre", "id", "original_title"};

        for (int i = 0; i < numColumnas; i++) {
            String nombre = datosCSV.nombresColumnas.obtener(i);

            boolean esEtiqueta = false;
            for (String etiq : columnasEtiqueta) {
                if (nombre.equalsIgnoreCase(etiq)) {
                    esEtiqueta = true;
                    break;
                }
            }

            if (esEtiqueta) continue;

            TipoColumna tipo = detectarTipoColumna(i, numFilas);

            CheckBox chk = new CheckBox(nombre + " (" + tipo.nombre + ")");
            chk.setSelected(tipo == TipoColumna.NUMERICA);
            chk.setStyle("-fx-font-size: 9px;");

            TextField txtPeso = new TextField("1.0");
            txtPeso.setPrefWidth(50);
            txtPeso.setStyle("-fx-font-size: 9px; -fx-alignment: center;");

            VBox box = new VBox(2, chk, txtPeso);
            vboxColumnas.getChildren().add(box);
        }

        System.out.println("Total columnas detectadas: " + vboxColumnas.getChildren().size());
    }

    private TipoColumna detectarTipoColumna(int indiceColumna, int filasAnalizar) {
        int numericos = 0;
        int textos = 0;

        for (int j = 0; j < filasAnalizar && j < datosCSV.datos.tamanio(); j++) {
            try {
                Dato fila = datosCSV.datos.obtener(j);
                Object valorObj = fila.getVectorOriginal().obtener(indiceColumna);

                if (valorObj == null) continue;

                String valor = String.valueOf(valorObj).trim();
                if (valor.isEmpty()) continue;

                try {
                    Double.parseDouble(valor);
                    numericos++;
                } catch (NumberFormatException e) {
                    textos++;
                }
            } catch (Exception e) {
                // Ignorar errores
            }
        }

        if (numericos > textos && numericos > filasAnalizar * 0.7) {
            return TipoColumna.NUMERICA;
        } else if (textos > 0) {
            return TipoColumna.CATEGORICA;
        }

        return TipoColumna.MIXTA;
    }

    private enum TipoColumna {
        NUMERICA("Núm"),
        CATEGORICA("Cat"),
        MIXTA("Mix");

        String nombre;
        TipoColumna(String nombre) { this.nombre = nombre; }
    }

    // ======== SIN LÍMITE: usa TODAS las filas del CSV ========
    private void generarDendrograma() {
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0) {
            mostrarError("Sin datos", "Carga un CSV válido antes de generar.");
            return;
        }

        final int n = datosCSV.datos.tamanio();

        // Tomar TODAS las filas
        Lista<Dato> datosUsar = new Lista<>();
        for (int i = 0; i < n; i++) {
            datosUsar.agregar(datosCSV.datos.obtener(i));
        }

        // Actualiza la etiqueta mostrando que usas todo
        lblArchivo.setText(lblArchivo.getText().split(" \\(")[0] + " (muestra " + n + "/" + n + ")");

        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);

        if (progress != null) {
            progress.setVisible(true);
            progress.setProgress(-1);
        }

        final String nomSel = comboNormalizacion.getValue();
        final String disSel = comboDistancia.getValue();
        final double[] pesos = obtenerPesos();
        final boolean[] ignoradas = obtenerColumnasIgnoradas();

        Task<ArbolBinario> task = new Task<>() {
            @Override
            protected ArbolBinario call() {
                INormalizacion normalizacion = FactoryNormalizacion.crear(nomSel);
                IDistancia distancia = FactoryDistancia.crear(disSel);

                AlgoritmoClustering algoritmo = new AlgoritmoClustering(
                        normalizacion, distancia, pesos, ignoradas
                );

                long t0 = System.currentTimeMillis();
                ArbolBinario arbol = algoritmo.ejecutar(datosUsar);
                long t1 = System.currentTimeMillis();

                System.out.println("Clustering (" + datosUsar.tamanio() + " filas) tomó: " + (t1 - t0) + " ms");
                return arbol;
            }
        };

        task.setOnSucceeded(ev -> {
            this.dendrograma = task.getValue();
            dibujarDendrograma();

            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnExportar.setDisable(false);

            mostrarInfo("Éxito", "Dendrograma generado con " + n + " elementos.");
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
    // =========================================================

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
                try {
                    peso = Double.parseDouble(txtPeso.getText());
                } catch (NumberFormatException ignored) {}

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

    /**
     * Traza las ramas del dendrograma en forma de "U"
     */
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
                gc.strokeLine(xIzq, yIzq, xIzq, yNodo);
                trazarRamasEnPath(izq, xmap, yBase, factorEscala, gc);
            }
        }

        NodoArbol der = nodo.getDerecho();
        if (der != null) {
            Double xDer = xmap.get(der);
            if (xDer != null) {
                double yDer = der.esHoja() ? yBase : yBase - (der.getDistancia() * factorEscala);
                gc.strokeLine(xDer, yDer, xDer, yNodo);
                trazarRamasEnPath(der, xmap, yBase, factorEscala, gc);
            }
        }

        if (izq != null && der != null) {
            Double xIzq = xmap.get(izq);
            Double xDer = xmap.get(der);
            if (xIzq != null && xDer != null) {
                gc.strokeLine(xIzq, yNodo, xDer, yNodo);
            }
        }
    }

    /**
     * DENDROGRAMA MEJORADO CON MEJOR ESPACIADO
     */
    private void dibujarDendrograma() {
        if (dendrograma == null || dendrograma.getRaiz() == null) return;

        GraphicsContext gc = canvasDendrograma.getGraphicsContext2D();
        double width = canvasDendrograma.getWidth();
        double height = canvasDendrograma.getHeight();

        Lista<NodoArbol> hojas = new Lista<>();
        obtenerHojasEnOrden(dendrograma.getRaiz(), hojas);
        int numHojas = hojas.tamanio();

        if (numHojas == 0) {
            mostrarError("Error", "No hay datos para visualizar");
            return;
        }

        // Cálculo de ancho ideal por cantidad de hojas
        double margenIzq = 70;
        double margenDer = 30;
        double margenSup = 40;
        double margenInf = 200;

        double anchoIdeal = numHojas * 30.0;
        double anchoMinCanvas = 1100;

        // Ajusta canvas (si tienes el Canvas dentro de un ScrollPane, esto permitirá scroll horizontal)
        if (anchoIdeal > anchoMinCanvas) {
            canvasDendrograma.setWidth(anchoIdeal);
            width = canvasDendrograma.getWidth();
        }

        // Fondo
        gc.setFill(Color.web("#f0f9ff"));
        gc.fillRect(0, 0, width, height);

        double anchoUtil = width - margenIzq - margenDer;
        double alturaUtil = height - margenSup - margenInf;

        double espacioHorizontal = anchoUtil / Math.max(1, numHojas);

        double distanciaMaxima = Math.max(1e-9, dendrograma.getRaiz().getDistancia());
        double factorEscala = alturaUtil / distanciaMaxima;
        double yBase = margenSup + alturaUtil;

        // Posiciones X
        Map<NodoArbol, Double> xmap = new HashMap<>();
        for (int i = 0; i < numHojas; i++) {
            NodoArbol hoja = hojas.obtener(i);
            double x = margenIzq + (i + 0.5) * espacioHorizontal;
            xmap.put(hoja, x);
        }

        calcularPosicionesInternas(dendrograma.getRaiz(), xmap);

        // Cuadrícula
        gc.setStroke(Color.web("#e5e7eb"));
        gc.setLineWidth(0.8);
        gc.setFill(Color.web("#6b7280"));
        gc.setFont(javafx.scene.text.Font.font("Arial", 9));

        int numLineas = 6;
        for (int i = 0; i <= numLineas; i++) {
            double dist = (distanciaMaxima * i) / numLineas;
            double y = margenSup + alturaUtil - (dist * factorEscala);
            gc.strokeLine(margenIzq, y, width - margenDer, y);
            gc.fillText(String.format("%.4f", dist), margenIzq - 55, y + 4);
        }

        // Ramas
        gc.setStroke(Color.web("#2dd4bf"));
        gc.setLineWidth(1.8);
        trazarRamasEnPath(dendrograma.getRaiz(), xmap, yBase, factorEscala, gc);

        // Etiquetas (muestra espaciadas para no tapar)
        gc.setFill(Color.web("#0f766e"));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 8));
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);

        int maxEtiquetas = Math.min(25, (int)(anchoUtil / 30.0));
        int salto = Math.max(1, numHojas / Math.max(1, maxEtiquetas));

        for (int i = 0; i < numHojas; i += salto) {
            NodoArbol hoja = hojas.obtener(i);
            double x = xmap.get(hoja);
            double yTexto = yBase + 12;

            gc.save();
            gc.translate(x, yTexto);
            gc.rotate(90);
            gc.fillText(acortar(hoja.getEtiqueta(), 22), 0, 0);
            gc.restore();
        }

        // Línea base
        gc.setStroke(Color.web("#374151"));
        gc.setLineWidth(2);
        gc.strokeLine(margenIzq, yBase, width - margenDer, yBase);

        // Título
        gc.setFill(Color.web("#00897b"));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 11));
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        gc.fillText("Dendrograma (" + numHojas + " elementos) | " +
                        comboDistancia.getValue() + " | " + comboNormalizacion.getValue(),
                margenIzq, 22);
    }

    private String acortar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : (s.substring(0, max - 1) + "…");
    }

    private void obtenerHojasEnOrden(NodoArbol nodo, Lista<NodoArbol> hojas) {
        if (nodo == null) return;
        if (nodo.esHoja()) {
            hojas.agregar(nodo);
            return;
        }
        obtenerHojasEnOrden(nodo.getIzquierdo(), hojas);
        obtenerHojasEnOrden(nodo.getDerecho(), hojas);
    }

    private void calcularPosicionesInternas(NodoArbol nodo, Map<NodoArbol, Double> posiciones) {
        if (nodo == null || nodo.esHoja()) return;

        calcularPosicionesInternas(nodo.getIzquierdo(), posiciones);
        calcularPosicionesInternas(nodo.getDerecho(), posiciones);

        Double xIzq = posiciones.get(nodo.getIzquierdo());
        Double xDer = posiciones.get(nodo.getDerecho());

        if (xIzq != null && xDer != null) {
            posiciones.put(nodo, (xIzq + xDer) / 2.0);
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
