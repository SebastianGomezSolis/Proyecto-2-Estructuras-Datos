package com.sistema.proyecto2estructurasdatos.modelo;

// Representa un árbol binario (cada nodo tiene un hijo izquierdo y uno derecho)
public class ArbolBinario {
    private NodoArbol raiz; // Nodo principal del árbol

    // Crea un árbol con una raíz
    public ArbolBinario(NodoArbol raiz) {
        this.raiz = raiz;
    }

    // Devuelve la raíz del árbol
    public NodoArbol getRaiz() {
        return raiz;
    }

}
