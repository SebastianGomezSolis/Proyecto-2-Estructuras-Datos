package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.Lista;
import com.sistema.proyecto2estructurasdatos.modelo.Vector;
import com.sistema.proyecto2estructurasdatos.modelo.Dato;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utilidad para leer archivos CSV
 * Complejidad: O(n*m) donde n es el número de filas y m el número de columnas
 */
public class CSV {

    /**
     * Clase para almacenar información sobre columnas
     */
    public static class InfoColumna {
        public String nombre;
        public boolean esNumerica;
        public Lista<String> categorias; // Para columnas cualitativas

        public InfoColumna(String nombre) {
            this.nombre = nombre;
            this.esNumerica = true;
            this.categorias = new Lista<>();
        }
    }

    /**
     * Clase para almacenar el resultado de la lectura
     */
    public static class ResultadoCSV {
        public Lista<Dato> datos;
        public Lista<InfoColumna> columnas;
        public int numFilas;
        public int numColumnas;

        public ResultadoCSV() {
            this.datos = new Lista<>();
            this.columnas = new Lista<>();
            this.numFilas = 0;
            this.numColumnas = 0;
        }
    }

    /**
     * Lee un archivo CSV y lo convierte en una lista de Datos
     * @param rutaArchivo Ruta del archivo CSV
     * @return ResultadoCSV con los datos procesados
     */
    public static ResultadoCSV leer(String rutaArchivo) throws IOException {
        ResultadoCSV resultado = new ResultadoCSV();

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            // Leer encabezados
            String lineaEncabezados = br.readLine();
            if (lineaEncabezados == null) {
                throw new IOException("El archivo CSV está vacío");
            }

            String[] encabezados = lineaEncabezados.split(",");
            resultado.numColumnas = encabezados.length;

            // Crear información de columnas
            for (String encabezado : encabezados) {
                resultado.columnas.agregar(new InfoColumna(encabezado.trim()));
            }

            // Almacenar todas las filas primero para analizar tipos
            Lista<String[]> filasTemporales = new Lista<>();
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] valores = linea.split(",");
                filasTemporales.agregar(valores);
                resultado.numFilas++;
            }

            // Analizar tipos de datos y categorías
            analizarTiposColumnas(resultado.columnas, filasTemporales);

            // Procesar cada fila
            int indiceDato = 0;
            for (int i = 0; i < filasTemporales.tamanio(); i++) {
                String[] valores = filasTemporales.obtener(i);

                // Crear vector con conversión one-hot para categóricos
                Vector vector = convertirAVector(valores, resultado.columnas);

                // Crear etiqueta (usar primera columna o índice)
                String etiqueta = valores.length > 0 ? valores[0] : "Dato_" + indiceDato;

                Dato dato = new Dato(etiqueta, vector, indiceDato);
                resultado.datos.agregar(dato);
                indiceDato++;
            }
        }

        return resultado;
    }

    /**
     * Analiza los tipos de datos de cada columna
     */
    private static void analizarTiposColumnas(Lista<InfoColumna> columnas, Lista<String[]> filas) {
        for (int col = 0; col < columnas.tamanio(); col++) {
            InfoColumna info = columnas.obtener(col);
            boolean todasNumericas = true;

            // Analizar todas las filas de esta columna
            for (int fila = 0; fila < filas.tamanio(); fila++) {
                String[] valores = filas.obtener(fila);
                if (col < valores.length) {
                    String valor = valores[col].trim();

                    if (!esNumerico(valor)) {
                        todasNumericas = false;
                        // Agregar categoría si no existe
                        boolean existe = false;
                        for (int i = 0; i < info.categorias.tamanio(); i++) {
                            if (info.categorias.obtener(i).equals(valor)) {
                                existe = true;
                                break;
                            }
                        }
                        if (!existe && !valor.isEmpty()) {
                            info.categorias.agregar(valor);
                        }
                    }
                }
            }

            info.esNumerica = todasNumericas;
        }
    }

    /**
     * Convierte una fila de strings a un vector numérico
     */
    private static Vector convertirAVector(String[] valores, Lista<InfoColumna> columnas) {
        Vector vector = new Vector();

        for (int i = 0; i < columnas.tamanio() && i < valores.length; i++) {
            InfoColumna info = columnas.obtener(i);
            String valor = valores[i].trim();

            if (info.esNumerica) {
                // Valor numérico directo
                try {
                    double valorNumerico = Double.parseDouble(valor);
                    vector.agregar(valorNumerico);
                } catch (NumberFormatException e) {
                    vector.agregar(0.0); // Valor por defecto si hay error
                }
            } else {
                // Valor cualitativo - codificación one-hot
                for (int j = 0; j < info.categorias.tamanio(); j++) {
                    if (info.categorias.obtener(j).equals(valor)) {
                        vector.agregar(1.0);
                    } else {
                        vector.agregar(0.0);
                    }
                }
            }
        }
        return vector;
    }

    /**
     * Verifica si un string es numérico
     */
    private static boolean esNumerico(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

