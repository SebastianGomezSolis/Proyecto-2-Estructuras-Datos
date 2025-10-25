package com.sistema.proyecto2estructurasdatos.Formato;

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
}