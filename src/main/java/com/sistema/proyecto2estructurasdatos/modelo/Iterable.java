package com.sistema.proyecto2estructurasdatos.modelo;

// Interfaz genérica para poder recorrer la colección propia
public interface Iterable<T> {
    // Devuelve un iterador para ir elemento por elemento
    Iterador<T> iterador();
}
