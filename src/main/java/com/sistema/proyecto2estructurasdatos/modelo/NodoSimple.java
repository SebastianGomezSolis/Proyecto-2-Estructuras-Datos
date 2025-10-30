package com.sistema.proyecto2estructurasdatos.modelo; // Ubicación de la clase

// Nodo de una lista enlazada simple
// Guarda un dato y una referencia al siguiente nodo
public class NodoSimple<T> {
    private T dato;                 // Valor que almacena este nodo
    private NodoSimple<T> siguiente; // Enlace al siguiente nodo en la lista

    // Crea un nodo nuevo con un dato y siguiente(null)
    public NodoSimple(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    public T getDato() { return dato; }
    public NodoSimple<T> getSiguiente() { return siguiente; }
    public void setSiguiente(NodoSimple<T> siguiente) { this.siguiente = siguiente; }
}
