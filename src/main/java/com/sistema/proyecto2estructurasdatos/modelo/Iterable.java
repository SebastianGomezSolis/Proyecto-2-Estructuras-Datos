package com.sistema.proyecto2estructurasdatos.modelo;

// Interfaz Iterable personalizada
// Equivalente a java.lang.Iterable pero implementada desde cero
// Permite que una colección pueda ser recorrida con un iterador
public interface Iterable<T> {
    Iterador<T> iterador();
}
