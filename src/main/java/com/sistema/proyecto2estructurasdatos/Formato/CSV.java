package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.Lista;
import com.sistema.proyecto2estructurasdatos.modelo.Vector;
import com.sistema.proyecto2estructurasdatos.modelo.Dato;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CSV {

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

            // Guardar nombres de columnas
            for (String encabezado : encabezados) {
                resultado.nombresColumnas.agregar(encabezado.trim());
            }

            // Leer todas las filas primero
            Lista<String[]> filasTemporales = new Lista<>();
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] valores = linea.split(",");
                filasTemporales.agregar(valores);
                resultado.numFilas++;
            }

            // Analizar qué columnas son numéricas y cuáles categóricas
            boolean[] esNumerica = analizarTipos(filasTemporales, encabezados.length);

            // Obtener categorías para columnas cualitativas
            Lista<Lista<String>> categoriasPorColumna = obtenerCategorias(filasTemporales, esNumerica);

            // Procesar cada fila y crear los datos
            for (int i = 0; i < filasTemporales.tamanio(); i++) {
                String[] valores = filasTemporales.obtener(i);

                Vector vector = convertirAVector(valores, esNumerica, categoriasPorColumna);
                String etiqueta = valores.length > 0 ? valores[0].trim() : "Dato_" + i;

                Dato dato = new Dato(etiqueta, vector, i);
                resultado.datos.agregar(dato);
            }
        }

        return resultado;
    }

    // Analiza qué columnas son numéricas
    private static boolean[] analizarTipos(Lista<String[]> filas, int numColumnas) {
        boolean[] esNumerica = new boolean[numColumnas];

        // Inicialmente asumimos que todas son numéricas
        for (int col = 0; col < numColumnas; col++) {
            esNumerica[col] = true;
        }

        // Verificar cada columna
        for (int col = 0; col < numColumnas; col++) {
            for (int fila = 0; fila < filas.tamanio(); fila++) {
                String[] valores = filas.obtener(fila);
                if (col < valores.length) {
                    String valor = valores[col].trim();
                    if (!esNumerico(valor)) {
                        esNumerica[col] = false;
                        break;
                    }
                }
            }
        }

        return esNumerica;
    }

    // Obtiene todas las categorías únicas para columnas categóricas
    private static Lista<Lista<String>> obtenerCategorias(Lista<String[]> filas, boolean[] esNumerica) {
        Lista<Lista<String>> categorias = new Lista<>();

        for (int col = 0; col < esNumerica.length; col++) {
            Lista<String> categoriasColumna = new Lista<>();

            if (!esNumerica[col]) {
                // Es categórica, recolectar valores únicos
                for (int fila = 0; fila < filas.tamanio(); fila++) {
                    String[] valores = filas.obtener(fila);
                    if (col < valores.length) {
                        String valor = valores[col].trim();
                        if (!valor.isEmpty() && !categoriasColumna.contiene(valor)) {
                            categoriasColumna.agregar(valor);
                        }
                    }
                }
            }

            categorias.agregar(categoriasColumna);
        }

        return categorias;
    }

    // Convierte una fila a vector numérico (con one-hot para categóricas)
    private static Vector convertirAVector(String[] valores, boolean[] esNumerica,
                                           Lista<Lista<String>> categorias) {
        Vector vector = new Vector();

        for (int col = 0; col < esNumerica.length && col < valores.length; col++) {
            String valor = valores[col].trim();

            if (esNumerica[col]) {
                // Valor numérico directo
                try {
                    double valorNumerico = Double.parseDouble(valor);
                    vector.agregar(valorNumerico);
                } catch (NumberFormatException e) {
                    vector.agregar(0.0);
                }
            } else {
                // Valor categórico - one-hot encoding
                Lista<String> categoriasCol = categorias.obtener(col);
                for (int i = 0; i < categoriasCol.tamanio(); i++) {
                    if (categoriasCol.obtener(i).equals(valor)) {
                        vector.agregar(1.0);
                    } else {
                        vector.agregar(0.0);
                    }
                }
            }
        }

        return vector;
    }

    // Verifica si un string es numérico
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