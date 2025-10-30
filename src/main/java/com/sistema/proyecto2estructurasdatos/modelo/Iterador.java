package com.sistema.proyecto2estructurasdatos.modelo;

// Interfaz que define cómo recorrer una lista o colección paso a paso
public interface Iterador<T> {
    // Indica si aún hay más elementos por recorrer
    boolean tieneSiguiente();
    // Devuelve el siguiente elemento de la colección
    T siguiente();
}
