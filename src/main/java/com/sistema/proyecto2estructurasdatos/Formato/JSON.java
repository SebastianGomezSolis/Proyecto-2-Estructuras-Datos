/*package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.ArbolBinario;
import com.sistema.proyecto2estructurasdatos.modelo.NodoArbol;
import java.io.IOException;
import java.io.FileWriter;

public class JSON {

    public static void exportar(ArbolBinario arbol, String rutaArchivo) throws IOException {
        StringBuilder json = new StringBuilder();

        if (arbol.getRaiz() != null) {
            construirJSON(arbol.getRaiz(), json, 0);
        } else {
            json.append("null");
        }

        // Escribir a archivo
        try (FileWriter writer = new FileWriter(rutaArchivo)) {
            writer.write(json.toString());
        }
    }

    // Construye recursivamente el JSON en formato compatible con el visualizador
    private static void construirJSON(NodoArbol nodo, StringBuilder json, int nivel) {
        if (nodo == null) {
            json.append("null");
            return;
        }

        String indentacion = obtenerIndentacion(nivel);
        String indentacionHija = obtenerIndentacion(nivel + 1);

        json.append("{\n");

        // "n" - nombre/etiqueta
        json.append(indentacionHija).append("\"n\": \"")
                .append(escaparJSON(nodo.getEtiqueta())).append("\",\n");

        // "d" - distancia
        json.append(indentacionHija).append("\"d\": ")
                .append(String.format("%.6f", nodo.getDistancia())).append(",\n");

        // "c" - children (hijos)
        json.append(indentacionHija).append("\"c\": ");

        if (nodo.esHoja()) {
            // Si es hoja, array vacío
            json.append("[]");
        } else {
            // Si tiene hijos, crear array con izquierdo y derecho
            json.append("[\n");

            if (nodo.getIzquierdo() != null) {
                json.append(obtenerIndentacion(nivel + 2));
                construirJSON(nodo.getIzquierdo(), json, nivel + 2);
            }

            if (nodo.getDerecho() != null) {
                json.append(",\n");
                json.append(obtenerIndentacion(nivel + 2));
                construirJSON(nodo.getDerecho(), json, nivel + 2);
            }

            json.append("\n").append(indentacionHija).append("]");
        }

        json.append("\n");
        json.append(indentacion).append("}");
    }

    private static String obtenerIndentacion(int nivel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivel; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    private static String escaparJSON(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static ArbolBinario importar(String rutaArchivo) throws IOException {
        throw new UnsupportedOperationException("Importación JSON no implementada en esta versión");
    }
}*/

package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.ArbolBinario;
import com.sistema.proyecto2estructurasdatos.modelo.NodoArbol;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Locale;

/**
 * Clase para exportar dendrogramas en formato JSON.
 * El campo "n" se genera al estilo test.json:
 *   - Hojas: "(id)"
 *   - Internos: "(id1,id2,...)"
 * El campo "d" conserva la distancia del nodo.
 * El campo "c" contiene los hijos (izquierdo, derecho).
 */
public class JSON {

    /**
     * Exporta un árbol binario (dendrograma) a un archivo JSON.
     *
     * @param arbol       El árbol binario a exportar
     * @param rutaArchivo Ruta del archivo de salida
     * @throws IOException Si ocurre un error al escribir el archivo
     */
    public static void exportar(ArbolBinario arbol, String rutaArchivo) throws IOException {
        StringBuilder json = new StringBuilder();

        if (arbol.getRaiz() != null) {
            // 1) Asignar IDs a hojas de izquierda a derecha
            Map<NodoArbol, Integer> leafIds = new HashMap<>();
            int[] counter = {1};
            asignarIdsHojas(arbol.getRaiz(), leafIds, counter);

            // 2) Construir JSON usando las tuplas en "n"
            construirJSON(arbol.getRaiz(), json, 0, leafIds);
        } else {
            json.append("null");
        }

        try (FileWriter writer = new FileWriter(rutaArchivo)) {
            writer.write(json.toString());
        }
    }

    /**
     * Recorre el árbol y asigna un ID incremental a cada hoja,
     * en orden izquierda → derecha (in-order).
     */
    private static void asignarIdsHojas(NodoArbol nodo,
                                        Map<NodoArbol, Integer> leafIds,
                                        int[] counter) {
        if (nodo == null) return;

        if (nodo.esHoja()) {
            leafIds.put(nodo, counter[0]++);
            return;
        }

        asignarIdsHojas(nodo.getIzquierdo(), leafIds, counter);
        asignarIdsHojas(nodo.getDerecho(), leafIds, counter);
    }

    /**
     * Construye recursivamente la representación JSON del dendrograma.
     * Estructura generada:
     * {
     *   "n": "(id)" o "(id1,id2,...)",
     *   "d": distancia,
     *   "c": [ hijo_izq, hijo_der ]   // [] si es hoja
     * }
     */
    private static void construirJSON(NodoArbol nodo,
                                      StringBuilder json,
                                      int nivel,
                                      Map<NodoArbol, Integer> leafIds) {
        if (nodo == null) {
            json.append("null");
            return;
        }

        String indent = obtenerIndentacion(nivel);
        String indentChild = obtenerIndentacion(nivel + 1);

        json.append("{\n");

        // "n" — etiqueta como tupla (estilo test.json)
        String etiqueta = etiquetaTupla(nodo, leafIds);
        json.append(indentChild).append("\"n\": \"")
                .append(escaparJSON(etiqueta)).append("\",\n");

        // "d" — distancia (punto decimal con Locale.US)
        json.append(indentChild).append("\"d\": ")
                .append(String.format(Locale.US, "%.6f", nodo.getDistancia()))
                .append(",\n");

        // "c" — hijos
        json.append(indentChild).append("\"c\": ");
        if (nodo.esHoja()) {
            json.append("[]");
        } else {
            json.append("[\n");

            // hijo izquierdo
            if (nodo.getIzquierdo() != null) {
                json.append(obtenerIndentacion(nivel + 2));
                construirJSON(nodo.getIzquierdo(), json, nivel + 2, leafIds);
            } else {
                json.append(obtenerIndentacion(nivel + 2)).append("null");
            }

            json.append(",\n");

            // hijo derecho
            if (nodo.getDerecho() != null) {
                json.append(obtenerIndentacion(nivel + 2));
                construirJSON(nodo.getDerecho(), json, nivel + 2, leafIds);
            } else {
                json.append(obtenerIndentacion(nivel + 2)).append("null");
            }

            json.append("\n").append(indentChild).append("]");
        }

        json.append("\n").append(indent).append("}");
    }

    /**
     * Genera la etiqueta estilo test.json:
     * - Si es hoja: "(id)"
     * - Si es interno: "(id1,id2,...)"
     */
    private static String etiquetaTupla(NodoArbol nodo, Map<NodoArbol, Integer> leafIds) {
        if (nodo.esHoja()) {
            Integer id = leafIds.get(nodo);
            return "(" + (id == null ? "?" : id) + ")";
        }
        SortedSet<Integer> ids = new TreeSet<>();
        recolectarIdsHojas(nodo, leafIds, ids);

        StringBuilder sb = new StringBuilder("(");
        int k = 0;
        for (int id : ids) {
            if (k++ > 0) sb.append(",");
            sb.append(id);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Agrega al conjunto 'acc' los IDs de las hojas bajo 'nodo'.
     */
    private static void recolectarIdsHojas(NodoArbol nodo,
                                           Map<NodoArbol, Integer> leafIds,
                                           Set<Integer> acc) {
        if (nodo == null) return;

        if (nodo.esHoja()) {
            Integer id = leafIds.get(nodo);
            if (id != null) acc.add(id);
            return;
        }

        recolectarIdsHojas(nodo.getIzquierdo(), leafIds, acc);
        recolectarIdsHojas(nodo.getDerecho(), leafIds, acc);
    }

    /**
     * Genera la indentación para el nivel especificado (2 espacios por nivel).
     */
    private static String obtenerIndentacion(int nivel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivel; i++) sb.append("  ");
        return sb.toString();
    }

    /**
     * Escapa caracteres especiales para JSON.
     */
    private static String escaparJSON(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
