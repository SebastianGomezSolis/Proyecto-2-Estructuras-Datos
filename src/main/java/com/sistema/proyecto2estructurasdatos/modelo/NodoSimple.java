package com.sistema.proyecto2estructurasdatos.modelo;

// Nodo simple para la lista
public class NodoSimple<T> {
    private T dato;
    private NodoSimple<T> siguiente;

    public NodoSimple(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    public NodoSimple(T dato, NodoSimple<T> siguiente) {
        this.dato = dato;
        this.siguiente = siguiente;
    }

    // Getters y Setters
    public T getDato() { return dato; }
    public NodoSimple<T> getSiguiente() { return siguiente; }

    public void setDato(T dato) { this.dato = dato; }
    public void setSiguiente(NodoSimple<T> siguiente) { this.siguiente = siguiente; }

    // Metodos para ver si posee siguiente o si posee ultimo
    public boolean esUltimo() { return this.siguiente == null; }
    public boolean tieneSiguiente() { return this.siguiente != null; }
}
