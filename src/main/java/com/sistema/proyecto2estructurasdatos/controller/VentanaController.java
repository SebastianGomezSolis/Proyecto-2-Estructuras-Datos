package com.sistema.proyecto2estructurasdatos.controller;

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
import com.sistema.proyecto2estructurasdatos.logica.*;

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

    /**
     * Dibuja el dendrograma completo con todas las mejoras visuales
     */
    private void dibujarDendrograma() {
        if (dendrograma == null || dendrograma.getRaiz() == null) return;

        GraphicsContext gc = canvasDendrograma.getGraphicsContext2D();
        double width = canvasDendrograma.getWidth();
        double height = canvasDendrograma.getHeight();

        // Limpiar canvas con fondo suave
        gc.setFill(Color.web("#f0f9ff"));
        gc.fillRect(0, 0, width, height);

        // Obtener hojas en orden correcto (in-order traversal)
        Lista<NodoArbol> hojas = new Lista<>();
        obtenerHojasEnOrden(dendrograma.getRaiz(), hojas);
        int numHojas = hojas.tamanio();

        if (numHojas == 0) {
            mostrarError("Error", "No hay datos para visualizar");
            return;
        }

        // Configuración de márgenes
        double margenIzq = 60;
        double margenDer = 40;
        double margenSup = 40;
        double margenInf = 120; // Espacio para etiquetas

        double anchoUtil = width - margenIzq - margenDer;
        double alturaUtil = height - margenSup - margenInf;

        // Calcular espaciado uniforme
        double espacioHorizontal = anchoUtil / numHojas;

        // Obtener distancia máxima para escalar
        double distanciaMaxima = dendrograma.getRaiz().getDistancia();
        if (distanciaMaxima <= 0) distanciaMaxima = 1.0;

        // Factor de escala vertical
        double factorEscala = alturaUtil / distanciaMaxima;

        // Asignar posiciones X a todas las hojas (centradas en su espacio)
        Map<NodoArbol, Double> posicionesX = new HashMap<>();
        for (int i = 0; i < numHojas; i++) {
            NodoArbol hoja = hojas.obtener(i);
            double x = margenIzq + (i + 0.5) * espacioHorizontal;
            posicionesX.put(hoja, x);
        }

        // Calcular posiciones X para nodos internos (promedio de hijos)
        calcularPosicionesInternas(dendrograma.getRaiz(), posicionesX);

        // Dibujar líneas de cuadrícula horizontales con etiquetas de distancia
        gc.setStroke(Color.web("#e5e7eb"));
        gc.setLineWidth(1);
        gc.setFont(javafx.scene.text.Font.font("Arial", 10));
        gc.setFill(Color.web("#6b7280"));

        int numLineas = 5;
        for (int i = 0; i <= numLineas; i++) {
            double dist = (distanciaMaxima * i) / numLineas;
            double y = margenSup + alturaUtil - (dist * factorEscala);

            gc.strokeLine(margenIzq, y, width - margenDer, y);
            gc.fillText(String.format("%.2f", dist), margenIzq - 35, y + 4);
        }

        // Dibujar el dendrograma
        gc.setStroke(Color.web("#2dd4bf"));
        gc.setLineWidth(2.5);
        double yBase = margenSup + alturaUtil;

        dibujarNodo(gc, dendrograma.getRaiz(), posicionesX, yBase, factorEscala, margenSup);

        // Dibujar etiquetas de las hojas con rotación
        gc.setFill(Color.web("#0f766e"));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 11));

        for (int i = 0; i < numHojas; i++) {
            NodoArbol hoja = hojas.obtener(i);
            double x = posicionesX.get(hoja);

            gc.save();
            gc.translate(x, yBase + 5);
            gc.rotate(-60); // 60 grados para mejor legibilidad
            gc.fillText(hoja.getEtiqueta(), 0, 0);
            gc.restore();
        }
    }

    /**
     * Obtiene las hojas en orden (in-order traversal) para mantener
     * el orden correcto de izquierda a derecha en el dendrograma
     */
    private void obtenerHojasEnOrden(NodoArbol nodo, Lista<NodoArbol> hojas) {
        if (nodo == null) return;

        if (nodo.esHoja()) {
            hojas.agregar(nodo);
            return;
        }

        // Recorrer primero el subárbol izquierdo
        obtenerHojasEnOrden(nodo.getIzquierdo(), hojas);
        // Luego el subárbol derecho
        obtenerHojasEnOrden(nodo.getDerecho(), hojas);
    }

    /**
     * Calcula recursivamente las posiciones X de los nodos internos
     * como el punto medio entre sus hijos
     */
    private void calcularPosicionesInternas(NodoArbol nodo, Map<NodoArbol, Double> posiciones) {
        if (nodo == null || nodo.esHoja()) return;

        // Primero calcular posiciones de los hijos recursivamente
        calcularPosicionesInternas(nodo.getIzquierdo(), posiciones);
        calcularPosicionesInternas(nodo.getDerecho(), posiciones);

        // Calcular posición de este nodo como promedio de sus hijos
        Double xIzq = posiciones.get(nodo.getIzquierdo());
        Double xDer = posiciones.get(nodo.getDerecho());

        if (xIzq != null && xDer != null) {
            posiciones.put(nodo, (xIzq + xDer) / 2.0);
        }
    }

    /**
     * Dibuja recursivamente un nodo y sus conexiones en forma de U invertida
     * Patrón: línea horizontal al nivel del padre, luego líneas verticales hacia abajo
     */
    private void dibujarNodo(GraphicsContext gc, NodoArbol nodo,
                             Map<NodoArbol, Double> posiciones,
                             double yBase, double factorEscala, double margenSup) {
        if (nodo == null) return;

        // Si es hoja, no hay nada que dibujar (solo la etiqueta que se dibuja después)
        if (nodo.esHoja()) return;

        // Obtener posición X de este nodo
        Double xNodo = posiciones.get(nodo);
        if (xNodo == null) return;

        // Calcular Y de este nodo basado en su distancia
        double yNodo = yBase - (nodo.getDistancia() * factorEscala);

        // Dibujar conexiones con hijo izquierdo
        if (nodo.getIzquierdo() != null) {
            Double xIzq = posiciones.get(nodo.getIzquierdo());
            if (xIzq != null) {
                // Calcular Y del hijo izquierdo
                double yIzq = nodo.getIzquierdo().esHoja()
                        ? yBase
                        : yBase - (nodo.getIzquierdo().getDistancia() * factorEscala);

                // Dibujar línea horizontal desde este nodo hacia la izquierda
                gc.strokeLine(xNodo, yNodo, xIzq, yNodo);
                // Dibujar línea vertical hacia abajo hasta el hijo
                gc.strokeLine(xIzq, yNodo, xIzq, yIzq);

                // Recursión para dibujar subárbol izquierdo
                dibujarNodo(gc, nodo.getIzquierdo(), posiciones, yBase, factorEscala, margenSup);
            }
        }

        // Dibujar conexiones con hijo derecho
        if (nodo.getDerecho() != null) {
            Double xDer = posiciones.get(nodo.getDerecho());
            if (xDer != null) {
                // Calcular Y del hijo derecho
                double yDer = nodo.getDerecho().esHoja()
                        ? yBase
                        : yBase - (nodo.getDerecho().getDistancia() * factorEscala);

                // Dibujar línea horizontal desde este nodo hacia la derecha
                gc.strokeLine(xNodo, yNodo, xDer, yNodo);
                // Dibujar línea vertical hacia abajo hasta el hijo
                gc.strokeLine(xDer, yNodo, xDer, yDer);

                // Recursión para dibujar subárbol derecho
                dibujarNodo(gc, nodo.getDerecho(), posiciones, yBase, factorEscala, margenSup);
            }
        }
    }

    /**
     * Exporta el dendrograma a formato JSON
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
                mostrarInfo("Éxito", "Dendrograma exportado correctamente a:\n" +
                        archivo.getName());
            } catch (IOException ex) {
                mostrarError("Error al exportar", ex.getMessage());
            }
        }
    }

    /**
     * Muestra un diálogo de error
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra un diálogo de información
     */
    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}