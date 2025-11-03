package com.sistema.proyecto2estructurasdatos.modelo;

/**
 * Representa un nodo dentro de la tabla hash.
 * Cada nodo guarda una llave, su valor y una referencia al siguiente nodo.
 */
public class NodoMapa<K, V> {
    final K llave;               // clave asociada al valor
    V valor;                     // dato o valor almacenado
    NodoMapa<K, V> siguiente;    // referencia al siguiente nodo en la misma posición de la tabla

    // crea un nodo con su llave, valor y enlace al siguiente
    public NodoMapa(K llave, V valor, NodoMapa<K, V> siguiente) {
        this.llave = llave;
        this.valor = valor;
        this.siguiente = siguiente;
    }
}
