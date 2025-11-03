package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.NodoArbol;
import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Clase que representa un cluster durante el proceso de agrupamiento.
 *
 * Un cluster puede ser:
 * - Un dato individual (hoja del árbol)
 * - Un grupo de datos fusionados (nodo interno del árbol)
 *
 * Mantiene información para calcular centroides eficientemente sin
 * recorrer todas las hojas cada vez.
 */

public class Cluster {
    NodoArbol nodo;        // Nodo del árbol que representa este cluster
    int indiceMatriz;      // Posición en la matriz de distancias

    // === Optimización para centroides ===
    // En vez de recalcular el centroide cada vez, guardamos la suma acumulada
    Vector suma;   // Suma de todos los vectores de las hojas
    int count;     // Cantidad de hojas en el cluster

    /**
     * crea un cluster a partir de un nodo (generalmente una hoja).
     */
    Cluster(NodoArbol nodo, int indiceMatriz) {
        this.nodo = nodo;
        this.indiceMatriz = indiceMatriz;

        // Inicializar suma y contador
        if (nodo != null && nodo.getDatos() != null) {
            Vector v = nodo.getDatos();
            this.suma = new Vector(v.tamanio());
            // Copiar el vector a 'suma'
            for (int k = 0; k < v.tamanio(); k++) {
                this.suma.agregar(v.obtener(k));
            }
            this.count = 1; // Es una hoja, así que count = 1
        } else {
            this.suma = new Vector();
            this.count = 0;
        }
    }
}