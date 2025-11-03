package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.*;
import java.io.*;

/**
 * Leer y procesar archivos CSV.
 * Convierte los datos del CSV en objetos Dato con vectores numéricos,
 * aplica one-hot para variables cualitativas.
 */
public class CSV {

    // Columnas numéricas
    private static final String[] COLUMNAS_NUMERICAS = {
            "budget", "popularity", "revenue", "runtime", "vote_average", "vote_count"
    };

    // Columnas cualitativas definidas (se codifican con one-hot)
    private static final String[] COLUMNAS_CATEGORICAS = {
            "genres", "original_language", "status"
    };

    // Columnas que se omiten por no aportar al vector (texto libre, ids, etc.)
    private static final String[] COLUMNAS_IGNORAR = {
            "homepage", "id", "keywords", "overview", "production_companies",
            "production_countries", "spoken_languages", "crew", "cast", "tagline",
            "release_date", "index"
    };

    public static ResultadoCSV leer(String rutaArchivo) throws IOException {

        ResultadoCSV resultado = new ResultadoCSV(); // contenedor de salida

        // Lector del archivo con UTF-8 para soportar caracteres especiales
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), "UTF-8")
        );

        String header = br.readLine(); // primera línea: nombres de columnas
        if (header == null) {
            br.close();
            throw new IOException("El archivo CSV está vacío.");
        }

        // Parseo del encabezado a una lista de nombres de columnas
        Lista<String> columnasHeader = separarLineaCSV(header);
        resultado.nombresColumnas = columnasHeader;          // guarda nombres
        resultado.numColumnas = columnasHeader.tamanio();    // total de columnas

        // Intenta localizar una columna que sirva como etiqueta legible
        int idxEtiqueta = buscarColumnaEtiqueta(columnasHeader);

        // Lee todas las filas restantes del archivo
        Lista<Lista<String>> todasLasFilas = new Lista<>();
        String linea;
        while ((linea = br.readLine()) != null) {
            Lista<String> columnas = separarLineaCSV(linea); // separa por campos respetando comillas
            if (columnas.tamanio() >= resultado.numColumnas) // valida tamaño consistente con header
                todasLasFilas.agregar(columnas);              // acumula fila válida
        }
        br.close(); // cierra el archivo

        // debe existir al menos una fila de datos
        if (todasLasFilas.tamanio() == 0)
            throw new IOException("No hay datos en el archivo CSV.");

        //  true = numérica, false = cualitativa/ignorada
        boolean[] esNumerica = new boolean[resultado.numColumnas];

        // one-hot: por índice de columna, un mapa (texto -> índice)
        HashMapa<Integer, HashMapa<String, Integer>> mapeosOneHot = new HashMapa<>();

        // Clasifica columnas (numérica, categórica conocida, inferida o ignorada)
        for (int col = 0; col < resultado.numColumnas; col++) {
            if (col == idxEtiqueta) {
                esNumerica[col] = false;  // la etiqueta no se vectoriza
                continue;                 // salta a la siguiente columna
            }
            String nombreColumna = columnasHeader.obtener(col);
            if (debeIgnorarColumna(nombreColumna)) {
                esNumerica[col] = false;  // se marcará como no vectorizable
                continue;
            }
            // Analiza por listas predefinidas e inferencia por muestreo
            analizarColumnaYActualizar(col, nombreColumna, todasLasFilas, esNumerica, mapeosOneHot);
        }

        // Recorre filas y construye el vector numérico final de cada una
        int contador = 0; // índice incremental para etiquetar filas sin nombre
        for (int f = 0; f < todasLasFilas.tamanio(); f++) {
            Lista<String> columnas = todasLasFilas.obtener(f);
            Vector vector = new Vector(); // vector numérico que se llenará en orden

            for (int col = 0; col < resultado.numColumnas; col++) {
                if (col == idxEtiqueta) continue; // no incluir la etiqueta en el vector
                String nombreColumna = columnasHeader.obtener(col);
                if (debeIgnorarColumna(nombreColumna)) continue; // descarta columnas ignoradas
                String valor = columnas.obtener(col); // texto crudo de la celda

                if (esNumerica[col]) {
                    // Convierte valor de texto a double (manejo seguro de vacíos y "null")
                    vector.agregar(convertirANumero(valor));
                } else {
                    // Si hay mapeo cualitativo, aplica one-hot y concatena al vector
                    HashMapa<String, Integer> mapeo = mapeosOneHot.obtener(col);
                    if (mapeo != null)
                        aplicarOneHot(valor, mapeo, vector, nombreColumna);
                }
            }

            // Determina etiqueta (si hay columna candidata) o genera "fila_i"
            String etiqueta = obtenerEtiqueta(columnas, idxEtiqueta, contador);

            // Crea el Dato de salida con su etiqueta, vector y posición original
            Dato d = new Dato(etiqueta, vector, contador);
            resultado.datos.agregar(d); // agrega al contenedor final
            contador++;                 // avanza el índice de fila
        }

        // Registra metadatos finales del procesamiento
        resultado.numFilas = contador;           // total de filas procesadas
        resultado.columnasNumericas = esNumerica; // expone qué columnas fueron numéricas
        return resultado;                        // retorna el conjunto listo
    }

    // Devuelve true si la columna está en la lista a ignorar (no se vectoriza)
    private static boolean debeIgnorarColumna(String nombreColumna) {
        for (String col : COLUMNAS_IGNORAR)
            if (col.equalsIgnoreCase(nombreColumna)) return true; // coincide por nombre
        return false; // no está listada para ignorar
    }

    // Verifica si la columna está en la lista de numéricas definidas a prioridad
    private static boolean esColumnaNumericaDefinida(String nombreColumna) {
        for (String col : COLUMNAS_NUMERICAS)
            if (col.equalsIgnoreCase(nombreColumna)) return true;
        return false;
    }

    // Verifica si la columna está en la lista de categóricas definidas a prioridad
    private static boolean esColumnaCategoricaDefinida(String nombreColumna) {
        for (String col : COLUMNAS_CATEGORICAS)
            if (col.equalsIgnoreCase(nombreColumna)) return true;
        return false;
    }

    // Decide si una columna es numérica o cualitativa; si es categórica, crea su mapeo one-hot
    private static void analizarColumnaYActualizar(
            int col, String nombreColumna, Lista<Lista<String>> filas,
            boolean[] esNumerica, HashMapa<Integer, HashMapa<String, Integer>> mapeosOneHot) {

        // Preferencia por configuración explícita
        if (esColumnaNumericaDefinida(nombreColumna)) {
            esNumerica[col] = true;  // fuerza a numérica
            return;
        }
        if (esColumnaCategoricaDefinida(nombreColumna)) {
            esNumerica[col] = false;                         // marca cualitativa
            construirMapeoOneHot(col, nombreColumna, filas, mapeosOneHot); // genera diccionario
            return;
        }

        // Inferencia automática por muestreo de hasta 100 filas (rápido y representativo)
        int numericos = 0, textos = 0;
        int maxAnalizar = Math.min(100, filas.tamanio()); // límite superior de muestras
        for (int f = 0; f < maxAnalizar; f++) {
            String valor = filas.obtener(f).obtener(col); // valor crudo de la columna
            if (esNumerico(valor)) numericos++; else textos++; // clasifica valor
        }

        // Se considera numérica si domina y supera un umbral (80% de las muestras)
        boolean colEsNumerica = (numericos > textos && numericos > maxAnalizar * 0.8);
        esNumerica[col] = colEsNumerica; // marca resultado de la inferencia
        if (!colEsNumerica) construirMapeoOneHot(col, nombreColumna, filas, mapeosOneHot); // prepara one-hot
    }

    // Construye el diccionario texto->índice para la columna categórica (one-hot)
    private static void construirMapeoOneHot(
            int col, String nombreColumna, Lista<Lista<String>> filas,
            HashMapa<Integer, HashMapa<String, Integer>> mapeosOneHot) {

        HashMapa<String, Integer> categorias = new HashMapa<>(); // diccionario de categorías
        int indiceCat = 0;                                       // índice incremental
        boolean esGeneros = nombreColumna.equalsIgnoreCase("genres"); // caso especial: múltiples etiquetas

        // Recorre todas las filas para descubrir categorías distintas
        for (int f = 0; f < filas.tamanio(); f++) {
            String valor = filas.obtener(f).obtener(col); // texto de la celda
            if (valor == null || valor.trim().isEmpty()) continue; // ignora vacíos

            if (esGeneros) {
                // "genres": permite varios tokens separados por espacios
                String[] generos = valor.trim().split("\\s+");
                for (String genero : generos) {
                    genero = genero.trim();
                    if (!genero.isEmpty() && !categorias.contieneLlave(genero))
                        categorias.insertar(genero, indiceCat++); // registra nueva categoría
                }
            } else {
                // Categórica simple: se descartan numéricos para no mezclar tipos
                valor = valor.trim();
                if (!esNumerico(valor) && !categorias.contieneLlave(valor))
                    categorias.insertar(valor, indiceCat++); // registra nueva categoría
            }
        }

        // Si se detectaron categorías, asocia el mapeo con la columna
        if (categorias.tamano() > 0)
            mapeosOneHot.insertar(col, categorias);
    }

    // Añade al vector el bloque one-hot correspondiente al valor de la celda
    private static void aplicarOneHot(String valor, HashMapa<String, Integer> mapeo, Vector vector, String nombreColumna) {
        int k = mapeo.tamano();         // dimensión del bloque one-hot
        double[] oneHot = new double[k];
        for (int i = 0; i < k; i++) oneHot[i] = 0.0; // inicializa en ceros

        if (valor != null && !valor.trim().isEmpty()) {
            boolean esGeneros = nombreColumna.equalsIgnoreCase("genres");
            if (esGeneros) {
                // Activa varias posiciones si hay múltiples etiquetas
                String[] valores = valor.trim().split("\\s+");
                for (String val : valores) {
                    Integer indice = mapeo.obtener(val.trim()); // consulta índice de categoría
                    if (indice != null && indice < k) oneHot[indice] = 1.0; // activa posición
                }
            } else {
                // Activa una sola posición para categoría única
                Integer indice = mapeo.obtener(valor.trim());
                if (indice != null && indice < k) oneHot[indice] = 1.0;
            }
        }

        // Concatena el bloque one-hot al final del vector de la fila
        for (int i = 0; i < k; i++) vector.agregar(oneHot[i]);
    }

    // Determina si una cadena representa un número (double) de forma segura
    private static boolean esNumerico(String valor) {
        if (valor == null || valor.trim().isEmpty()) return false; // vacío no es número
        try {
            Double.parseDouble(valor.trim()); //  parsea
            return true;
        } catch (NumberFormatException e) {
            return false; // si falla el parseo, no es numérico
        }
    }

    // Convierte cadena a double; devuelve 0.0 ante vacío, "null" o formato inválido
    private static double convertirANumero(String valor) {
        if (valor == null || valor.trim().isEmpty() || valor.equalsIgnoreCase("null")) return 0.0;
        try {
            return Double.parseDouble(valor.trim()); // parseo directo
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Intenta localizar una columna adecuada como etiqueta (nombres comunes)
    private static int buscarColumnaEtiqueta(Lista<String> columnas) {
        String[] posiblesNombres = {"title", "nombre", "id", "original_title"};
        for (String nombre : posiblesNombres)
            for (int i = 0; i < columnas.tamanio(); i++)
                if (columnas.obtener(i).equalsIgnoreCase(nombre))
                    return i; // devuelve el índice de la primera coincidencia
        return -1; // no se encontró columna de etiqueta
    }

    // Obtiene etiqueta a mostrar por fila; si no hay, genera "fila_<n>"
    private static String obtenerEtiqueta(Lista<String> columnas, int idxEtiqueta, int contador) {
        if (idxEtiqueta != -1 && idxEtiqueta < columnas.tamanio()) {
            String etiqueta = columnas.obtener(idxEtiqueta);
            if (etiqueta != null && !etiqueta.trim().isEmpty())
                return etiqueta.trim(); // etiqueta válida
        }
        return "fila_" + contador; // nombre por defecto
    }

    // Parser de una línea CSV que respeta campos entrecomillados y comas internas
    private static Lista<String> separarLineaCSV(String line) {
        Lista<String> resultado = new Lista<>();
        StringBuilder actual = new StringBuilder(); // acumulador del campo actual
        boolean dentroDeComillas = false;          //  dentro o fuera de comillas

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // Manejo de comillas escapadas: ""
                if (dentroDeComillas && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    actual.append('"'); // añade una comilla al campo
                    i++;                // salta la comilla siguiente
                } else dentroDeComillas = !dentroDeComillas; // alterna el estado
            } else if (c == ',' && !dentroDeComillas) {
                // Fin de campo solo si no estamos dentro de comillas
                resultado.agregar(actual.toString()); // guarda el campo
                actual.setLength(0);                  // reinicia el acumulador
            } else actual.append(c); // agrega carácter normal al campo
        }
        // Agrega el último campo que queda en el acumulador
        resultado.agregar(actual.toString());
        return resultado; // devuelve la lista de columnas
    }
}
