package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.Dato;
import com.sistema.proyecto2estructurasdatos.modelo.Lista;
import com.sistema.proyecto2estructurasdatos.modelo.Vector;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Esta clase se encarga de leer archivos CSV (archivos de Excel con comas).
 * Lo especial es que convierte los datos en un formato que las computadoras entienden mejor:
 * - Si encuentra números, los deja como están
 * - Si encuentra palabras o categorías (como "rojo", "azul"), las convierte en unos y ceros
 */

public class CSV {

    // Este es el metodo principal que lee todo el archivo CSV
    public static ResultadoCSV leer(String rutaArchivo) throws IOException {
        // Creamos una cajita donde guardaremos todo lo que leamos
        ResultadoCSV resultado = new ResultadoCSV();

        // Abrimos el archivo para leerlo, asegurándonos de que lea bien los acentos (UTF-8)
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), "UTF-8")
        );

        // === LEER LA PRIMERA LÍNEA (LOS TÍTULOS DE LAS COLUMNAS) ===

        // Leemos la primera línea del archivo que tiene los nombres de las columnas
        String header = br.readLine();

        // Si está vacía, hay un problema
        if (header == null) {
            br.close();
            throw new IOException("El archivo CSV está vacío.");
        }

        // si los archivos tienen un carácter invisible al inicio, lo quitamos
        if (header.length() > 0 && header.charAt(0) == '\uFEFF') {
            header = header.substring(1);
        }

        // Separamos los títulos de las columnas (están separados por comas)
        Lista<String> columnasHeader = separarLineaCSV(header);

        // Guardamos los nombres de las columnas y cuántas hay
        resultado.nombresColumnas = columnasHeader;
        resultado.numColumnas = columnasHeader.tamanio();

        // Buscamos si hay una columna especial que sirva como "nombre" o "etiqueta"
        // (por ejemplo: "título", "nombre", "id")
        int idxEtiqueta = buscarColumnaEtiqueta(columnasHeader);

        // === PASO 1: LEER TODAS LAS FILAS DEL ARCHIVO ===

        // Creamos una lista para guardar temporalmente todas las filas
        Lista<Lista<String>> todasLasFilas = new Lista<>();
        String linea;

        // Leemos línea por línea hasta que se acabe el archivo
        while ((linea = br.readLine()) != null) {
            // Separamos cada línea en sus columnas
            Lista<String> columnas = separarLineaCSV(linea);

            // Solo guardamos la fila si tiene suficientes columnas
            if (columnas.tamanio() >= resultado.numColumnas) {
                todasLasFilas.agregar(columnas);
            }
        }
        br.close(); // Cerramos el archivo porque ya terminamos de leer

        // Si no hay datos, lanzamos un error
        if (todasLasFilas.tamanio() == 0) {
            throw new IOException("No hay datos en el archivo CSV.");
        }

        // === PASO 2: AVERIGUAR QUÉ TIPO DE DATOS HAY EN CADA COLUMNA ===

        // Creamos un array que dice si cada columna tiene números (true) o texto (false)
        boolean[] esNumerica = new boolean[resultado.numColumnas];

        // Este mapa guarda las conversiones para columnas de texto
        // Por ejemplo: si una columna tiene "rojo", "azul", "verde"
        // las convierte en: rojo=0, azul=1, verde=2
        Map<Integer, Map<String, Integer>> mapeosOneHot = new HashMap<>();

        // Revisamos cada columna una por una
        for (int col = 0; col < resultado.numColumnas; col++) {
            // Si es la columna de etiquetas, la saltamos
            if (col == idxEtiqueta) {
                esNumerica[col] = false;
                continue;
            }

            // Analizamos la columna para ver qué tipo de datos tiene
            AnalisisColumna analisis = analizarColumna(col, todasLasFilas);
            esNumerica[col] = analisis.esNumerica;

            // Si la columna tiene texto y encontramos diferentes categorías,
            // guardamos el mapeo para convertirlas a números después
            if (!analisis.esNumerica && analisis.categoriasUnicas.size() > 0) {
                mapeosOneHot.put(col, analisis.categoriasUnicas);
            }
        }

        // === PASO 3: CONVERTIR TODAS LAS FILAS A VECTORES NUMÉRICOS ===

        int contador = 0; // Llevamos la cuenta de cuántas filas procesamos

        // Recorremos cada fila que leímos
        for (int f = 0; f < todasLasFilas.tamanio(); f++) {
            Lista<String> columnas = todasLasFilas.obtener(f);

            // Creamos un vector (lista de números) para esta fila
            Vector vector = new Vector();

            // Procesamos cada columna de la fila
            for (int col = 0; col < resultado.numColumnas; col++) {
                // Saltamos la columna de etiquetas
                if (col == idxEtiqueta) continue;

                String valor = columnas.obtener(col);

                if (esNumerica[col]) {
                    // Si la columna es numérica, simplemente agregamos el número
                    vector.agregar(convertirANumero(valor));
                } else {
                    // Si la columna es de texto, aplicamos "one-hot encoding"
                    // Esto significa: convertir "rojo" en [1, 0, 0], "azul" en [0, 1, 0], etc.

                    Map<String, Integer> mapeo = mapeosOneHot.get(col);
                    if (mapeo != null) {
                        // Limpiamos el valor (quitamos espacios)
                        String valorLimpio = (valor != null) ? valor.trim() : "";

                        // Buscamos a qué número corresponde este valor
                        Integer indiceCategoria = mapeo.get(valorLimpio);

                        // Creamos un conjunto de ceros con un 1 en la posición correcta
                        // Ejemplo: si "azul" es la categoría #1 de 3, ponemos [0, 1, 0]
                        for (int i = 0; i < mapeo.size(); i++) {
                            vector.agregar((indiceCategoria != null && indiceCategoria == i) ? 1.0 : 0.0);
                        }
                    }
                }
            }

            // Obtenemos la etiqueta (nombre) de esta fila
            String etiqueta = obtenerEtiqueta(columnas, idxEtiqueta, contador);

            // Creamos un objeto Dato con la etiqueta, el vector y el número de fila
            Dato d = new Dato(etiqueta, vector, contador);
            resultado.datos.agregar(d);
            contador++;
        }

        // Guardamos cuántas filas procesamos en total
        resultado.numFilas = contador;
        return resultado; // Devolvemos lo que procesamos
    }

    /**
     * Esta clase pequeña guarda el resultado de analizar una columna:
     * - Si es numérica o no
     * - Las categorías únicas que encontró (para columnas de texto)
     */
    private static class AnalisisColumna {
        boolean esNumerica; // ¿Es una columna de números?
        Map<String, Integer> categoriasUnicas; // Las diferentes palabras encontradas

        AnalisisColumna(boolean esNumerica, Map<String, Integer> categorias) {
            this.esNumerica = esNumerica;
            this.categoriasUnicas = categorias;
        }
    }

    /**
     * Este método revisa una columna completa para decidir si tiene números o texto,
     * y si es texto, crea una lista de todas las palabras diferentes que encuentra
     */
    private static AnalisisColumna analizarColumna(int col, Lista<Lista<String>> filas) {
        // Contadores para llevar la cuenta
        int numericos = 0; // ¿Cuántos valores numéricos encontramos?
        int textos = 0;    // ¿Cuántos valores de texto encontramos?

        // Aquí guardamos las diferentes categorías de texto que encontramos
        Map<String, Integer> categorias = new HashMap<>();
        int indiceCat = 0; // Número que le asignamos a cada categoría nueva

        // No revisamos TODAS las filas para ser más rápidos, solo las primeras 200
        int maxAnalizar = Math.min(200, filas.tamanio());

        // Revisamos cada fila (hasta 200)
        for (int f = 0; f < maxAnalizar; f++) {
            Lista<String> fila = filas.obtener(f);

            // Si la fila no tiene esta columna, la saltamos
            if (col >= fila.tamanio()) continue;

            String valor = fila.obtener(col);

            // Si está vacía, la saltamos
            if (valor == null || valor.trim().isEmpty()) continue;

            valor = valor.trim(); // Quitamos espacios al inicio y final

            // Verificamos si es un número
            if (esNumerico(valor)) {
                numericos++; // Aumentamos el contador de números
            } else {
                textos++; // Aumentamos el contador de texto

                // Si es una palabra nueva que no habíamos visto, la agregamos
                if (!categorias.containsKey(valor)) {
                    categorias.put(valor, indiceCat++);
                }
            }
        }

        // Decidimos: si más del 80% son números, consideramos que es columna numérica
        boolean esNumerica = (numericos > textos && numericos > maxAnalizar * 0.8);

        // Si decidimos que es columna de texto, revisamos TODAS las filas restantes
        // para asegurarnos de no perdernos ninguna categoría
        if (!esNumerica && textos > 0) {
            for (int f = maxAnalizar; f < filas.tamanio(); f++) {
                Lista<String> fila = filas.obtener(f);
                if (col >= fila.tamanio()) continue;

                String valor = fila.obtener(col);
                if (valor != null && !valor.trim().isEmpty()) {
                    valor = valor.trim();

                    // Si encontramos una categoría nueva, la agregamos
                    if (!esNumerico(valor) && !categorias.containsKey(valor)) {
                        categorias.put(valor, indiceCat++);
                    }
                }
            }
        }

        // Devolvemos el resultado del análisis
        return new AnalisisColumna(esNumerica, categorias);
    }

    /**
     * Este método verifica si un texto es realmente un número
     * (por ejemplo: "123.45" es número, pero "hola" no lo es)
     */
    private static boolean esNumerico(String valor) {
        // Si está vacío, no es número
        if (valor == null || valor.trim().isEmpty()) return false;

        try {
            // Intentamos convertirlo a número
            Double.parseDouble(valor.trim());
            return true; // Si funcionó, ¡es número!
        } catch (NumberFormatException e) {
            return false; // Si falló, no es número
        }
    }

    /**
     * Convierte un texto a número.
     * Si no puede convertirlo, devuelve 0
     */
    private static double convertirANumero(String valor) {
        // Si está vacío o dice "null", devolvemos 0
        if (valor == null || valor.trim().isEmpty() || valor.equalsIgnoreCase("null")) {
            return 0.0;
        }

        try {
            // Intentamos convertir el texto a número
            return Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            // Si no se puede, devolvemos 0
            return 0.0;
        }
    }

    /**
     * Busca si hay una columna que sirva como "etiqueta" o "nombre"
     * Busca nombres comunes como "title", "name", "nombre", "id", etc.
     */
    private static int buscarColumnaEtiqueta(Lista<String> columnas) {
        // Lista de nombres típicos para columnas de etiquetas
        String[] posiblesNombres = {"title", "name", "nombre", "id", "label",
                "original_title", "movie_title"};

        // Revisamos cada posible nombre
        for (String nombre : posiblesNombres) {
            // Revisamos cada columna del archivo
            for (int i = 0; i < columnas.tamanio(); i++) {
                String col = columnas.obtener(i);

                // Si coincide con algún nombre típico, ¡la encontramos!
                if (col != null && col.trim().equalsIgnoreCase(nombre)) {
                    return i; // Devolvemos la posición de la columna
                }
            }
        }

        // Si no encontramos ninguna columna de etiquetas, devolvemos -1
        return -1;
    }

    /**
     * Obtiene la etiqueta (nombre) de una fila.
     * Si hay una columna especial de etiquetas, usa esa.
     * Si no, genera un nombre automático como "fila_0", "fila_1", etc.
     */
    private static String obtenerEtiqueta(Lista<String> columnas, int idxEtiqueta, int contador) {
        // Si encontramos una columna de etiquetas y tiene valor
        if (idxEtiqueta != -1 && idxEtiqueta < columnas.tamanio()) {
            String etiqueta = columnas.obtener(idxEtiqueta);

            // Si no está vacía, la usamos
            if (etiqueta != null && !etiqueta.trim().isEmpty()) {
                return etiqueta.trim();
            }
        }

        // Si no hay etiqueta, generamos una automática
        return "fila_" + contador;
    }

    /**
     * Este método separa una línea CSV en sus columnas individuales.
     * Es complicado porque los valores pueden estar entre comillas
     * y las comillas pueden contener comas (por eso no podemos simplemente separar por comas)
     */
    private static Lista<String> separarLineaCSV(String line) {
        // Aquí guardaremos las columnas que vamos encontrando
        Lista<String> resultado = new Lista<>();

        // Aquí vamos construyendo cada columna, letra por letra
        StringBuilder actual = new StringBuilder();

        // Esta variable nos dice si estamos dentro de comillas o no
        boolean dentroDeComillas = false;

        // Recorremos la línea carácter por carácter
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i); // El carácter actual

            if (c == '"') {
                // Si encontramos comillas dobles (""), significa que es una comilla literal
                if (dentroDeComillas && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    actual.append('"'); // Agregamos una comilla
                    i++; // Saltamos la siguiente comilla
                } else {
                    // Si es una sola comilla, cambiamos el estado (entramos o salimos de comillas)
                    dentroDeComillas = !dentroDeComillas;
                }
            } else if (c == ',' && !dentroDeComillas) {
                // Si encontramos una coma Y no estamos dentro de comillas,
                // significa que terminó esta columna
                resultado.agregar(actual.toString());
                actual.setLength(0); // Limpiamos para empezar la siguiente columna
            } else {
                // Cualquier otro carácter lo agregamos a la columna actual
                actual.append(c);
            }
        }

        // No olvidamos agregar la última columna
        resultado.agregar(actual.toString());
        return resultado;
    }
}