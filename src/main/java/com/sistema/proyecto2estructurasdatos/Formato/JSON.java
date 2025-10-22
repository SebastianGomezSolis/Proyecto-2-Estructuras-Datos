package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.ArbolBinario;
import java.io.IOException;
import java.io.FileWriter;

public class JSON {
    /**
     * Exporta un dendrograma a archivo JSON
     * @param arbol Árbol binario del dendrograma
     * @param rutaArchivo Ruta donde guardar el archivo
     */
    public static void exportar(ArbolBinario arbol, String rutaArchivo) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"dendrograma\": ");

        if (arbol.getRaiz() != null) {
            construirJSON(arbol.getRaiz(), json, 2);
        } else {
            json.append("null");
        }

        json.append("\n}");

        // Escribir a archivo
        try (FileWriter writer = new FileWriter(rutaArchivo)) {
            writer.write(json.toString());
        }
    }

    /**
     * Construye recursivamente la representación JSON del árbol
     * Complejidad: O(n)
     */
    private static void construirJSON(ArbolBinario.Nodo nodo, StringBuilder json, int nivel) {
        if (nodo == null) {
            json.append("null");
            return;
        }

        String indentacion = obtenerIndentacion(nivel);
        String indentacionHija = obtenerIndentacion(nivel + 1);

        json.append("{\n");

        // Etiqueta
        json.append(indentacionHija).append("\"etiqueta\": \"")
                .append(escaparJSON(nodo.getEtiqueta())).append("\",\n");

        // Distancia
        json.append(indentacionHija).append("\"distancia\": ")
                .append(String.format("%.6f", nodo.getDistancia())).append(",\n");

        // Es hoja
        json.append(indentacionHija).append("\"esHoja\": ")
                .append(nodo.esHoja()).append(",\n");

        // Índice original (solo para hojas)
        if (nodo.esHoja()) {
            json.append(indentacionHija).append("\"indiceOriginal\": ")
                    .append(nodo.getIndiceOriginal()).append(",\n");
        }

        // Datos (solo para hojas)
        if (nodo.esHoja() && nodo.getDatos() != null) {
            json.append(indentacionHija).append("\"datos\": [");
            for (int i = 0; i < nodo.getDatos().tamanio(); i++) {
                json.append(String.format("%.6f", nodo.getDatos().obtener(i)));
                if (i < nodo.getDatos().tamanio() - 1) {
                    json.append(", ");
                }
            }
            json.append("],\n");
        }

        // Hijo izquierdo
        json.append(indentacionHija).append("\"izquierdo\": ");
        if (nodo.getIzquierdo() != null) {
            construirJSON(nodo.getIzquierdo(), json, nivel + 1);
        } else {
            json.append("null");
        }
        json.append(",\n");

        // Hijo derecho
        json.append(indentacionHija).append("\"derecho\": ");
        if (nodo.getDerecho() != null) {
            construirJSON(nodo.getDerecho(), json, nivel + 1);
        } else {
            json.append("null");
        }
        json.append("\n");

        json.append(indentacion).append("}");
    }

    /**
     * Genera una cadena de indentación
     */
    private static String obtenerIndentacion(int nivel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivel; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    /**
     * Escapa caracteres especiales para JSON
     */
    private static String escaparJSON(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Importa un dendrograma desde un archivo JSON
     * (Método simplificado - en una implementación completa sería más robusto)
     */
    public static ArbolBinario importar(String rutaArchivo) throws IOException {
        // Por simplicidad, dejamos esta funcionalidad como stub
        // En una implementación completa, se usaría un parser JSON
        throw new UnsupportedOperationException("Importación JSON no implementada en esta versión");
    }
}
