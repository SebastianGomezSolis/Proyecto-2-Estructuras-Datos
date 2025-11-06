package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.*;
import java.io.*;

// Clase responsable de leer y procesar archivos CSV.
// Funciones principales:
//  - Leer el archivo línea por línea.
//  - Separar encabezados y filas.
//  - Detectar automáticamente qué columnas son numéricas y cuáles son cualitativas.
//  - Generar los mapeos necesarios para one-hot en columnas categóricas.
//  - Construir objetos Dato con un vector numérico listo para el clustering.
// El resultado final se devuelve en un objeto ResultadoCSV.
public class CSV {
    // Lee un archivo CSV desde la ruta indicada y lo transforma en un ResultadoCSV
    // Pasos generales:
    //  1. Leer el encabezado y obtener los nombres de las columnas.
    //  2. Leer todas las filas válidas y guardarlas como listas de Strings.
    //  3. Analizar cada columna para decidir si es numérica, cualitativa o ignorada.
    //  4. Construir un vector numérico por fila (aplicando one-hot cuando corresponda).
    //  5. Crear objetos Dato y llenar el ResultadoCSV.
    public static ResultadoCSV leer(String rutaArchivo) throws IOException {
        ResultadoCSV resultado = new ResultadoCSV();

        // === Lectura del archivo ===
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), "UTF-8")
        );

        // Leer primera línea (encabezados)
        String header = br.readLine();
        if (header == null) {
            br.close();
            throw new IOException("El archivo CSV está vacío.");
        }

        // Nombres de columnas a partir del header
        Lista<String> columnasHeader = separarLineaCSV(header);
        resultado.nombresColumnas = columnasHeader;
        resultado.numColumnas = columnasHeader.tamanio();

        // Intentamos detectar una columna que funcione como "etiqueta" (nombre, título, etc.)
        int idxEtiqueta = buscarColumnaEtiqueta(columnasHeader);

        // === Leer todas filas ===
        Lista<Lista<String>> todasLasFilas = new Lista<>();
        String linea;
        while ((linea = br.readLine()) != null) {
            Lista<String> columnas = separarLineaCSV(linea);
            if (columnas.tamanio() >= resultado.numColumnas)
                todasLasFilas.agregar(columnas);
        }
        br.close();

        // Si no se encontró ninguna fila de datos, el archivo no sirve
        if (todasLasFilas.tamanio() == 0)
            throw new IOException("No hay datos en el archivo CSV.");

        // === Preparación de estructuras ===
        boolean[] esNumerica = new boolean[resultado.numColumnas];
        boolean[] esCualitativa = new boolean[resultado.numColumnas];
        HashMapa<Integer, HashMapa<String, Integer>> mapeosOneHot = new HashMapa<>();

        // === Clasificación automática de cada columna ===
        for (int col = 0; col < resultado.numColumnas; col++) {
            if (col == idxEtiqueta) {
                esNumerica[col] = false;
                esCualitativa[col] = false;
                continue;
            }
            analizarColumna(col, todasLasFilas, esNumerica, esCualitativa, mapeosOneHot);
        }

        // === Construcción del vector numérico para cada fila ===
        int contador = 0;
        for (int f = 0; f < todasLasFilas.tamanio(); f++) {
            Lista<String> columnas = todasLasFilas.obtener(f);
            Vector vector = new Vector();

            // Recorremos columna por columna
            for (int col = 0; col < resultado.numColumnas; col++) {
                // Saltamos la columna de etiqueta (se usa solo como nombre)
                if (col == idxEtiqueta) continue;

                String valor = columnas.obtener(col);

                // Si la columna es numérica, convertimos a double y lo agregamos
                if (esNumerica[col]) {
                    vector.agregar(convertirANumero(valor));
                }
                // Si es cualitativa, aplicamos one-hot usando el mapeo construido
                else if (esCualitativa[col]) {
                    HashMapa<String, Integer> mapeo = mapeosOneHot.obtener(col);
                    if (mapeo != null)
                        aplicarOneHot(valor, mapeo, vector);
                }
                // Si no es numérica ni cualitativa, simplemente se ignora (no aporta al vector)
            }

            // Etiqueta humana para la fila (por ejemplo, el título de la película)
            String etiqueta = obtenerEtiqueta(columnas, idxEtiqueta, contador);
            // Creamos el Dato que representa esta fila y lo agregamos al resultado
            Dato d = new Dato(etiqueta, vector, contador);
            resultado.datos.agregar(d);
            contador++;
        }

        // === Metadatos finales ===
        resultado.numFilas = contador;
        resultado.columnasNumericas = esNumerica;
        resultado.columnasCualitativas = esCualitativa;
        resultado.mapeosOneHot = mapeosOneHot;

        // Devolvemos el objeto conteniendo todo lo calculado
        return resultado;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    // Analiza una columna para determinar si debe tratarse como numérica,
    // cualitativa (one-hot) o ignorarse como texto libre.
    // Usa una muestra limitada de filas para decidir:
    //  - Proporción de valores numéricos.
    //  - Cantidad de categorías distintas.
    //  - Longitud promedio de los textos.
    private static void analizarColumna(
            int col,
            Lista<Lista<String>> filas,
            boolean[] esNumerica,
            boolean[] esCualitativa,
            HashMapa<Integer, HashMapa<String, Integer>> mapeosOneHot
    ) {
        int numericos = 0, textos = 0, noVacios = 0;
        int maxAnalizar = Math.min(150, filas.tamanio());

        HashMapa<String, Integer> categorias = new HashMapa<>();
        int distintos = 0;
        int largoPromedio = 0;

        // Recorremos una muestra de filas para esta columna
        for (int f = 0; f < maxAnalizar; f++) {
            String valor = filas.obtener(f).obtener(col);
            if (valor == null) continue;
            valor = valor.trim();
            if (valor.isEmpty()) continue;

            noVacios++;
            if (esNumerico(valor)) {
                numericos++;
            } else {
                textos++;
                largoPromedio += valor.length();
                if (!categorias.contieneLlave(valor))
                    categorias.insertar(valor, distintos++);
            }
        }

        // Si nunca vimos un valor no vacío, la columna se ignora
        if (noVacios == 0) {
            esNumerica[col] = false;
            esCualitativa[col] = false;
            return;
        }

        // Métricas para decidir el tipo de columna
        double propNum = numericos / (double) noVacios;
        double propDistintos = distintos / (double) noVacios;
        double largoMedio = textos == 0 ? 0 : largoPromedio / (double) textos;

        // Regla 1: si la mayoría son numéricos, la tratamos como numérica
        if (propNum >= 0.8) {
            esNumerica[col] = true;
            esCualitativa[col] = false;
        }
        // Regla 2: si hay pocas categorías distintas y textos no tan largos,
        // la consideramos cualitativa (categórica) y hacemos one-hot
        else if (distintos <= 50 && propDistintos <= 0.9 && largoMedio <= 40) {
            esNumerica[col] = false;
            esCualitativa[col] = true;
            construirMapeoOneHot(col, filas, mapeosOneHot);
        }
        // Caso contrario: la tratamos como texto libre y se ignora en el vector
        else {
            esNumerica[col] = false;
            esCualitativa[col] = false; // texto libre, se ignora
        }
    }

    // Construye el mapeo categoría → índice para una columna cualitativa.
    // Este mapeo se usará luego para generar los vectores one-hot.
    private static void construirMapeoOneHot(
            int col,
            Lista<Lista<String>> filas,
            HashMapa<Integer, HashMapa<String, Integer>> mapeosOneHot
    ) {
        HashMapa<String, Integer> categorias = new HashMapa<>();
        int idx = 0;

        // Recorremos todas las filas para recolectar las categorías distintas
        for (int f = 0; f < filas.tamanio(); f++) {
            String valor = filas.obtener(f).obtener(col);
            if (valor == null) continue;
            valor = valor.trim();
            if (valor.isEmpty() || esNumerico(valor)) continue;
            if (!categorias.contieneLlave(valor))
                categorias.insertar(valor, idx++);
        }

        // Solo guardamos el mapeo si encontramos al menos una categoría
        if (categorias.tamano() > 0)
            mapeosOneHot.insertar(col, categorias);
    }

    // Dado un valor categórico y su mapeo, agrega al vector el bloque one-hot
    // correspondiente a esa categoría.
    private static void aplicarOneHot(String valor, HashMapa<String, Integer> mapeo, Vector vector) {
        int k = mapeo.tamano();
        double[] oneHot = new double[k];

        // Inicializamos el vector one-hot con ceros
        for (int i = 0; i < k; i++) oneHot[i] = 0.0;

        // Si el valor está en el mapeo, activamos la posición correspondiente
        if (valor != null) {
            valor = valor.trim();
            Integer pos = mapeo.obtener(valor);
            if (pos != null && pos < k) oneHot[pos] = 1.0;
        }

        // Agregamos el bloque one-hot completo al vector final
        for (int i = 0; i < k; i++) vector.agregar(oneHot[i]);
    }

    // Verifica si una cadena puede interpretarse como número (double).
    private static boolean esNumerico(String valor) {
        if (valor == null || valor.trim().isEmpty()) return false;
        try {
            Double.parseDouble(valor.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Intenta convertir un String a número double.
    // Si el valor es nulo, vacío o no válido, devuelve 0.0.
    private static double convertirANumero(String valor) {
        if (valor == null || valor.trim().isEmpty() || valor.equalsIgnoreCase("null")) return 0.0;
        try {
            return Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Busca una columna que pueda servir como etiqueta (nombre legible),
    // revisando algunos nombres típicos en datasets comunes.
    private static int buscarColumnaEtiqueta(Lista<String> columnas) {
        String[] posibles = {"title", "nombre", "name", "original_title"};
        for (String n : posibles)
            for (int i = 0; i < columnas.tamanio(); i++)
                if (columnas.obtener(i).equalsIgnoreCase(n))
                    return i;
        return -1;
    }

    // Obtiene una etiqueta legible para una fila.
    // Si existe una columna de etiqueta, usa su valor; si no, genera algo como "fila_0", "fila_1", etc.
    private static String obtenerEtiqueta(Lista<String> columnas, int idxEtiqueta, int contador) {
        if (idxEtiqueta != -1 && idxEtiqueta < columnas.tamanio()) {
            String et = columnas.obtener(idxEtiqueta);
            if (et != null && !et.trim().isEmpty())
                return et.trim();
        }
        return "fila_" + contador;
    }

    // Separa una línea de texto CSV en una lista de columnas,
    // respetando comillas dobles y comas dentro de campos.
    // Implementa un parseo simple de CSV:
    //  - Usa comillas "..." para agrupar texto que puede contener comas.
    //  - Permite escapar comillas internas con "".
    private static Lista<String> separarLineaCSV(String line) {
        Lista<String> resultado = new Lista<>();
        StringBuilder actual = new StringBuilder();
        boolean comillas = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Caso de comillas dobles internas ("")
                if (comillas && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    // Alternamos el estado: dentro/fuera de comillas
                    comillas = !comillas;
                }
            } else if (c == ',' && !comillas) {
                // Si encontramos una coma fuera de comillas, terminamos una columna
                resultado.agregar(actual.toString());
                actual.setLength(0);
            } else {
                // Cualquier otro carácter se agrega al texto actual
                actual.append(c);
            }
        }
        // Última columna de la línea
        resultado.agregar(actual.toString());
        return resultado;
    }
}
