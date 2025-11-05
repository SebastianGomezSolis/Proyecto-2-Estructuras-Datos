package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.*;
import java.io.*;

/**
 * Leer y procesar archivos CSV.
 * Convierte los datos del CSV en objetos Dato con vectores numéricos.
 * Detecta automáticamente columnas numéricas y cualitativas (one-hot).
 */
public class CSV {

    public static ResultadoCSV leer(String rutaArchivo) throws IOException {
        ResultadoCSV resultado = new ResultadoCSV();

        // === Lectura del archivo ===
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), "UTF-8")
        );

        String header = br.readLine();
        if (header == null) {
            br.close();
            throw new IOException("El archivo CSV está vacío.");
        }

        Lista<String> columnasHeader = separarLineaCSV(header);
        resultado.nombresColumnas = columnasHeader;
        resultado.numColumnas = columnasHeader.tamanio();

        // columna de etiqueta (title, name, etc.)
        int idxEtiqueta = buscarColumnaEtiqueta(columnasHeader);

        // === Leer filas ===
        Lista<Lista<String>> todasLasFilas = new Lista<>();
        String linea;
        while ((linea = br.readLine()) != null) {
            Lista<String> columnas = separarLineaCSV(linea);
            if (columnas.tamanio() >= resultado.numColumnas)
                todasLasFilas.agregar(columnas);
        }
        br.close();

        if (todasLasFilas.tamanio() == 0)
            throw new IOException("No hay datos en el archivo CSV.");

        // === Preparación de estructuras ===
        boolean[] esNumerica = new boolean[resultado.numColumnas];
        boolean[] esCualitativa = new boolean[resultado.numColumnas];
        HashMapa<Integer, HashMapa<String, Integer>> mapeosOneHot = new HashMapa<>();

        // === Clasificación automática ===
        for (int col = 0; col < resultado.numColumnas; col++) {
            if (col == idxEtiqueta) {
                esNumerica[col] = false;
                esCualitativa[col] = false;
                continue;
            }
            analizarColumna(col, todasLasFilas, esNumerica, esCualitativa, mapeosOneHot);
        }

        // === Construcción de los vectores de salida ===
        int contador = 0;
        for (int f = 0; f < todasLasFilas.tamanio(); f++) {
            Lista<String> columnas = todasLasFilas.obtener(f);
            Vector vector = new Vector();

            for (int col = 0; col < resultado.numColumnas; col++) {
                if (col == idxEtiqueta) continue;

                String valor = columnas.obtener(col);

                if (esNumerica[col]) {
                    vector.agregar(convertirANumero(valor));
                } else if (esCualitativa[col]) {
                    HashMapa<String, Integer> mapeo = mapeosOneHot.obtener(col);
                    if (mapeo != null)
                        aplicarOneHot(valor, mapeo, vector);
                }
            }

            String etiqueta = obtenerEtiqueta(columnas, idxEtiqueta, contador);
            Dato d = new Dato(etiqueta, vector, contador);
            resultado.datos.agregar(d);
            contador++;
        }

        // === Metadatos finales ===
        resultado.numFilas = contador;
        resultado.columnasNumericas = esNumerica;
        resultado.columnasCualitativas = esCualitativa;
        resultado.mapeosOneHot = mapeosOneHot;

        return resultado;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Determina automáticamente si una columna es numérica o cualitativa.
     * Si es cualitativa, genera su mapeo one-hot.
     */
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

        if (noVacios == 0) {
            esNumerica[col] = false;
            esCualitativa[col] = false;
            return;
        }

        double propNum = numericos / (double) noVacios;
        double propDistintos = distintos / (double) noVacios;
        double largoMedio = textos == 0 ? 0 : largoPromedio / (double) textos;

        if (propNum >= 0.8) {
            esNumerica[col] = true;
            esCualitativa[col] = false;
        } else if (distintos <= 50 && propDistintos <= 0.9 && largoMedio <= 40) {
            esNumerica[col] = false;
            esCualitativa[col] = true;
            construirMapeoOneHot(col, filas, mapeosOneHot);
        } else {
            esNumerica[col] = false;
            esCualitativa[col] = false; // texto libre, se ignora
        }
    }

    private static void construirMapeoOneHot(
            int col,
            Lista<Lista<String>> filas,
            HashMapa<Integer, HashMapa<String, Integer>> mapeosOneHot
    ) {
        HashMapa<String, Integer> categorias = new HashMapa<>();
        int idx = 0;
        for (int f = 0; f < filas.tamanio(); f++) {
            String valor = filas.obtener(f).obtener(col);
            if (valor == null) continue;
            valor = valor.trim();
            if (valor.isEmpty() || esNumerico(valor)) continue;
            if (!categorias.contieneLlave(valor))
                categorias.insertar(valor, idx++);
        }
        if (categorias.tamano() > 0)
            mapeosOneHot.insertar(col, categorias);
    }

    private static void aplicarOneHot(String valor, HashMapa<String, Integer> mapeo, Vector vector) {
        int k = mapeo.tamano();
        double[] oneHot = new double[k];
        for (int i = 0; i < k; i++) oneHot[i] = 0.0;

        if (valor != null) {
            valor = valor.trim();
            Integer pos = mapeo.obtener(valor);
            if (pos != null && pos < k) oneHot[pos] = 1.0;
        }

        for (int i = 0; i < k; i++) vector.agregar(oneHot[i]);
    }

    private static boolean esNumerico(String valor) {
        if (valor == null || valor.trim().isEmpty()) return false;
        try {
            Double.parseDouble(valor.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static double convertirANumero(String valor) {
        if (valor == null || valor.trim().isEmpty() || valor.equalsIgnoreCase("null")) return 0.0;
        try {
            return Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static int buscarColumnaEtiqueta(Lista<String> columnas) {
        String[] posibles = {"title", "nombre", "name", "original_title"};
        for (String n : posibles)
            for (int i = 0; i < columnas.tamanio(); i++)
                if (columnas.obtener(i).equalsIgnoreCase(n))
                    return i;
        return -1;
    }

    private static String obtenerEtiqueta(Lista<String> columnas, int idxEtiqueta, int contador) {
        if (idxEtiqueta != -1 && idxEtiqueta < columnas.tamanio()) {
            String et = columnas.obtener(idxEtiqueta);
            if (et != null && !et.trim().isEmpty())
                return et.trim();
        }
        return "fila_" + contador;
    }

    private static Lista<String> separarLineaCSV(String line) {
        Lista<String> resultado = new Lista<>();
        StringBuilder actual = new StringBuilder();
        boolean comillas = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (comillas && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    comillas = !comillas;
                }
            } else if (c == ',' && !comillas) {
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
