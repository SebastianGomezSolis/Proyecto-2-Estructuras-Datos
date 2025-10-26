package com.sistema.proyecto2estructurasdatos.modelo;

public class ArbolBinario {
    private NodoArbol raiz;

    public ArbolBinario(NodoArbol raiz) {
        this.raiz = raiz;
    }

    public NodoArbol getRaiz() {
        return raiz;
    }

    // Calcular la altura
    public int altura() {
        return alturaRecursiva(raiz);
    }

    // Método recursivo auxiliar para calcular altura
    private int alturaRecursiva(NodoArbol nodo) {
        if (nodo == null) {
            return 0;
        }
        int alturaIzq = alturaRecursiva(nodo.getIzquierdo());
        int alturaDer = alturaRecursiva(nodo.getDerecho());
        return 1 + Math.max(alturaIzq, alturaDer);
    }

    // Método recursivo auxiliar para contar nodos
    private int contarNodos(NodoArbol nodo) {
        if (nodo == null) {
            return 0;
        }

        return 1 + contarNodos(nodo.getIzquierdo()) + contarNodos(nodo.getDerecho());
    }

}
