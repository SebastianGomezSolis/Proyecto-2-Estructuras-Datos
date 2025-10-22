package com.sistema.proyecto2estructurasdatos.modelo;

// Interfaz Iterator personalizada
// Equivalente a java.util.Iterator pero implementada desde cero
// Permite recorrer colecciones de forma secuencial
public interface Iterador<T> {
    boolean tieneSiguiente();
    T siguiente();
}
