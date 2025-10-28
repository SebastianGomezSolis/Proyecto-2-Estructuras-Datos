package com.sistema.proyecto2estructurasdatos.modelo;

import java.util.NoSuchElementException;

/**
 * Lista enlazada simple genérica.
 * - Inserción al final en O(1) gracias a 'cola'.
 * - Corrige eliminación del último nodo actualizando 'cola'.
 * - Implementa SOLO tus interfaces personalizadas: Iterable/Iterador.
 */
public class Lista<T> implements Iterable<T> {

    private NodoSimple<T> cabeza;
    private NodoSimple<T> cola;   // puntero a la cola para agregar en O(1)
    private int tamanio;

    public Lista() {
        this.cabeza = null;
        this.cola = null;
        this.tamanio = 0;
    }

    // Agregar al final en O(1)
    public void agregar(T elemento) {
        NodoSimple<T> nuevo = new NodoSimple<>(elemento);
        if (cabeza == null) {
            cabeza = nuevo;
            cola = nuevo;
        } else {
            cola.setSiguiente(nuevo);
            cola = nuevo;
        }
        tamanio++;
    }

    public T obtener(int indice) {
        validarIndice(indice);
        NodoSimple<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    public T eliminar(int indice) {
        validarIndice(indice);
        T eliminado;

        if (indice == 0) {
            eliminado = cabeza.getDato();
            cabeza = cabeza.getSiguiente();
            if (cabeza == null) cola = null; // lista quedó vacía
        } else {
            NodoSimple<T> anterior = cabeza;
            for (int i = 0; i < indice - 1; i++) {
                anterior = anterior.getSiguiente();
            }
            NodoSimple<T> rem = anterior.getSiguiente();
            eliminado = rem.getDato();
            NodoSimple<T> sig = rem.getSiguiente();
            anterior.setSiguiente(sig);
            if (sig == null) cola = anterior; // si borré el último, actualizo cola
        }
        tamanio--;
        return eliminado;
    }

    public int buscar(T elemento) {
        NodoSimple<T> actual = cabeza;
        int idx = 0;
        while (actual != null) {
            T dato = actual.getDato();
            if ((dato == null && elemento == null) ||
                    (dato != null && dato.equals(elemento))) {
                return idx;
            }
            actual = actual.getSiguiente();
            idx++;
        }
        return -1;
    }

    public boolean contiene(T elemento) { return buscar(elemento) != -1; }
    public int tamanio() { return tamanio; }
    public boolean estaVacia() { return tamanio == 0; }

    public void limpiar() {
        cabeza = null;
        cola = null;
        tamanio = 0;
    }

    private void validarIndice(int indice) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango [0, " + tamanio + ")"
            );
        }
    }

    /* ===== Iterador personalizado ===== */

    @Override // <- este @Override es válido porque implementas TU Iterable<T>
    public Iterador<T> iterador() {
        return new ListaIterador();
    }

    private class ListaIterador implements Iterador<T> {
        private NodoSimple<T> actual = cabeza;

        @Override
        public boolean tieneSiguiente() {
            return actual != null;
        }

        @Override
        public T siguiente() {
            if (actual == null) {
                throw new NoSuchElementException("No hay más elementos en la lista");
            }
            T dato = actual.getDato();
            actual = actual.getSiguiente();
            return dato;
        }
    }
}
