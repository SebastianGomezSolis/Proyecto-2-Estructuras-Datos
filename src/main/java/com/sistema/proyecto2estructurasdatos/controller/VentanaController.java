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

// Controlador principal de la ventana para clustering jerárquico.
// Responsabilidades:
//  - Permitir al usuario cargar un archivo CSV.
//  - Mostrar dinámicamente las columnas del CSV para configurar:
//      - Qué variables se usan o se ignoran.
//      - Pesos de cada variable.
//      - Tipo de normalización por variable.
//  - Ejecutar el algoritmo de clustering jerárquico en segundo plano.
//  - Exportar el dendrograma resultante a formato JSON (para verlo en otro visor).
public class VentanaController {
    // Controles inyectados desde el FXML
    @FXML private ComboBox<String> comboDistancia;
    @FXML private Button btnCargarCSV;
    @FXML private Button btnGenerar;
    @FXML private Button btnDescargar;
    @FXML private Label lblArchivo;
    @FXML private Label lblEstado;
    @FXML private VBox vboxColumnas;
    @FXML private ProgressIndicator progress;

    // Datos cargados desde el CSV
    private ResultadoCSV datosCSV;
    // Árbol resultante del clustering
    private ArbolBinario dendrograma;
    // Último archivo JSON generado (para luego descargarlo donde el usuario elija)
    private File ultimoArchivoJSON;

    // Listas que guardan referencias a los controles dinámicos por columna
    private Lista<CheckBox> checkBoxesColumnas;
    private Lista<TextField> textFieldsPesos;
    private Lista<ComboBox<String>> combosNormalizacion;

    // Método de inicialización del controlador.
    // Se llama automáticamente después de cargar el FXML.
    @FXML
    public void initialize() {
        // Opciones disponibles para la distancia
        comboDistancia.getItems().addAll("Euclidiana", "Manhattan", "Coseno", "Hamming");
        comboDistancia.setValue("Euclidiana");

        // Estado inicial de botones: no se puede generar ni descargar sin CSV
        btnGenerar.setDisable(true);
        btnDescargar.setDisable(true);

        // Ocultar indicador de progreso al inicio
        if (progress != null) progress.setVisible(false);

        // Inicializar las listas que referencian controles por columna
        checkBoxesColumnas = new Lista<>();
        textFieldsPesos = new Lista<>();
        combosNormalizacion = new Lista<>();

        // Asociar acciones a los botones
        btnCargarCSV.setOnAction(e -> cargarCSV());
        btnGenerar.setOnAction(e -> generarDendrograma());
        btnDescargar.setOnAction(e -> descargarJSON());

        lblEstado.setText("Listo para cargar archivo CSV");
    }

    // Permite al usuario seleccionar un archivo CSV y lo carga en segundo plano.
    // Pasos:
    //  - Abre un FileChooser para seleccionar el CSV.
    //  - Ejecuta CSV.leer(...) en un Task para no bloquear la UI.
    //  - Cuando termina:
    //      * Guarda el ResultadoCSV.
    //      * Muestra info básica (filas, columnas).
    //      * Genera los controles dinámicos por columna.
    private void cargarCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo CSV");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos CSV", "*.csv")
        );

        File file = fc.showOpenDialog(btnCargarCSV.getScene().getWindow());
        if (file == null) return; // Usuario canceló

        // Deshabilitar acciones mientras se carga el archivo
        btnCargarCSV.setDisable(true);
        btnGenerar.setDisable(true);
        btnDescargar.setDisable(true);

        if (progress != null) {
            progress.setVisible(true);
            // -1 indica animación indeterminada (no se conoce el avance)
            progress.setProgress(-1);
        }

        lblArchivo.setText("Cargando: " + file.getName());
        lblEstado.setText("Leyendo archivo CSV...");

        // Tarea en segundo plano para no bloquear la interfaz gráfica
        Task<ResultadoCSV> task = new Task<>() {
            @Override
            protected ResultadoCSV call() throws Exception {
                return CSV.leer(file.getAbsolutePath());
            }
        };

        // Si la lectura fue exitosa
        task.setOnSucceeded(e -> {
            ResultadoCSV r = task.getValue();
            this.datosCSV = r;

            lblArchivo.setText(file.getName());
            lblEstado.setText(
                    String.format("CSV cargado: %d filas, %d columnas",
                            r.numFilas, r.numColumnas)
            );

            // Crear controles dinámicos para cada columna del CSV
            generarControlesColumnas();

            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);
            btnCargarCSV.setDisable(false);

            mostrarInfo("CSV cargado",
                    String.format("Filas: %d | Columnas: %d", r.numFilas, r.numColumnas));
        });

        // Si hubo error al leer el CSV
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

        // Lanzar la tarea en un hilo aparte
        new Thread(task, "csv-loader").start();
    }

    // Genera dinámicamente una "tabla" de controles para cada columna del CSV.
    // Por cada columna (que no sea de etiqueta):
    //  - CheckBox: incluir/ignorar variable.
    //  - TextField: peso numérico.
    //  - ComboBox: tipo de normalización (numéricas) o One-Hot (cualitativas).
    private void generarControlesColumnas() {
        // Limpiamos cualquier configuración anterior
        vboxColumnas.getChildren().clear();
        checkBoxesColumnas = new Lista<>();
        textFieldsPesos = new Lista<>();
        combosNormalizacion = new Lista<>();

        if (datosCSV == null || datosCSV.nombresColumnas == null) return;

        int numColumnas = datosCSV.nombresColumnas.tamanio();

        // Algunos nombres típicos de columnas que usaremos solo como etiqueta, no como variable
        String[] columnasEtiqueta = {"title", "name", "nombre", "id", "original_title"};

        // ===== Encabezado tipo tabla =====
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

        // ===== Fila por cada columna del CSV =====
        for (int i = 0; i < numColumnas; i++) {
            String nombre = datosCSV.nombresColumnas.obtener(i);

            // Saltar columnas que probablemente sean solo etiquetas/identificadores
            boolean esEtiqueta = false;
            for (String etiq : columnasEtiqueta) {
                if (nombre.equalsIgnoreCase(etiq)) {
                    esEtiqueta = true;
                    break;
                }
            }
            if (esEtiqueta) continue;

            // Contenedor de la fila
            HBox fila = new HBox(10);
            fila.setStyle("-fx-padding: 5; -fx-alignment: center-left; " +
                    "-fx-background-color: " + (i % 2 == 0 ? "#ffffff" : "#f5f5f5") + ";");

            // CheckBox con el nombre de la variable
            CheckBox chk = new CheckBox(nombre);
            chk.setSelected(true); // por defecto se selecciona
            chk.setPrefWidth(200);
            chk.setStyle("-fx-font-size: 10px;");

            // Campo de texto para el peso
            TextField txtPeso = new TextField("1.0");
            txtPeso.setPrefWidth(70);
            txtPeso.setStyle("-fx-font-size: 10px; -fx-alignment: center;");

            // ComboBox para elegir la normalización
            ComboBox<String> comboNorm = new ComboBox<>();
            comboNorm.setPrefWidth(130);
            comboNorm.setStyle("-fx-font-size: 10px;");

            // Determinar si la columna es numérica según el análisis de CSV
            boolean esNumerica = (datosCSV.columnasNumericas != null &&
                    i < datosCSV.columnasNumericas.length &&
                    datosCSV.columnasNumericas[i]);

            if (esNumerica) {
                // Variables numéricas: varias opciones de normalización
                comboNorm.getItems().addAll("Min-Max", "Z-Score", "Logarítmica");
                comboNorm.setValue("Min-Max");
            } else {
                // Variables cualitativas: ya se tratan como One-Hot desde CSV
                comboNorm.getItems().add("One-Hot");
                comboNorm.setValue("One-Hot");
                comboNorm.setDisable(true); // no se permite cambiar
            }

            fila.getChildren().addAll(chk, txtPeso, comboNorm);
            vboxColumnas.getChildren().add(fila);

            // Guardar referencias para luego leer la configuración
            checkBoxesColumnas.agregar(chk);
            textFieldsPesos.agregar(txtPeso);
            combosNormalizacion.agregar(comboNorm);
        }
    }

    // Ejecuta el clustering jerárquico usando la configuración actual y genera automáticamente el archivo JSON del dendrograma.
    private void generarDendrograma() {
        // Validar que haya datos cargados
        if (datosCSV == null || datosCSV.datos == null || datosCSV.datos.tamanio() == 0) {
            mostrarError("Sin datos", "Carga un CSV válido antes de generar.");
            return;
        }

        final int n = datosCSV.datos.tamanio();

        // Copiamos los datos a una nueva lista (por seguridad / claridad)
        Lista<Dato> datosUsar = new Lista<>();
        for (int i = 0; i < n; i++) {
            datosUsar.agregar(datosCSV.datos.obtener(i));
        }

        // Bloquear controles mientras se ejecuta el clustering
        btnGenerar.setDisable(true);
        btnDescargar.setDisable(true);
        if (progress != null) {
            progress.setVisible(true);
            progress.setProgress(-1);
        }

        lblEstado.setText("Procesando clustering...");

        // Leer la configuración actual de la interfaz
        final String disSel = comboDistancia.getValue();
        final double[] pesos = obtenerPesos();
        final boolean[] ignoradas = obtenerColumnasIgnoradas();
        final INormalizacion[] normalizaciones = obtenerNormalizaciones();

        // Tarea en segundo plano para el clustering
        Task<ArbolBinario> task = new Task<>() {
            @Override
            protected ArbolBinario call() {
                // Crear estrategia de distancia según selección
                IDistancia distancia =
                        FactoryDistancia.obtenerInstancia().crear(disSel);

                // Crear algoritmo con normalizaciones, distancia, pesos e ignoradas
                AlgoritmoClustering algoritmo =
                        new AlgoritmoClustering(normalizaciones, distancia, pesos, ignoradas);

                // Indicar qué columnas son numéricas (ya detectadas en CSV)
                algoritmo.setColumnasNumericas(datosCSV.columnasNumericas);

                // Ejecutar clustering y devolver el árbol resultante
                return algoritmo.ejecutar(datosUsar);
            }
        };

        // Si el clustering terminó bien
        task.setOnSucceeded(ev -> {
            this.dendrograma = task.getValue();

            try {
                // Crear archivo temporal para el JSON del dendrograma
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

        // Si falló algo durante el clustering
        task.setOnFailed(ev -> {
            if (progress != null) progress.setVisible(false);
            btnGenerar.setDisable(false);

            Throwable ex = task.getException();
            lblEstado.setText("Error en clustering");
            mostrarError("Error al generar",
                    ex != null ? ex.getMessage() : "Fallo desconocido");
            if (ex != null) ex.printStackTrace();
        });

        // Ejecutar la tarea en un hilo separado
        new Thread(task, "cluster-worker").start();
    }

    // Permite al usuario guardar en disco el último archivo JSON generado.
    private void descargarJSON() {
        // Verificar que exista un archivo JSON listo
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
        if (destino == null) return; // Usuario canceló

        try {
            // Copiar desde el archivo temporal al destino elegido por el usuario
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

    // Construye un arreglo de estrategias de normalización, una por dimensión del vectorOriginal, en función de lo seleccionado en los ComboBox.
    // - Por defecto, todas las columnas se normalizan con Min-Max.
    // - Si en la interfaz se selecciona otra normalización (Z-Score, Logarítmica),
    //   se usa la correspondiente para esa posición.
    // - Las columnas "One-Hot" quedan con Min-Max (pero normalmente ya vienen
    //   en una escala 0/1 desde el CSV).
    private INormalizacion[] obtenerNormalizaciones() {
        if (datosCSV == null || datosCSV.datos == null ||
                datosCSV.datos.tamanio() == 0) return new INormalizacion[0];

        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        INormalizacion[] normalizaciones = new INormalizacion[dimensionReal];

        // Inicializar todas las columnas con Min-Max por defecto
        for (int i = 0; i < dimensionReal; i++) {
            normalizaciones[i] = new NormalizacionMinMax();
        }

        // Mapear la selección de los combos a estrategias concretas
        for (int i = 0; i < combosNormalizacion.tamanio() && i < dimensionReal; i++) {
            ComboBox<String> combo = combosNormalizacion.obtener(i);
            String seleccion = combo.getValue();

            // Ignoramos "One-Hot" porque la codificación ya viene hecha desde CSV
            if (seleccion != null && !seleccion.equals("One-Hot")) {
                normalizaciones[i] =
                        FactoryNormalizacion.obtenerInstancia().crear(seleccion);
            }
        }

        return normalizaciones;
    }

    // Lee los pesos introducidos por el usuario para cada variable.
    // - Intenta parsear el valor de cada TextField.
    // - Si hay error de formato, se deja el peso en 1.0.
    // - Si hay más dimensiones en el vector que TextFields, las restantes se
    //   rellenan con 1.0 también.
    private double[] obtenerPesos() {
        if (datosCSV == null || datosCSV.datos == null ||
                datosCSV.datos.tamanio() == 0) return new double[0];

        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        double[] pesos = new double[dimensionReal];

        // Leer pesos desde los campos de texto
        for (int i = 0; i < textFieldsPesos.tamanio() && i < dimensionReal; i++) {
            TextField txtPeso = textFieldsPesos.obtener(i);
            double peso = 1.0;
            try {
                peso = Double.parseDouble(txtPeso.getText().trim());
            } catch (NumberFormatException ignored) {}
            pesos[i] = peso;
        }

        // Si hay más dimensiones que campos de texto, completar con 1.0
        for (int i = textFieldsPesos.tamanio(); i < dimensionReal; i++) {
            pesos[i] = 1.0;
        }

        return pesos;
    }

    // Devuelve un arreglo booleano indicando qué columnas deben ser ignoradas
    // en el clustering, según el estado de los CheckBox.
    // - true  → columna ignorada.
    // - false → columna utilizada.
    private boolean[] obtenerColumnasIgnoradas() {
        if (datosCSV == null || datosCSV.datos == null ||
                datosCSV.datos.tamanio() == 0) return new boolean[0];

        int dimensionReal = datosCSV.datos.obtener(0).getVectorOriginal().tamanio();
        boolean[] ignoradas = new boolean[dimensionReal];

        // Las primeras columnas se corresponden con los CheckBox configurados
        for (int i = 0; i < checkBoxesColumnas.tamanio() && i < dimensionReal; i++) {
            CheckBox chk = checkBoxesColumnas.obtener(i);
            ignoradas[i] = !chk.isSelected();
        }

        // El resto, si no hay CheckBox para ellas, no se ignoran
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
