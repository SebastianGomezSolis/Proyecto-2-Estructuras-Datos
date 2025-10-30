package com.sistema.proyecto2estructurasdatos.controller; // Paquete donde vive esta clase

// Importamos lo que necesitamos del proyecto y de JavaFX
import com.sistema.proyecto2estructurasdatos.modelo.*;
import com.sistema.proyecto2estructurasdatos.Formato.CSV;               // Para leer el archivo CSV
import com.sistema.proyecto2estructurasdatos.Formato.ResultadoCSV;     // Resultado de la lectura del CSV
import com.sistema.proyecto2estructurasdatos.Formato.JSON;             // Para exportar a JSON
import com.sistema.proyecto2estructurasdatos.logica.*;                 // Lógica de normalización, distancia y clustering
import javafx.concurrent.Task;                                          // Tareas en segundo plano (para no trabar la ventana)
import javafx.fxml.FXML;                                                // Para enlazar con el FXML
import javafx.scene.canvas.Canvas;                                      // Lienzo donde dibujamos
import javafx.scene.canvas.GraphicsContext;                             // “Lápiz” para dibujar en el lienzo
import javafx.scene.control.*;                                          // Botones, etiquetas, cuadros de texto, etc.
import javafx.scene.layout.VBox;                                        // Contenedor vertical
import javafx.scene.paint.Color;                                        // Colores para el dibujo
import javafx.stage.FileChooser;                                        // Diálogo para elegir/guardar archivos

import java.io.File;                                                    // Representa un archivo en disco
import java.io.IOException;                                             // Posibles errores al leer/escribir
import java.util.HashMap;
import java.util.Map;

// Controlador de la ventana principal: maneja botones, lectura de datos y el dibujo
public class VentanaController {
    // Estos @FXML se conectan con los controles del archivo .fxml
    @FXML private ComboBox<String> comboNormalizacion;  // Lista para elegir cómo “acomodar” los datos
    @FXML private ComboBox<String> comboDistancia;      // Lista para elegir cómo medir “qué tan parecidos” son
    @FXML private Button btnCargarCSV;                  // Botón para abrir un CSV
    @FXML private Button btnGenerar;                    // Botón para crear el dendrograma
    @FXML private Button btnExportar;                   // Botón para guardar el árbol como JSON
    @FXML private Label lblArchivo;                     // Muestra el nombre del archivo elegido
    @FXML private Canvas canvasDendrograma;             // Lugar donde dibujamos el dendrograma
    @FXML private VBox vboxColumnas;                    // Aquí listamos las columnas con checks y pesos
    @FXML private ProgressIndicator progress;           // Ruedita de “cargando…”

    // Variables para guardar lo que leímos y lo que calculamos
    private ResultadoCSV datosCSV;                      // Aquí vive el contenido del CSV
    private ArbolBinario dendrograma;                   // Aquí vive el árbol resultante del clustering

    @FXML
    public void initialize() {                          // Se ejecuta al iniciar la ventana
        // Llenamos el combo de normalización con opciones y dejamos una por defecto
        comboNormalizacion.getItems().addAll("Min-Max", "Z-Score", "Logarítmica");
        comboNormalizacion.setValue("Min-Max");

        // Llenamos el combo de distancia con opciones y dejamos una por defecto
        comboDistancia.getItems().addAll("Euclidiana", "Manhattan", "Coseno", "Hamming");
        comboDistancia.setValue("Euclidiana");

        // Al comienzo no se puede generar ni exportar (aún no hay datos)
        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);

        // Ocultamos la ruedita hasta que haga falta
        if (progress != null) progress.setVisible(false);

        // Conectamos los botones con lo que deben hacer
        btnCargarCSV.setOnAction(e -> cargarCSV());         // Al hacer clic, pedimos el archivo y lo leemos
        btnGenerar.setOnAction(e -> generarDendrograma());
        btnExportar.setOnAction(e -> exportarJSON());
    }

    // Abre un diálogo para escoger un CSV y lo carga en segundo plano
    private void cargarCSV() {
        FileChooser fc = new FileChooser();                             // Ventana para escoger el archivo
        fc.setTitle("Seleccionar archivo CSV");                         // Título de la ventanita
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV", "*.csv")); // Solo .csv
        File file = fc.showOpenDialog(btnCargarCSV.getScene().getWindow()); // Abrimos el diálogo

        if (file == null) return;                                       // Si cancelaron, no seguimos

        // Mientras cargamos, apagamos los botones para evitar problemas de doble clic
        btnCargarCSV.setDisable(true);
        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);

        // Mostramos la ruedita y la ponemos “girando”
        if (progress != null) {
            progress.setVisible(true);
            progress.setProgress(-1); // -1 significa “indefinido”, o sea, sigue cargando
        }

        // Mostramos el nombre del archivo que estamos cargando
        lblArchivo.setText("Cargando: " + file.getName());

        // Creamos una tarea para leer el archivo sin congelar la pantalla
        Task<ResultadoCSV> task = new Task<>() {
            @Override
            protected ResultadoCSV call() throws Exception {
                // Leemos el CSV y devolvemos lo que encontramos
                return CSV.leer(file.getAbsolutePath());
            }
        };

        // Si la lectura salió bien:
        task.setOnSucceeded(e -> {
            ResultadoCSV r = task.getValue();          // Tomamos el resultado
            this.datosCSV = r;                         // Lo guardamos para usarlo luego
            lblArchivo.setText(file.getName());        // Mostramos el nombre limpio
            generarControlesColumnas();                // Creamos checks y pesos para las columnas

            // Contamos cuántas filas y columnas se cargaron y lo avisamos
            mostrarInfo("CSV cargado",
                    "Filas: " + r.numFilas + " | Columnas: " + r.numColumnas);

            // Quitamos la ruedita y dejamos listo el botón de generar
            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnExportar.setDisable(true);              // Aún no hay árbol, por eso no exportamos
            btnCargarCSV.setDisable(false);            // Ya se puede cargar otro si quieren
        });

        // Si algo falló al leer:
        task.setOnFailed(e -> {
            if (progress != null) progress.setVisible(false);  // Quitamos la ruedita
            btnCargarCSV.setDisable(false);                    // Rehabilitamos los botones
            btnGenerar.setDisable(true);
            btnExportar.setDisable(true);

            Throwable ex = task.getException();                // Vemos qué pasó
            mostrarError("Error cargando CSV",
                    (ex != null ? ex.getMessage() : "Fallo desconocido")); // Contamos el error de forma simple
            if (ex != null) ex.printStackTrace();              // Lo mostramos en consola por si ayuda

            lblArchivo.setText("Sin seleccionar");             // Volvemos al estado inicial
        });

        // Iniciamos la lectura en un hilo aparte, con nombre para ubicarlo si hace falta
        new Thread(task, "csv-loader").start();
    }

    // Crea una lista de columnas con un check (incluir/excluir) y un campo para su peso (importancia)
    private void generarControlesColumnas() {
        vboxColumnas.getChildren().clear();                // Limpiamos por si ya había algo
        if (datosCSV == null || datosCSV.nombresColumnas == null) return; // Si no hay columnas, salimos

        int numColumnas = datosCSV.nombresColumnas.tamanio();             // Cuántas columnas hay
        int numFilas = Math.min(datosCSV.datos.tamanio(), 50);            // Tope rápido (aquí ya no se usa)

        // Estas columnas las tratamos como etiquetas o nombres, no como números para calcular
        String[] columnasEtiqueta = {"title", "name", "nombre", "id", "original_title"}; // Se ignoran en el cálculo

        // Recorremos todas las columnas
        for (int i = 0; i < numColumnas; i++) {
            String nombre = datosCSV.nombresColumnas.obtener(i);          // Nombre de la columna

            // Revisamos si esta columna es una etiqueta (para saltarla)
            boolean esEtiqueta = false;
            for (String etiq : columnasEtiqueta) {
                if (nombre.equalsIgnoreCase(etiq)) {
                    esEtiqueta = true;
                    break;
                }
            }

            if (esEtiqueta) continue; // Si es etiqueta, no la mostramos para el cálculo

            // Creamos el check para activar/desactivar la columna
            CheckBox chk = new CheckBox(nombre);
            chk.setSelected(true);                         // Por defecto, todas vienen activadas
            chk.setStyle("-fx-font-size: 9px;");          // Letra pequeña para que quepa mejor

            // Campo para escribir el peso (importancia) de esta columna
            TextField txtPeso = new TextField("1.0");     // 1.0 significa “peso normal”
            txtPeso.setPrefWidth(50);                     // Ancho pequeño para que no ocupe mucho
            txtPeso.setStyle("-fx-font-size: 9px; -fx-alignment: center;"); // Letra pequeña y centrado

            // Metemos el check y el peso en una cajita vertical y la agregamos a la lista
            VBox box = new VBox(2, chk, txtPeso);
            vboxColumnas.getChildren().add(box);
        }
    }

    // Toma los datos, corre el algoritmo y dibuja el dendrograma
    private void generarDendrograma() {
        // Primero revisamos que sí haya datos
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0) {
            mostrarError("Sin datos", "Carga un CSV válido antes de generar.");
            return;
        }
        final int n = datosCSV.datos.tamanio();           // Cuántas filas tenemos

        // Tomamos TODAS las filas del CSV para el cálculo
        Lista<Dato> datosUsar = new Lista<>();
        for (int i = 0; i < n; i++) {
            datosUsar.agregar(datosCSV.datos.obtener(i));
        }

        // Actualizamos la etiqueta para decir que usamos todo
        lblArchivo.setText(lblArchivo.getText().split(" \\(")[0] + " (muestra " + n + "/" + n + ")");

        // Mientras corre el cálculo, desactivamos botones para evitar clics extra
        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);

        // Mostramos la ruedita
        if (progress != null) {
            progress.setVisible(true);
            progress.setProgress(-1);
        }

        // Leemos lo que la persona eligió en los combos y lo que escribió en pesos/checkboxes
        final String nomSel = comboNormalizacion.getValue(); // Cómo “acomodar” los datos antes de comparar
        final String disSel = comboDistancia.getValue();     // Cómo medir qué tan parecidos son
        final double[] pesos = obtenerPesos();               // Importancia de cada columna
        final boolean[] ignoradas = obtenerColumnasIgnoradas(); // Cuáles columnas se desactivaron

        // Creamos una tarea para hacer el cálculo sin congelar la ventana
        Task<ArbolBinario> task = new Task<>() {
            @Override
            protected ArbolBinario call() {
                // Pedimos una “forma de acomodar” y una “forma de medir” según lo que eligieron
                INormalizacion normalizacion = FactoryNormalizacion.obtenerInstancia().crear(nomSel);
                IDistancia distancia = FactoryDistancia.obtenerInstancia().crear(disSel);

                // Armamos el algoritmo con esas reglas y con los pesos/ignorar
                AlgoritmoClustering algoritmo = new AlgoritmoClustering(
                        normalizacion, distancia, pesos, ignoradas
                );

                ArbolBinario arbol = algoritmo.ejecutar(datosUsar); // Aquí se hace el “agrupado”
                return arbol; // Devolvemos el árbol para que lo dibujen
            }
        };

        // Si salió bien, guardamos y dibujamos el dendrograma
        task.setOnSucceeded(ev -> {
            this.dendrograma = task.getValue(); // Guardamos el resultado
            dibujarDendrograma();               // Lo dibujamos en el canvas

            // Quitamos la ruedita y reactivamos botones
            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnExportar.setDisable(false);      // Ya hay algo para exportar

            // Avisamos cuántos elementos se usaron
            mostrarInfo("Éxito", "Dendrograma generado con " + n + " elementos.");
        });

        // Si hubo un error, lo contamos y restauramos la interfaz
        task.setOnFailed(ev -> {
            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnExportar.setDisable(true);

            Throwable ex = task.getException();
            mostrarError("Error al generar", ex != null ? ex.getMessage() : "Fallo desconocido");
            if (ex != null) ex.printStackTrace();
        });

        // Arrancamos la tarea en un hilo con nombre
        new Thread(task, "cluster-worker").start();
    }

    // Lee los pesos de cada columna desde los campos de texto
    private double[] obtenerPesos() {
        // Si no hay datos, devolvemos un arreglo vacío
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0)
            return new double[0];

        // “dimensionReal” es cuántos números tiene cada fila realmente
        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        double[] pesos = new double[dimensionReal]; // Aquí guardaremos los pesos

        int idx = 0; // Posición en el arreglo de pesos
        // Recorremos cada cajita (VBox) que tiene un CheckBox y un TextField
        for (javafx.scene.Node nodo : vboxColumnas.getChildren()) {
            if (nodo instanceof VBox box && box.getChildren().size() >= 2) {
                TextField txtPeso = (TextField) box.getChildren().get(1); // El segundo hijo es el peso
                double peso = 1.0;                                        // Valor por defecto
                try {
                    peso = Double.parseDouble(txtPeso.getText());         // Intentamos leer lo que escribieron
                } catch (NumberFormatException ignored) {}                // Si está mal escrito, dejamos 1.0

                if (idx < pesos.length) pesos[idx++] = peso;              // Guardamos el peso y avanzamos
            }
        }

        // Si por cualquier razón faltaron, completamos con 1.0
        while (idx < dimensionReal) pesos[idx++] = 1.0;

        return pesos; // Devolvemos todos los pesos
    }

    // Indica qué columnas se ignoran (true = ignorada) según el estado de cada check
    private boolean[] obtenerColumnasIgnoradas() {
        // Si no hay datos, devolvemos un arreglo vacío
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0)
            return new boolean[0];

        // “dimensionReal” es cuántos números tiene cada fila realmente
        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        boolean[] ignoradas = new boolean[dimensionReal]; // true/false por cada columna

        int idx = 0; // Posición en el arreglo de ignoradas
        // Recorremos cada cajita (VBox) que tiene al menos el CheckBox
        for (javafx.scene.Node nodo : vboxColumnas.getChildren()) {
            if (nodo instanceof VBox box && box.getChildren().size() >= 1) {
                CheckBox chk = (CheckBox) box.getChildren().get(0);       // El primero es el check
                if (idx < ignoradas.length) ignoradas[idx++] = !chk.isSelected(); // Si no está marcado, se ignora
            }
        }

        // Si faltaron, las marcamos como NO ignoradas
        while (idx < dimensionReal) ignoradas[idx++] = false;

        return ignoradas; // Devolvemos la lista de sí/no
    }

    // Dibuja las “ramas” del dendrograma de forma recursiva (de arriba hacia abajo)
    private void trazarRamasEnCamino(NodoArbol nodo, Map<NodoArbol, Double> xmap, double yBase, double factorEscala, GraphicsContext gc) {
        if (nodo == null || nodo.esHoja()) return;       // Si no hay nada o es hoja, no trazamos más

        Double xNodo = xmap.get(nodo);                    // Posición horizontal del nodo actual
        if (xNodo == null) return;                        // Si no la tenemos, salimos

        double yNodo = yBase - (nodo.getDistancia() * factorEscala); // Altura según su “distancia”

        // Parte izquierda
        NodoArbol izq = nodo.getIzquierdo();
        if (izq != null) {
            Double xIzq = xmap.get(izq);                  // Posición horizontal del hijo izquierdo
            if (xIzq != null) {
                // Si es hoja, la altura es la base; si no, calculamos su altura
                double yIzq = izq.esHoja() ? yBase : yBase - (izq.getDistancia() * factorEscala);
                gc.strokeLine(xIzq, yIzq, xIzq, yNodo);    // Línea vertical que sube
                trazarRamasEnCamino(izq, xmap, yBase, factorEscala, gc); // Repetimos con sus hijos
            }
        }

        // Parte derecha
        NodoArbol der = nodo.getDerecho();
        if (der != null) {
            Double xDer = xmap.get(der);                  // Posición horizontal del hijo derecho
            if (xDer != null) {
                // Si es hoja, la altura es la base; si no, calculamos su altura
                double yDer = der.esHoja() ? yBase : yBase - (der.getDistancia() * factorEscala);
                gc.strokeLine(xDer, yDer, xDer, yNodo);    // Línea vertical que sube
                trazarRamasEnCamino(der, xmap, yBase, factorEscala, gc); // Repetimos con sus hijos
            }
        }

        // Línea horizontal que une izquierda con derecha a la altura del nodo actual
        if (izq != null && der != null) {
            Double xIzq = xmap.get(izq);
            Double xDer = xmap.get(der);
            if (xIzq != null && xDer != null) {
                gc.strokeLine(xIzq, yNodo, xDer, yNodo);
            }
        }
    }

    // Dibuja el dendrograma completo
    private void dibujarDendrograma() {
        if (dendrograma == null || dendrograma.getRaiz() == null) return; // Si no hay árbol, salimos

        GraphicsContext gc = canvasDendrograma.getGraphicsContext2D();
        double width = canvasDendrograma.getWidth();                       // Ancho del lienzo
        double height = canvasDendrograma.getHeight();                     // Alto del lienzo

        // Conseguimos las hojas (los elementos finales) en el orden en que deben verse
        Lista<NodoArbol> hojas = new Lista<>();
        obtenerHojasEnOrden(dendrograma.getRaiz(), hojas);
        int numHojas = hojas.tamanio();                                    // Cuántas hojas hay

        if (numHojas == 0) {
            mostrarError("Error", "No hay datos para visualizar");         // Si no hay nada, avisamos
            return;
        }

        // Márgenes para que el dibujo no pegue con los bordes
        double margenIzq = 70;
        double margenDer = 30;
        double margenSup = 40;
        double margenInf = 200;

        double anchoIdeal = numHojas * 30.0;                                // Ancho que ocuparían las etiquetas/hojas
        double anchoMinCanvas = 1100;                                       // Ancho mínimo cómodo

        // Si el dibujo necesita más ancho que el mínimo, agrandamos el lienzo
        if (anchoIdeal > anchoMinCanvas) {
            canvasDendrograma.setWidth(anchoIdeal);
            width = canvasDendrograma.getWidth();
        }

        // Pintamos un fondo clarito
        gc.setFill(Color.web("#f0f9ff"));
        gc.fillRect(0, 0, width, height);

        // Área realmente útil para dibujar (restando márgenes)
        double anchoUtil = width - margenIzq - margenDer;
        double alturaUtil = height - margenSup - margenInf;

        // Espacio horizontal entre cada hoja
        double espacioHorizontal = anchoUtil / Math.max(1, numHojas);

        // La “distancia” más grande del árbol nos sirve para escalar el dibujo en vertical
        double distanciaMaxima = Math.max(1e-9, dendrograma.getRaiz().getDistancia());
        double factorEscala = alturaUtil / distanciaMaxima;                 // Cuánto “pesa” cada unidad de distancia
        double yBase = margenSup + alturaUtil;                              // Línea de base (donde están las hojas)

        // Posición horizontal (X) de cada hoja
        Map<NodoArbol, Double> xmap = new HashMap<>();
        for (int i = 0; i < numHojas; i++) {
            NodoArbol hoja = hojas.obtener(i);
            double x = margenIzq + (i + 0.5) * espacioHorizontal;           // Las centramos en su “casilla”
            xmap.put(hoja, x);
        }

        // Calculamos la posición X de los nodos internos como el promedio de sus dos hijos
        calcularPosicionesInternas(dendrograma.getRaiz(), xmap);

        // Dibujamos una cuadrícula suave para ubicar alturas (como reglas)
        gc.setStroke(Color.web("#e5e7eb"));
        gc.setLineWidth(0.8);
        gc.setFill(Color.web("#6b7280"));
        gc.setFont(javafx.scene.text.Font.font("Arial", 9));

        int numLineas = 6;                                                  // Cantidad de líneas guía
        for (int i = 0; i <= numLineas; i++) {
            double dist = (distanciaMaxima * i) / numLineas;                // Valor de cada línea
            double y = margenSup + alturaUtil - (dist * factorEscala);      // Altura donde va esa línea
            gc.strokeLine(margenIzq, y, width - margenDer, y);              // Línea horizontal
            gc.fillText(String.format("%.2f", dist), margenIzq - 55, y + 4); // Etiqueta de la línea
        }

        // pintamos las ramas del árbol (las “uniones”)
        gc.setStroke(Color.web("#2dd4bf"));
        gc.setLineWidth(1.8);
        trazarRamasEnCamino(dendrograma.getRaiz(), xmap, yBase, factorEscala, gc);

        // Etiquetas de las hojas: las giramos para que quepan mejor
        gc.setFill(Color.web("#0f766e"));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 8));
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);

        int maxEtiquetas = Math.min(25, (int)(anchoUtil / 30.0));           // Mostramos solo algunas para no tapar
        int salto = Math.max(1, numHojas / Math.max(1, maxEtiquetas));      // Saltos entre etiquetas

        for (int i = 0; i < numHojas; i += salto) {
            NodoArbol hoja = hojas.obtener(i);                              // Tomamos una hoja
            double x = xmap.get(hoja);                                      // Su X
            double yTexto = yBase + 12;                                     // Un poquito debajo de la base

            gc.save();                // Guardamos el estado
            gc.translate(x, yTexto);  // Movemos el punto de escritura a donde va la etiqueta
            gc.rotate(90);            // Giramos el texto para que sea vertical
            gc.fillText(acortar(hoja.getEtiqueta(), 22), 0, 0); // Escribimos el nombre recortado si es muy largo
            gc.restore();             // Volvemos a la posición normal
        }

        // Línea base gruesa (donde se “apoyan” todas las hojas)
        gc.setStroke(Color.web("#374151"));
        gc.setLineWidth(2);
        gc.strokeLine(margenIzq, yBase, width - margenDer, yBase);

        // Título arriba con un mini resumen
        gc.setFill(Color.web("#00897b"));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 11));
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        gc.fillText("Dendrograma (" + numHojas + " elementos) | " +
                        comboDistancia.getValue() + " | " + comboNormalizacion.getValue(),
                margenIzq, 22);
    }

    // Si un texto es muy largo, lo recorta y agrega “…” al final
    private String acortar(String s, int max) {
        if (s == null) return "";                           // Si no hay texto, devolvemos vacío
        return s.length() <= max ? s : (s.substring(0, max - 1) + "…");
    }

    // Recorre el árbol y va guardando todas las hojas en orden de izquierda a derecha
    private void obtenerHojasEnOrden(NodoArbol nodo, Lista<NodoArbol> hojas) {
        if (nodo == null) return;                           // Nada que hacer
        if (nodo.esHoja()) {                                // Si es hoja, la guardamos
            hojas.agregar(nodo);
            return;
        }
        // Si no, seguimos por izquierda y luego por derecha
        obtenerHojasEnOrden(nodo.getIzquierdo(), hojas);
        obtenerHojasEnOrden(nodo.getDerecho(), hojas);
    }

    // Calcula la posición X de cada nodo interno como el promedio de sus dos hijos
    private void calcularPosicionesInternas(NodoArbol nodo, Map<NodoArbol, Double> posiciones) {
        if (nodo == null || nodo.esHoja()) return;          // Las hojas ya tienen su X, aquí no hacemos nada

        // Primero calculamos para los hijos (recursivo)
        calcularPosicionesInternas(nodo.getIzquierdo(), posiciones);
        calcularPosicionesInternas(nodo.getDerecho(), posiciones);

        // Luego sacamos el promedio de las X de sus dos hijos
        Double xIzq = posiciones.get(nodo.getIzquierdo());
        Double xDer = posiciones.get(nodo.getDerecho());

        if (xIzq != null && xDer != null) {
            posiciones.put(nodo, (xIzq + xDer) / 2.0);
        }
    }

    // Abre un diálogo para guardar el árbol como JSON
    private void exportarJSON() {
        if (dendrograma == null) {                          // Si no hay árbol, no hay qué guardar
            mostrarError("Error", "No hay dendrograma para exportar");
            return;
        }

        FileChooser fileChooser = new FileChooser();        // Ventana para elegir dónde guardar
        fileChooser.setTitle("Guardar dendrograma");        // Título de la ventanita
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo JSON", "*.json") // Sólo .json
        );
        fileChooser.setInitialFileName("dendrograma.json"); // Nombre sugerido

        File archivo = fileChooser.showSaveDialog(btnExportar.getScene().getWindow()); // Abrimos el diálogo

        if (archivo != null) {                              // Si escogieron un lugar:
            try {
                JSON.exportar(dendrograma, archivo.getAbsolutePath()); // Guardamos el árbol
                mostrarInfo("Éxito", "Dendrograma exportado a:\n" +
                        archivo.getName());                // Avisamos que se guardó
            } catch (IOException ex) {                     // Si hubo un problema al escribir:
                mostrarError("Error al exportar", ex.getMessage());
            }
        }
    }

    // Muestra un mensaje de error en una ventanita simple
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);     // Tipo “error”
        alert.setTitle(titulo);                             // Título de la ventana
        alert.setHeaderText(null);                          // Sin encabezado extra
        alert.setContentText(mensaje);                      // El mensaje principal
        alert.showAndWait();                                // Mostrar y esperar que cierren
    }

    // Muestra un mensaje informativo en una ventanita simple
    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION); // Tipo “información”
        alert.setTitle(titulo);                               // Título de la ventana
        alert.setHeaderText(null);                            // Sin encabezado extra
        alert.setContentText(mensaje);                        // El mensaje principal
        alert.showAndWait();                                  // Mostrar y esperar que cierren
    }
}
