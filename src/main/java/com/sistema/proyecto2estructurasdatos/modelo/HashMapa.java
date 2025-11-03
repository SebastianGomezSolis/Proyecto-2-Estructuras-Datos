package com.sistema.proyecto2estructurasdatos.modelo;

// Implementación sencilla de una tabla hash con listas en cada posición

public class HashMapa<K, V> {

    private NodoMapa<K,V>[] tabla;   // arreglo principal donde se guardan los datos
    private int cantidad;            // número total de pares (llave, valor)
    private int capacidad;           // tamaño actual de la tabla
    private static final int capacidad_inicial = 16;  // tamaño inicial por defecto
    private static final double elem_insertados = 0.75;  // límite de llenado antes de redimensionar

    public HashMapa() {
        // Inicializa la tabla vacía con la capacidad inicial
        this.capacidad = capacidad_inicial;
        this.tabla = (NodoMapa<K,V>[]) new NodoMapa[capacidad];
        this.cantidad = 0;
    }

    // Calcula el índice de la tabla correspondiente a una llave
    private int indice(Object llave) {
        int h = (llave == null) ? 0 : llave.hashCode(); // obtiene el hash de la llave
        int mezcla = h ^ (h >>> 16); // mezcla los bits altos y bajos del hash
        return mezcla & (capacidad - 1); // asegura que el índice esté dentro del rango de la tabla
    }

    //Inserta un nuevo par (llave, valor) o reemplaza el valor si la llave ya existe, Devuelve el valor anterior si lo había, o null si es una nueva inserción.

    public V insertar(K llave, V valor) {
        // Si la tabla supera el 75% de ocupación, se expande
        if ((cantidad + 1.0) / capacidad > elem_insertados) {
            rehash();
        }
        int idx = indice(llave);
        NodoMapa<K,V> actual = tabla[idx];

        // Busca si la llave ya está en la lista de esa posición
        while (actual != null) {
            if ((actual.llave == llave) || (actual.llave != null && actual.llave.equals(llave))) {
                V viejo = actual.valor;   // guarda el valor anterior
                actual.valor = valor;     // reemplaza con el nuevo
                return viejo;             // devuelve el valor viejo
            }
            actual = actual.siguiente;
        }

        // Si no existe, se agrega un nuevo nodo al inicio de la lista
        tabla[idx] = new NodoMapa<>(llave, valor, tabla[idx]);
        cantidad++;
        return null;
    }

    //Obtiene el valor asociado a una llav y Devuelve null si la llave no existe.

    public V obtener(K llave) {
        int idx = indice(llave);
        NodoMapa<K,V> actual = tabla[idx];

        // Recorre la lista en esa posición buscando la llave
        while (actual != null) {
            if ((actual.llave == llave) || (actual.llave != null && actual.llave.equals(llave))) {
                return actual.valor;
            }
            actual = actual.siguiente;
        }
        return null;
    }
     //Verifica si una llave está presente en la tabla y Devuelve true si existe, false si no.

    public boolean contieneLlave(K llave) {
        int idx = indice(llave);
        NodoMapa<K,V> actual = tabla[idx];

        // Recorre la lista correspondiente
        while (actual != null) {
            if ((actual.llave == llave) || (actual.llave != null && actual.llave.equals(llave))) {
                return true;
            }
            actual = actual.siguiente;
        }
        return false;
    }

    //Devuelve la cantidad total de elementos almacenados.
    public int tamano() { return cantidad; }

    // Duplica la capacidad de la tabla y reubica todos los nodos según la nueva distribución
    private void rehash() {
        int nuevaCap = capacidad << 1;  // duplica la capacidad
        NodoMapa<K,V>[] nueva = (NodoMapa<K,V>[]) new NodoMapa[nuevaCap];

        // Recorre cada posición de la tabla actual
        for (int i = 0; i < capacidad; i++) {
            NodoMapa<K,V> actual = tabla[i];
            while (actual != null) {
                NodoMapa<K,V> siguiente = actual.siguiente; // guarda el siguiente antes de moverlo
                int h = (actual.llave == null) ? 0 : actual.llave.hashCode();
                int mezcla = h ^ (h >>> 16);
                int idx = mezcla & (nuevaCap - 1);

                // Inserta el nodo en la nueva tabla
                actual.siguiente = nueva[idx];
                nueva[idx] = actual;

                actual = siguiente;
            }
        }
        // Actualiza la referencia de la tabla y su capacidad
        this.tabla = nueva;
        this.capacidad = nuevaCap;
    }
}
