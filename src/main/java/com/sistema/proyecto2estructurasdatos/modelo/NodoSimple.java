package com.sistema.proyecto2estructurasdatos.modelo;

// Nodo simple para la lista
public class NodoSimple<T> {
    private T dato;
    private NodoSimple<T> siguiente;

    public NodoSimple(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }


    // Getters y Setters
    public T getDato() { return dato; }
    public NodoSimple<T> getSiguiente() { return siguiente; }
    public void setSiguiente(NodoSimple<T> siguiente) { this.siguiente = siguiente; }
}
