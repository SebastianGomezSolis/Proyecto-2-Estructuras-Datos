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
 * No utiliza librerías externas ni internas avanzadas del lenguaje.
 * Cumple con las restricciones del proyecto.
 */
public class CSV {

    /**
     * Lee un archivo CSV desde la ruta indicada y genera un objeto ResultadoCSV.
     * @param rutaArchivo ruta absoluta del archivo CSV
     * @return estructura ResultadoCSV con los datos cargados
     * @throws IOException si ocurre un error de lectura
     */
    public static ResultadoCSV leer(String rutaArchivo) throws IOException {
        ResultadoCSV resultado = new ResultadoCSV();

        // Abrimos el archivo de forma segura
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), "UTF-8")
        );

        // Leemos la primera línea (cabecera)
        String header = br.readLine();
        if (header == null) {
            br.close();
            throw new IOException("El archivo CSV está vacío.");
        }

        // Eliminamos posible BOM (Byte Order Mark)
        if (header.length() > 0 && header.charAt(0) == '\uFEFF') {
            header = header.substring(1);
        }

        // Procesamos los nombres de columna
        Lista<String> nombres = new Lista<>();
        Lista<String> columnasHeader = parseCSVLine(header);
        for (int i = 0; i < columnasHeader.tamanio(); i++) {
            nombres.agregar(columnasHeader.obtener(i));
        }
        resultado.nombresColumnas = nombres;
        resultado.numColumnas = nombres.tamanio();

        // Posiciones de columnas relevantes (las que usaremos en el clustering)
        int idxBudget = buscarIndice("budget", nombres);
        int idxPopularity = buscarIndice("popularity", nombres);
        int idxRevenue = buscarIndice("revenue", nombres);
        int idxRuntime = buscarIndice("runtime", nombres);
        int idxVoteAverage = buscarIndice("vote_average", nombres);
        int idxVoteCount = buscarIndice("vote_count", nombres);
        int idxTitle = buscarIndice("title", nombres);
        int idxOriginalTitle = buscarIndice("original_title", nombres);

        String linea;
        int contador = 0;

        // Leemos cada fila
        while ((linea = br.readLine()) != null) {
            Lista<String> columnas = parseCSVLine(linea);
            if (columnas.tamanio() < resultado.numColumnas) {
                // Línea corrupta o incompleta
                continue;
            }

            Vector vector = new Vector();

            // Cargar solo las columnas numéricas
            vector.agregar(obtenerNumero(columnas, idxBudget));
            vector.agregar(obtenerNumero(columnas, idxPopularity));
            vector.agregar(obtenerNumero(columnas, idxRevenue));
            vector.agregar(obtenerNumero(columnas, idxRuntime));
            vector.agregar(obtenerNumero(columnas, idxVoteAverage));
            vector.agregar(obtenerNumero(columnas, idxVoteCount));

            // Obtener la etiqueta (nombre de la película o fila)
            String etiqueta = "fila_" + contador;
            if (idxTitle != -1) {
                etiqueta = columnas.obtener(idxTitle);
            } else if (idxOriginalTitle != -1) {
                etiqueta = columnas.obtener(idxOriginalTitle);
            }

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
     * Busca un nombre de columna dentro de la lista de encabezados.
     * Retorna -1 si no se encuentra.
     */
    private static int buscarIndice(String nombre, Lista<String> columnas) {
        for (int i = 0; i < columnas.tamanio(); i++) {
            String actual = columnas.obtener(i);
            if (actual != null && actual.equalsIgnoreCase(nombre)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Convierte una cadena a número, retorna 0.0 si no es válida.
     */
    private static double obtenerNumero(Lista<String> columnas, int indice) {
        if (indice == -1) return 0.0;
        String valor = columnas.obtener(indice);
        if (valor == null) return 0.0;

        valor = valor.trim();
        if (valor.equals("") || valor.equalsIgnoreCase("null")) return 0.0;

        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Analiza una línea CSV y separa las columnas respetando las comillas.
     * Implementación manual sin uso de bibliotecas.
     */
    private static Lista<String> parseCSVLine(String line) {
        Lista<String> resultado = new Lista<>();
        StringBuilder actual = new StringBuilder();
        boolean dentroDeComillas = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Doble comilla escapada
                if (dentroDeComillas && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    dentroDeComillas = !dentroDeComillas;
                }
            } else if (c == ',' && !dentroDeComillas) {
                // Nueva columna
                resultado.agregar(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }

        // Agregar última columna
        resultado.agregar(actual.toString());
        return resultado;
    }
}
