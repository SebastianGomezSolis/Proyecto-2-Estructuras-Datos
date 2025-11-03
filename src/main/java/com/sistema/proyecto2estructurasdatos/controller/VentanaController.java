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

/**
 * Controlador principal de la ventana.
 */

public class VentanaController {

    /** Lista desplegable para escoger cómo normalizar los datos numéricos. */
    @FXML private ComboBox<String> comboNormalizacion;

    /** Lista desplegable para escoger la métrica de distancia entre registros. */
    @FXML private ComboBox<String> comboDistancia;

    /** Botón para abrir el archivo CSV desde el equipo. */
    @FXML private Button btnCargarCSV;

    /** Botón para ejecutar el clustering y generar el dendrograma. */
    @FXML private Button btnGenerar;

    /** Botón para exportar el árbol resultante a un archivo JSON. */
    @FXML private Button btnExportar;

    /** Etiqueta que muestra el nombre del archivo y/o detalles de la muestra. */
    @FXML private Label lblArchivo;

    /** Lienzo donde se dibuja el dendrograma. */
    @FXML private Canvas canvasDendrograma;

    /**
     * Contenedor vertical donde, por cada columna útil del CSV,
     * se crea un par de controles:
     *  - CheckBox (usar/ignorar la columna)
     *  - TextField (peso numérico de la columna)
     */
    @FXML private VBox vboxColumnas;

    //Indicador visual que aparece mientras se realizan tareas pesadas. *
    @FXML private ProgressIndicator progress;

    //Resultado de la lectura del CSV: nombres de columnas, matriz de datos, etc.
    private ResultadoCSV datosCSV;

    // Árbol binario que representa el dendrograma final (raíz + nodos).
    private ArbolBinario dendrograma;

    /**
     * Se ejecuta automáticamente al crear la ventana.
     * Aquí configuramos opciones por defecto y enlazamos botones a sus acciones.
     */
    @FXML
    public void initialize() {
        // Cargamos las opciones de normalización. El valor por defecto es "Min-Max".
        comboNormalizacion.getItems().addAll("Min-Max", "Z-Score", "Logarítmica");
        comboNormalizacion.setValue("Min-Max");

        // Cargamos las opciones de distancia. El valor por defecto es "Euclidiana".
        comboDistancia.getItems().addAll("Euclidiana", "Manhattan", "Coseno", "Hamming");
        comboDistancia.setValue("Euclidiana");

        // Inicialmente no se puede generar ni exportar porque no hay datos cargados.
        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);

        // Ocultamos el indicador de progreso. Solo se muestra cuando hay trabajo en curso.
        if (progress != null) progress.setVisible(false);

        // Conectamos los botones con sus métodos manejadores.
        btnCargarCSV.setOnAction(e -> cargarCSV());
        btnGenerar.setOnAction(e -> generarDendrograma());
        btnExportar.setOnAction(e -> exportarJSON());
    }

    /**
     * Abre un cuadro de diálogo para seleccionar un archivo CSV y lo carga.
     * La lectura se hace en un hilo aparte para que la interfaz no se congele.
     */
    private void cargarCSV() {
        //Abrimos selector de archivos restringido a ".csv"
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));

        //Mostramos el diálogo y capturamos el archivo elegido
        File file = fc.showOpenDialog(btnCargarCSV.getScene().getWindow());
        if (file == null) return; // Si el usuario canceló, salimos sin hacer nada.

        //Mientras leemos, bloqueamos acciones que dependen del CSV
        btnCargarCSV.setDisable(true);
        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);

        //Mostramos el indicador de progreso en modo "indeterminado"
        if (progress != null) {
            progress.setVisible(true);
            progress.setProgress(-1); // -1 indica animación giratoria sin porcentaje
        }

        //Actualizamos etiqueta para informar qué archivo está cargando
        lblArchivo.setText("Cargando: " + file.getName());

        //Creamos una tarea para leer el CSV en segundo plano
        Task<ResultadoCSV> task = new Task<>() {
            @Override
            protected ResultadoCSV call() throws Exception {
                // La lectura real del archivo: retorna estructura resultado con datos y metadatos
                return CSV.leer(file.getAbsolutePath());
            }
        };

        //Si la lectura se completa con éxito:
        task.setOnSucceeded(e -> {
            ResultadoCSV r = task.getValue();
            this.datosCSV = r; // guardamos el resultado para usos posteriores

            // Mostramos el nombre base del archivo (ya leído)
            lblArchivo.setText(file.getName());

            // Creamos controles por columna (check + peso) según el CSV cargado
            generarControlesColumnas();

            // Mensaje de confirmación con resumen (filas y columnas detectadas)
            mostrarInfo("CSV cargado", "Filas: " + r.numFilas + " | Columnas: " + r.numColumnas);

            // Ocultamos indicador y liberamos botones coherentes con el estado actual
            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);  // Ya se puede generar el dendrograma
            btnExportar.setDisable(true);  // Aún no hay nada que exportar
            btnCargarCSV.setDisable(false);
        });

        // Si ocurre un error durante la lectura:
        task.setOnFailed(e -> {
            if (progress != null) progress.setVisible(false);

            // Permitimos volver a intentar cargar otro archivo
            btnCargarCSV.setDisable(false);
            btnGenerar.setDisable(true);
            btnExportar.setDisable(true);

            // Mostramos el mensaje de error más claro posible
            Throwable ex = task.getException();
            mostrarError("Error cargando CSV", (ex != null ? ex.getMessage() : "Fallo desconocido"));
            if (ex != null) ex.printStackTrace();

            // Volvemos la etiqueta a un estado neutro
            lblArchivo.setText("Sin seleccionar");
        });

        // Iniciamos la tarea en un hilo independiente
        new Thread(task, "csv-loader").start();
    }

    /**
     * Crea dinámicamente, para cada columna útil, dos controles:
     * - CheckBox para decidir si la columna participa en el cálculo de distancias.
     * - TextField para asignar un peso (importancia relativa).
     *
     * NOTA: se omiten columnas típicamente descriptivas (id, nombres, títulos) que no
     *       aportan a distancia numérica, para reducir "errores".
     */
    private void generarControlesColumnas() {
        // Limpiamos cualquier contenido previo (por si el usuario recarga otro CSV).
        vboxColumnas.getChildren().clear();
        if (datosCSV == null || datosCSV.nombresColumnas == null) return;

        int numColumnas = datosCSV.nombresColumnas.tamanio();

        // Lista de columnas que usualmente son etiquetas y no métricas
        String[] columnasEtiqueta = {"title", "name", "nombre", "id", "original_title"};

        for (int i = 0; i < numColumnas; i++) {
            String nombre = datosCSV.nombresColumnas.obtener(i);

            // Verificamos si la columna es de etiqueta. Si lo es, la saltamos.
            boolean esEtiqueta = false;
            for (String etiq : columnasEtiqueta) {
                if (nombre.equalsIgnoreCase(etiq)) { esEtiqueta = true; break; }
            }
            if (esEtiqueta) continue;

            // CheckBox para activar/desactivar el uso de esta columna
            CheckBox chk = new CheckBox(nombre);
            chk.setSelected(true);               // Por defecto se usan todas
            chk.setStyle("-fx-font-size: 9px;"); // Tamaño de fuente compacto

            // TextField para el peso de la columna (1.0 significa importancia estándar)
            TextField txtPeso = new TextField("1.0");
            txtPeso.setPrefWidth(50);
            txtPeso.setStyle("-fx-font-size: 9px; -fx-alignment: center;");

            // Agrupamos ambos en una cajita vertical y la añadimos al panel
            VBox box = new VBox(2, chk, txtPeso);
            vboxColumnas.getChildren().add(box);
        }
    }

    /**
     * proceso de clustering jerárquico.
     * Puntos clave:
     * - Se valida que haya datos.
     * - Se leen opciones de la interfaz (normalización, distancia, pesos, ignoradas).
     * - Se corre en segundo plano para no congelar la ventana.
     * - Al terminar, se dibuja el dendrograma en el canvas.
     */
    private void generarDendrograma() {
        // Validamos que existan datos útiles en memoria
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0) {
            mostrarError("Sin datos", "Carga un CSV válido antes de generar.");
            return;
        }

        final int n = datosCSV.datos.tamanio();

        // Copiamos los registros a una lista independiente (por claridad y posible filtrado futuro)
        Lista<Dato> datosUsar = new Lista<>();
        for (int i = 0; i < n; i++) datosUsar.agregar(datosCSV.datos.obtener(i));

        // Actualizamos la etiqueta con una pequeña nota de muestra usada
        lblArchivo.setText(lblArchivo.getText().split(" \\(")[0] + " (muestra " + n + "/" + n + ")");

        // Mientras se calcula, bloqueamos acciones y mostramos progreso
        btnGenerar.setDisable(true);
        btnExportar.setDisable(true);
        if (progress != null) { progress.setVisible(true); progress.setProgress(-1); }

        // Leemos parámetros elegidos por el usuario en la UI
        final String nomSel = comboNormalizacion.getValue();
        final String disSel = comboDistancia.getValue();
        final double[] pesos = obtenerPesos();                 // Importancia por columna
        final boolean[] ignoradas = obtenerColumnasIgnoradas();// Columnas marcadas como ignoradas

        // Definimos la tarea pesada (clustering) en un hilo aparte
        Task<ArbolBinario> task = new Task<>() {
            @Override
            protected ArbolBinario call() {
                // Obtenemos las estrategias concretas a partir de las "fabricas"
                INormalizacion normalizacion = FactoryNormalizacion.obtenerInstancia().crear(nomSel);
                IDistancia distancia = FactoryDistancia.obtenerInstancia().crear(disSel);

                // Armamos el algoritmo con todo lo configurado
                AlgoritmoClustering algoritmo =
                        new AlgoritmoClustering(normalizacion, distancia, pesos, ignoradas);

                // MUY IMPORTANTE: solo normalizar las columnas numéricas reales
                algoritmo.setColumnasNumericas(datosCSV.columnasNumericas);

                // Ejecutamos el clustering y devolvemos el árbol (dendrograma)
                return algoritmo.ejecutar(datosUsar);
            }
        };

        // Si la tarea termina bien:
        task.setOnSucceeded(ev -> {
            this.dendrograma = task.getValue(); // Guardamos el árbol resultante
            dibujarDendrograma();               // Lo pintamos en el canvas

            // Restablecemos botones y ocultamos progreso
            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnExportar.setDisable(false);      // Ya hay algo para exportar

            mostrarInfo("Éxito", "Dendrograma generado con " + n + " elementos.");
        });

        // Si la tarea falla:
        task.setOnFailed(ev -> {
            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnExportar.setDisable(true);

            Throwable ex = task.getException();
            mostrarError("Error al generar", ex != null ? ex.getMessage() : "Fallo desconocido");
            if (ex != null) ex.printStackTrace();
        });

        // Lanzamos la ejecución en un nuevo hilo
        new Thread(task, "cluster-worker").start();
    }

    /**
     * Recorre los TextField de la interfaz y convierte sus contenidos a números.
     * - Si el usuario no escribe un número válido, se usa 1.0 por defecto.
     * - Si hay menos pesos que columnas reales, se rellenan con 1.0.
     */
    private double[] obtenerPesos() {
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0)
            return new double[0];

        // Cantidad de columnas reales en el vector original de cada registro
        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        double[] pesos = new double[dimensionReal];

        int idx = 0;
        // Por cada "cajita" (VBox) en el panel, leemos el segundo control (TextField del peso)
        for (javafx.scene.Node nodo : vboxColumnas.getChildren()) {
            if (nodo instanceof VBox box && box.getChildren().size() >= 2) {
                TextField txtPeso = (TextField) box.getChildren().get(1);
                double peso = 1.0; // valor por defecto
                try {
                    peso = Double.parseDouble(txtPeso.getText().trim());
                } catch (NumberFormatException ignored) {
                    // Si el valor no es numérico, mantenemos 1.0
                }
                if (idx < pesos.length) pesos[idx++] = peso;
            }
        }

        // Si faltaron pesos (menos controles que columnas), completamos con 1.0
        while (idx < dimensionReal) pesos[idx++] = 1.0;

        return pesos;
    }

    /**
     * Recorre los CheckBox de la interfaz y marca como "ignoradas" las columnas desmarcadas.
     * - true  = ignorar la columna en el cálculo de distancias.
     * - false = usar la columna.
     * Si faltan casillas para todas las columnas, el resto se asume "usar" (false).
     */
    private boolean[] obtenerColumnasIgnoradas() {
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0)
            return new boolean[0];

        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        boolean[] ignoradas = new boolean[dimensionReal];

        int idx = 0;
        // Por cada "cajita" (VBox) leemos el primer control (CheckBox de uso)
        for (javafx.scene.Node nodo : vboxColumnas.getChildren()) {
            if (nodo instanceof VBox box && box.getChildren().size() >= 1) {
                CheckBox chk = (CheckBox) box.getChildren().get(0);
                // Si está desmarcado => ignorar = true
                if (idx < ignoradas.length) ignoradas[idx++] = !chk.isSelected();
            }
        }

        // Si faltaron casillas, asumimos que esas columnas NO se ignoran
        while (idx < dimensionReal) ignoradas[idx++] = false;

        return ignoradas;
    }

    /**
     * Dibuja recursivamente las ramas de un nodo hacia sus hijos.
     * La idea es formar la típica "U" invertida:
     * - Una línea vertical que sube desde cada hijo hasta la altura del padre.
     * - Una línea horizontal que une ambas verticales a la altura del padre.
     *
     *  nodo         nodo actual del árbol
     *  xmap         mapa con la posición X calculada para cada nodo
     *  yBase        altura (Y) en donde se ubican las hojas
     *  factorEscala factor para convertir la distancia del árbol a píxeles
     *  gc           pincel del canvas para dibujar líneas
     */
    private void trazarRamasEnCamino(NodoArbol nodo, HashMapa<NodoArbol, Double> xmap, double yBase, double factorEscala, GraphicsContext gc) {
        // Si el nodo es nulo o es hoja (no tiene hijos), no hay ramas que dibujar
        if (nodo == null || nodo.esHoja()) return;

        // Buscamos la X del nodo actual; si no la tenemos, no podemos continuar
        Double xNodo = xmap.obtener(nodo);
        if (xNodo == null) return;

        // La altura Y del nodo depende de su "distancia" en el clustering
        // A mayor distancia, más arriba en el dibujo
        double yNodo = yBase - (nodo.getDistancia() * factorEscala);

        // --- Hijo izquierdo ---
        NodoArbol izq = nodo.getIzquierdo();
        if (izq != null) {
            Double xIzq = xmap.obtener(izq);
            if (xIzq != null) {
                double yIzq = izq.esHoja() ? yBase : yBase - (izq.getDistancia() * factorEscala);
                // Línea vertical desde el hijo hasta la altura del padre
                gc.strokeLine(xIzq, yIzq, xIzq, yNodo);
                // Repetimos el proceso recursivamente para el subárbol izquierdo
                trazarRamasEnCamino(izq, xmap, yBase, factorEscala, gc);
            }
        }

        // --- Hijo derecho ---
        NodoArbol der = nodo.getDerecho();
        if (der != null) {
            Double xDer = xmap.obtener(der);
            if (xDer != null) {
                double yDer = der.esHoja() ? yBase : yBase - (der.getDistancia() * factorEscala);
                gc.strokeLine(xDer, yDer, xDer, yNodo);
                trazarRamasEnCamino(der, xmap, yBase, factorEscala, gc);
            }
        }

        // --- Unión horizontal entre ambos hijos ---
        if (izq != null && der != null) {
            Double xIzq = xmap.obtener(izq);
            Double xDer = xmap.obtener(der);
            if (xIzq != null && xDer != null) {
                gc.strokeLine(xIzq, yNodo, xDer, yNodo);
            }
        }
    }

    /**
     * Calcula la posición X de los nodos internos como el promedio de las X de sus hijos.
     * Esto centra visualmente cada unión sobre sus dos subramas.
     */
    private void calcularPosicionesInternas(NodoArbol nodo, HashMapa<NodoArbol, Double> xmap) {
        if (nodo == null || nodo.esHoja()) return;

        // Primero calculamos recursivamente en los hijos
        calcularPosicionesInternas(nodo.getIzquierdo(), xmap);
        calcularPosicionesInternas(nodo.getDerecho(), xmap);

        // Luego, si ambos hijos tienen posición, promediamos
        Double xi = xmap.obtener(nodo.getIzquierdo());
        Double xd = xmap.obtener(nodo.getDerecho());
        if (xi != null && xd != null) {
            xmap.insertar(nodo, (xi + xd) / 2.0);
        }
    }

    /**
     * Dibuja el dendrograma completo:
     * - Fondo
     * - Rejilla de referencia horizontal (valores de distancia)
     * - Ramas del árbol
     * - Etiquetas de las hojas (rotadas para ahorrar espacio)
     * - Título con las opciones usadas
     */
    private void dibujarDendrograma() {
        // Verificamos que exista un árbol válido para dibujar
        if (dendrograma == null || dendrograma.getRaiz() == null) return;

        GraphicsContext gc = canvasDendrograma.getGraphicsContext2D();
        double width  = canvasDendrograma.getWidth();
        double height = canvasDendrograma.getHeight();

        // Obtenemos todas las hojas (elementos finales) en orden de izquierda a derecha
        Lista<NodoArbol> hojas = new Lista<>();
        obtenerHojasEnOrden(dendrograma.getRaiz(), hojas);
        int numHojas = hojas.tamanio();
        if (numHojas == 0) {
            mostrarError("Error", "No hay datos para visualizar");
            return;
        }

        // Márgenes para dejar espacio a ejes, etiquetas y título
        double margenIzq = 70;
        double margenDer = 30;
        double margenSup = 40;
        double margenInf = 200; // espacio para etiquetas rotadas

        // Si hay muchas hojas, ampliamos el ancho del canvas para que no se amontonen
        double anchoIdeal = numHojas * 30.0; // ~30 píxeles por hoja
        double anchoMinCanvas = 1100;
        if (anchoIdeal > anchoMinCanvas) {
            canvasDendrograma.setWidth(anchoIdeal);
            width = canvasDendrograma.getWidth();
        }

        // Fondo suave para facilitar lectura
        gc.setFill(Color.web("#f0f9ff"));
        gc.fillRect(0, 0, width, height);

        // Área útil de dibujo (sin márgenes)
        double anchoUtil = width - margenIzq - margenDer;
        double alturaUtil = height - margenSup - margenInf;

        // Separación horizontal entre hojas
        double espacioHorizontal = anchoUtil / Math.max(1, numHojas);

        // Escala vertical: convierte la distancia máxima del árbol a píxeles disponibles
        double distanciaMaxima = Math.max(1e-9, dendrograma.getRaiz().getDistancia());
        double factorEscala = alturaUtil / distanciaMaxima;

        // Línea base donde se dibujan las hojas
        double yBase = margenSup + alturaUtil;

        // --- Asignamos posición X a cada hoja ---
        HashMapa<NodoArbol, Double> xmap = new HashMapa<>();
        for (int i = 0; i < numHojas; i++) {
            NodoArbol hoja = hojas.obtener(i);
            // Colocamos la hoja en el centro de su "casilla" horizontal
            double x = margenIzq + (i + 0.5) * espacioHorizontal;
            xmap.insertar(hoja, x);
        }

        // --- Calculamos posiciones X de nodos internos (promedio de sus hijos) ---
        calcularPosicionesInternas(dendrograma.getRaiz(), xmap);

        // --- Dibujamos rejilla horizontal y etiquetas de distancia ---
        gc.setStroke(Color.web("#e5e7eb"));
        gc.setLineWidth(0.8);
        gc.setFill(Color.web("#6b7280"));
        gc.setFont(javafx.scene.text.Font.font("Arial", 9));

        int numLineas = 6; // cantidad de líneas guía
        for (int i = 0; i <= numLineas; i++) {
            // Valor de distancia correspondiente a esa línea
            double dist = (distanciaMaxima * i) / numLineas;
            // Altura Y de la línea
            double y = margenSup + alturaUtil - (dist * factorEscala);

            gc.strokeLine(margenIzq, y, width - margenDer, y); // trazo de guía
            gc.fillText(String.format("%.2f", dist), margenIzq - 55, y + 4); // etiqueta a la izquierda
        }

        // --- Dibujamos las ramas del árbol ---
        gc.setStroke(Color.web("#2dd4bf"));
        gc.setLineWidth(1.8);
        trazarRamasEnCamino(dendrograma.getRaiz(), xmap, yBase, factorEscala, gc);

        // --- Etiquetas de hojas (rotadas para caber mejor) ---
        gc.setFill(Color.web("#0f766e"));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 8));
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);

        // Para evitar saturar, limitamos cuántas etiquetas mostramos si hay muchas hojas
        int maxEtiquetas = Math.min(25, (int)(anchoUtil / 30.0));
        int salto = Math.max(1, numHojas / Math.max(1, maxEtiquetas));

        for (int i = 0; i < numHojas; i += salto) {
            NodoArbol hoja = hojas.obtener(i);
            double x = xmap.obtener(hoja);
            double yTexto = yBase + 12; // posición inicial del texto (se rotará)

            gc.save();                 // guardamos estado gráfico
            gc.translate(x, yTexto);   // movemos origen al punto donde escribiremos
            gc.rotate(90);             // rotamos 90° para escribir vertical
            gc.fillText(acortar(hoja.getEtiqueta(), 28), 0, 0); // texto (corto para no tapar)
            gc.restore();              // restauramos estado
        }

        // --- Título superior con configuración usada ---
        gc.setFill(Color.web("#0f172a"));
        gc.setFont(javafx.scene.text.Font.font("Arial", 13));
        gc.fillText(
                "Dendrograma (distancia: " + comboDistancia.getValue() +
                        ", normalización: " + comboNormalizacion.getValue() + ")",
                margenIzq, 20
        );
    }

    /**
     * Recorre el árbol y agrega a la lista únicamente las hojas (sin hijos),
     * respetando el orden izquierdo → derecho, para que el dibujo sea coherente.
     */
    private void obtenerHojasEnOrden(NodoArbol nodo, Lista<NodoArbol> hojas) {
        if (nodo == null) return;
        if (nodo.esHoja()) { hojas.agregar(nodo); return; }
        obtenerHojasEnOrden(nodo.getIzquierdo(), hojas);
        obtenerHojasEnOrden(nodo.getDerecho(), hojas);
    }

    /**
     * Si una etiqueta es demasiado larga, la recortamos y agregamos "..."
     * para que no tape otros elementos del dibujo.
     */
    private String acortar(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    /**
     * Permite guardar el árbol (dendrograma) en un archivo JSON.
     * Primero valida que exista un árbol. Luego abre un diálogo para elegir ruta.
     */
    private void exportarJSON() {
        if (dendrograma == null || dendrograma.getRaiz() == null) {
            mostrarError("Nada que exportar", "Primero genera el dendrograma.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar dendrograma como JSON");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        fc.setInitialFileName("dendrograma.json");

        File file = fc.showSaveDialog(btnExportar.getScene().getWindow());
        if (file == null) return; // Si canceló, no se hace nada

        try {
            JSON.exportar(dendrograma, file.getAbsolutePath());
            mostrarInfo("Exportado", "Archivo guardado en: " + file.getName());
        } catch (IOException e) {
            mostrarError("Error al exportar", e.getMessage());
        }
    }

    /** Muestra una ventana de información con un texto simple. */
    private void mostrarInfo(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }

    /** Muestra una ventana de error con un texto simple. */
    private void mostrarError(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}
