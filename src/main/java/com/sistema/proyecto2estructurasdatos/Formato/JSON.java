package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.ArbolBinario;
import com.sistema.proyecto2estructurasdatos.modelo.NodoArbol;
import com.sistema.proyecto2estructurasdatos.modelo.HashMapa;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Esta clase se encarga de exportar "dendrogramas" (árboles de agrupamiento) a archivos JSON.
 *
 * El JSON que genera tiene esta estructura:
 *   - "n": Es como el nombre del nodo. Para hojas es "(id)" y para ramas es "(id1,id2,...)"
 *   - "d": Es la distancia o altura en el árbol (qué tan separados están los grupos)
 *   - "c": Son los hijos (las ramitas que salen de este nodo)
 */

public class JSON {

    /**
     * Este es el método principal que toma un árbol y lo guarda en un archivo JSON.
     *
     * arbol       El árbol que queremos guardar
     * rutaArchivo Dónde queremos guardar el archivo (ejemplo: "resultado.json")
     * IOException Si algo sale mal al intentar escribir el archivo
     */
    public static void exportar(ArbolBinario arbol, String rutaArchivo) throws IOException {
        // Creamos un StringBuilder, que es como un cuaderno donde vamos escribiendo el JSON
        StringBuilder json = new StringBuilder();

        // Verificamos que el árbol no esté vacío
        if (arbol.getRaiz() != null) {
            // PASO 1: Asignar números (IDs) a cada hoja del árbol
            // Las hojas son los nodos finales (los que no tienen hijos)
            // Los numeramos de izquierda a derecha: 1, 2, 3, 4...

            // Este mapa guardará: cada hoja → su número
            HashMapa<NodoArbol, Integer> leafIds = new HashMapa<>();

            // Usamos un array de un elemento como "truco" para poder modificar
            // el contador dentro de métodos recursivos
            int[] counter = {1}; // Empezamos a contar desde 1

            // Recorremos el árbol y asignamos IDs
            asignarIdsHojas(arbol.getRaiz(), leafIds, counter);

            // PASO 2: Construir el texto JSON usando esos IDs
            construirJSON(arbol.getRaiz(), json, 0, leafIds);
        } else {
            // Si el árbol está vacío, simplemente ponemos "null"
            json.append("null");
        }

        // Escribimos el JSON en el archivo
        try (FileWriter writer = new FileWriter(rutaArchivo)) {
            writer.write(json.toString());
        }
    }

    /**
     * Este método recorre el árbol y asigna un número a cada hoja,
     * yendo de izquierda a derecha.
     */
    private static void asignarIdsHojas(NodoArbol nodo,
                                        HashMapa<NodoArbol, Integer> leafIds,
                                        int[] counter) {
        // Si llegamos a un nodo vacío, nos devolvemos
        if (nodo == null) return;

        // Si es una hoja (no tiene hijos), le asignamos un número
        if (nodo.esHoja()) {
            leafIds.insertar(nodo, counter[0]++); // Le damos el número actual y lo aumentamos
            return;
        }

        // Si no es hoja, seguimos explorando sus hijos
        // Primero vamos a la izquierda
        asignarIdsHojas(nodo.getIzquierdo(), leafIds, counter);
        // Luego vamos a la derecha
        asignarIdsHojas(nodo.getDerecho(), leafIds, counter);
    }

    /**
     * Este metodo construye el texto JSON recursivamente (llamándose a sí mismo).
     *
     * Va creando el JSON nodo por nodo, como si estuvieras describiendo el árbol
     * rama por rama.
     *
     * La estructura que crea es:
     * {
     *   "n": "(id)" o "(id1,id2,...)",  ← El nombre/etiqueta
     *   "d": distancia,                  ← La altura o distancia
     *   "c": [ hijo_izq, hijo_der ]      ← Los hijos (vacío [] si es hoja)
     * }
     */
    private static void construirJSON(NodoArbol nodo, StringBuilder json, int nivel, HashMapa<NodoArbol, Integer> leafIds) {
        // Si el nodo es nulo, escribimos "null" y listo
        if (nodo == null) {
            json.append("null");
            return;
        }

        // Calculamos la indentación (los espacios al inicio) para que se vea bonito
        // Cada nivel del árbol tiene más espacios
        String indent = obtenerIndentacion(nivel);
        String indentChild = obtenerIndentacion(nivel + 1);

        // Comenzamos a escribir este objeto JSON
        json.append("{\n");

        // === CAMPO "n": El nombre del nodo ===
        // Generamos la etiqueta como tupla: "(1)" para hojas, "(1,2,3)" para ramas
        String etiqueta = etiquetaTupla(nodo, leafIds);
        json.append(indentChild).append("\"n\": \"")
                .append(escaparJSON(etiqueta)).append("\",\n");

        // === CAMPO "d": La distancia ===
        // Guardamos la distancia con 6 decimales, usando punto (no coma)
        // Locale.US asegura que use punto decimal (1.5 en vez de 1,5)
        json.append(indentChild).append("\"d\": ")
                .append(String.format(Locale.US, "%.6f", nodo.getDistancia()))
                .append(",\n");

        // === CAMPO "c": Los hijos ===
        json.append(indentChild).append("\"c\": ");

        if (nodo.esHoja()) {
            // Si es una hoja, no tiene hijos, así que ponemos []
            json.append("[]");
        } else {
            // Si tiene hijos, los escribimos en un array
            json.append("[\n");

            // Escribimos el hijo izquierdo
            if (nodo.getIzquierdo() != null) {
                json.append(obtenerIndentacion(nivel + 2));
                // Llamada recursiva: procesamos el hijo izquierdo
                construirJSON(nodo.getIzquierdo(), json, nivel + 2, leafIds);
            } else {
                json.append(obtenerIndentacion(nivel + 2)).append("null");
            }

            json.append(",\n"); // Separamos con una coma

            // Escribimos el hijo derecho
            if (nodo.getDerecho() != null) {
                json.append(obtenerIndentacion(nivel + 2));
                // Llamada recursiva: procesamos el hijo derecho
                construirJSON(nodo.getDerecho(), json, nivel + 2, leafIds);
            } else {
                json.append(obtenerIndentacion(nivel + 2)).append("null");
            }

            // Cerramos el array de hijos
            json.append("\n").append(indentChild).append("]");
        }

        // Cerramos el objeto JSON de este nodo
        json.append("\n").append(indent).append("}");
    }

    /**
     * Este método genera la etiqueta (el campo "n") para cada nodo.
     *
     * El formato es:
     * - Si es hoja: "(3)" donde 3 es su número
     * - Si es rama interna: "(1,2,3)" con todos los números de las hojas que tiene debajo
     *
     * Por ejemplo, si una rama tiene las hojas 1, 2 y 3, su etiqueta será "(1,2,3)"
     */
    private static String etiquetaTupla(NodoArbol nodo, HashMapa<NodoArbol, Integer> leafIds) {
        // Si es una hoja, solo ponemos su número
        if (nodo.esHoja()) {
            Integer id = leafIds.obtener(nodo);
            return "(" + (id == null ? "?" : id) + ")";
        }

        // Si es un nodo interno, recolectamos todos los IDs de las hojas que tiene debajo
        // Usamos un TreeSet para que queden ordenados automáticamente
        SortedSet<Integer> ids = new TreeSet<>();
        recolectarIdsHojas(nodo, leafIds, ids);

        // Construimos la tupla: "(1,2,3,4)"
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
     * Este método recolecta todos los IDs de las hojas que están debajo de un nodo.
     */
    private static void recolectarIdsHojas(NodoArbol nodo, HashMapa<NodoArbol, Integer> leafIds, Set<Integer> acc) {
        // Si no hay nodo, no hay nada que hacer
        if (nodo == null) return;

        // Si llegamos a una hoja, agregamos su ID a la colección
        if (nodo.esHoja()) {
            Integer id = leafIds.obtener(nodo);
            if (id != null) acc.add(id);
            return;
        }

        // Si no es hoja, seguimos buscando en sus hijos
        recolectarIdsHojas(nodo.getIzquierdo(), leafIds, acc);
        recolectarIdsHojas(nodo.getDerecho(), leafIds, acc);
    }

    /**
     * Genera los espacios de indentación para hacer el JSON más legible.
     *
     * Cada nivel del árbol tiene 2 espacios más.
     * Nivel 0 = sin espacios
     * Nivel 1 = 2 espacios "  "
     * Nivel 2 = 4 espacios "    "
     * etc.
     */
    private static String obtenerIndentacion(int nivel) {
        StringBuilder sb = new StringBuilder();
        // Por cada nivel, agregamos 2 espacios
        for (int i = 0; i < nivel; i++) sb.append("  ");
        return sb.toString();
    }

    /**
     * "Escapa" caracteres especiales en el texto para que sean válidos en JSON.
     *
     * Algunos caracteres tienen significados especiales en JSON y necesitan
     * ser escritos de forma especial:
     * - Las comillas " se convierten en \"
     * - Las barras invertidas \ se convierten en \\
     * - Los saltos de línea se convierten en \n
     */
    private static String escaparJSON(String texto) {
        // Si el texto está vacío, devolvemos texto vacío
        if (texto == null) return "";

        // Hacemos los reemplazos necesarios
        return texto.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
