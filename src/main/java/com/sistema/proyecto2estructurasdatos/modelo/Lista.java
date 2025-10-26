package com.sistema.proyecto2estructurasdatos.modelo;

import java.util.NoSuchElementException;

// Lista enlazada simple genérica
// Implementa una estructura de datos lineal con nodos enlazados
public class Lista<T> implements Iterable<T> {
    private NodoSimple<T> cabeza;
    private int tamanio;

    // Constructor
    public Lista() {
        this.cabeza = null;
        this.tamanio = 0;
    }

    // Agregar elemento al final de la lista
    public void agregar(T elemento) {
        NodoSimple<T> nuevoNodo = new NodoSimple<>(elemento);

        // Si la lista está vacía, el nuevo nodo es la cabeza
        if (cabeza == null) {
            cabeza = nuevoNodo;
        } else {
            // Recorrer hasta el último nodo
            NodoSimple<T> actual = cabeza;
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nuevoNodo);
        }
        tamanio++;
    }

    // Obtener elemento en una posición específica
    public T obtener(int indice) {
        validarIndice(indice);

        NodoSimple<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }

        return actual.getDato();
    }


    // Eliminar elemento en una posición específica
    public T eliminar(int indice) {
        validarIndice(indice);

        T elementoEliminado;

        // Caso especial: eliminar la cabeza
        if (indice == 0) {
            elementoEliminado = cabeza.getDato();
            cabeza = cabeza.getSiguiente();
        } else {
            // Encontrar el nodo anterior al que queremos eliminar
            NodoSimple<T> anterior = cabeza;
            for (int i = 0; i < indice - 1; i++) {
                anterior = anterior.getSiguiente();
            }

            // Guardar el dato a eliminar y actualizar el enlace
            elementoEliminado = anterior.getSiguiente().getDato();
            anterior.setSiguiente(anterior.getSiguiente().getSiguiente());
        }

        tamanio--;
        return elementoEliminado;
    }

    public int buscar(T elemento) {
        NodoSimple<T> actual = cabeza;
        int indice = 0;

        while (actual != null) {
            if (actual.getDato() == null && elemento == null) {
                return indice;
            }
            if (actual.getDato() != null && actual.getDato().equals(elemento)) {
                return indice;
            }
            actual = actual.getSiguiente();
            indice++;
        }

        return -1; // No encontrado
    }

    // Verificar si la lista contiene un elemento
    public boolean contiene(T elemento) {
        return buscar(elemento) != -1;
    }

    //  Obtener el tamaño de la lista
    public int tamanio() {
        return tamanio;
    }

    // Validar que un índice esté dentro del rango válido
    private void validarIndice(int indice) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango [0, " + tamanio + ")"
            );
        }
    }


    // Implementación del patrón Iterator
    //  Permite recorrer la lista con foreach
    @Override
    public Iterador<T> iterador() {
        return new ListaIterator();
    }

    /**
     * Clase interna que implementa IIterator para Lista
     * Permite recorrer los elementos de la lista de forma secuencial
     */
    private class ListaIterator implements Iterador<T> {
        private NodoSimple<T> actual;

        // Constructor del iterator
        ListaIterator() {
            this.actual = cabeza;
        }

        @Override
        public boolean tieneSiguiente() {
            return actual != null;
        }

        @Override
        public T siguiente() {
            if (!tieneSiguiente()) {
                throw new NoSuchElementException("No hay más elementos en la lista");
            }
            T dato = actual.getDato();
            actual = actual.getSiguiente();
            return dato;
        }
    }
}