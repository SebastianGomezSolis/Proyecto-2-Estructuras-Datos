package com.sistema.proyecto2estructurasdatos.modelo;

import java.util.NoSuchElementException;

/**
 * Lista enlazada simple genérica.
 * Inserta al final rápido usando 'cola'.
 * Implementa interfaces propias Iterable/Iterador.
 */
public class Lista<T> implements Iterable<T> {

    private NodoSimple<T> cabeza; // primer nodo
    private NodoSimple<T> cola;   // último nodo (para agregar rápido)
    private int tamanio;          // cuántos elementos hay

    public Lista() {
        this.cabeza = null; // lista vacía
        this.cola = null;   // sin último
        this.tamanio = 0;   // tamaño 0
    }

    // Agrega al final en O(1)
    public void agregar(T elemento) {
        NodoSimple<T> nuevo = new NodoSimple<>(elemento); // crea nodo
        if (cabeza == null) {       // si está vacía
            cabeza = nuevo;         // cabeza apunta al nuevo
            cola = nuevo;           // cola también
        } else {
            cola.setSiguiente(nuevo); // enlaza al final
            cola = nuevo;             // mueve la cola
        }
        tamanio++; // aumenta el tamaño
    }

    // Devuelve el elemento en 'indice'
    public T obtener(int indice) {
        validarIndice(indice);          // chequea rango
        NodoSimple<T> actual = cabeza;  // arranca en cabeza
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente(); // avanza
        }
        return actual.getDato(); // regresa el dato
    }

    public int tamanio() { return tamanio; }                               // cuántos hay

    // Valida que el índice esté dentro del tamaño
    private void validarIndice(int indice) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango [0, " + tamanio + ")"
            );
        }
    }

    @Override // implementa Iterable<T>
    public Iterador<T> iterador() {
        return new ListaIterador(); // devuelve el iterador
    }

    // Recorre la lista nodo por nodo
    private class ListaIterador implements Iterador<T> {
        private NodoSimple<T> actual = cabeza; // empieza en cabeza

        @Override
        public boolean tieneSiguiente() {
            return actual != null; // hay más si no es null
        }

        @Override
        public T siguiente() {
            if (actual == null) {
                throw new NoSuchElementException("No hay más elementos en la lista");
            }
            T dato = actual.getDato();            // toma el dato
            actual = actual.getSiguiente();       // avanza
            return dato;                          // retorna el dato
        }
    }
}
