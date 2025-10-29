package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.Dato;
import com.sistema.proyecto2estructurasdatos.modelo.Lista;
import com.sistema.proyecto2estructurasdatos.modelo.Vector;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Clase encargada de leer archivos CSV y convertirlos en estructuras propias.
 * AHORA CARGA TODAS LAS COLUMNAS AUTOMÁTICAMENTE
 */
public class CSV {

    public static ResultadoCSV leer(String rutaArchivo) throws IOException {
        ResultadoCSV resultado = new ResultadoCSV();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), "UTF-8")
        );

        // Leer cabecera
        String header = br.readLine();
        if (header == null) {
            br.close();
            throw new IOException("El archivo CSV está vacío.");
        }

        // Eliminar BOM si existe
        if (header.length() > 0 && header.charAt(0) == '\uFEFF') {
            header = header.substring(1);
        }

        // Procesar nombres de columna
        Lista<String> columnasHeader = parseCSVLine(header);
        resultado.nombresColumnas = columnasHeader;
        resultado.numColumnas = columnasHeader.tamanio();

        // Buscar columna de etiqueta (title o nombre)
        int idxEtiqueta = buscarColumnaEtiqueta(columnasHeader);

        // Leer todas las filas
        String linea;
        int contador = 0;

        while ((linea = br.readLine()) != null) {
            Lista<String> columnas = parseCSVLine(linea);

            if (columnas.tamanio() < resultado.numColumnas) {
                continue; // Línea incompleta
            }

            // Crear vector con TODAS las columnas
            Vector vector = new Vector(resultado.numColumnas);

            for (int i = 0; i < resultado.numColumnas; i++) {
                String valorStr = columnas.obtener(i);
                double valor = convertirANumero(valorStr);
                vector.agregar(valor);
            }

            // Obtener etiqueta
            String etiqueta = obtenerEtiqueta(columnas, idxEtiqueta, contador);

            // Crear el objeto Dato
            Dato d = new Dato(etiqueta, vector, contador);
            resultado.datos.agregar(d);
            contador++;
        }

        br.close();
        resultado.numFilas = contador;

        return resultado;
    }

    /**
     * Busca columnas típicas de etiqueta/nombre
     */
    private static int buscarColumnaEtiqueta(Lista<String> columnas) {
        String[] posiblesNombres = {"title", "name", "nombre", "id", "label",
                "original_title", "movie_title"};

        for (String nombre : posiblesNombres) {
            for (int i = 0; i < columnas.tamanio(); i++) {
                String col = columnas.obtener(i);
                if (col != null && col.equalsIgnoreCase(nombre)) {
                    return i;
                }
            }
        }
        return -1; // No encontrada
    }

    /**
     * Obtiene la etiqueta para una fila
     */
    private static String obtenerEtiqueta(Lista<String> columnas, int idxEtiqueta, int contador) {
        if (idxEtiqueta != -1) {
            String etiqueta = columnas.obtener(idxEtiqueta);
            if (etiqueta != null && !etiqueta.trim().isEmpty()) {
                return etiqueta.trim();
            }
        }
        return "fila_" + contador;
    }

    /**
     * Convierte una cadena a número
     * - Si es numérico: retorna el valor
     * - Si es texto: aplica vectorización one-hot (genera un hash único)
     * - Si está vacío/null: retorna 0.0
     */
    private static double convertirANumero(String valor) {
        if (valor == null) return 0.0;

        valor = valor.trim();

        if (valor.isEmpty() || valor.equalsIgnoreCase("null")) {
            return 0.0;
        }

        // Intentar convertir a número
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            // Es texto categórico - usar hash code normalizado
            // Esto crea una representación numérica única para cada categoría
            return Math.abs(valor.hashCode() % 1000) / 1000.0;
        }
    }

    /**
     * Analiza una línea CSV respetando comillas
     */
    private static Lista<String> parseCSVLine(String line) {
        Lista<String> resultado = new Lista<>();
        StringBuilder actual = new StringBuilder();
        boolean dentroDeComillas = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (dentroDeComillas && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    dentroDeComillas = !dentroDeComillas;
                }
            } else if (c == ',' && !dentroDeComillas) {
                resultado.agregar(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }

        resultado.agregar(actual.toString());
        return resultado;
    }
}