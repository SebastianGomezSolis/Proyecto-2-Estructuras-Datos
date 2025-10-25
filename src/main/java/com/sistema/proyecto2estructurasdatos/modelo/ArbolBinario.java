package com.sistema.proyecto2estructurasdatos.modelo;

public class ArbolBinario {
    private NodoArbol raiz;

    public ArbolBinario(NodoArbol raiz) {
        this.raiz = raiz;
    }

    public ArbolBinario() {
        this.raiz = null;
    }

    public NodoArbol getRaiz() {
        return raiz;
    }

    public void setRaiz(NodoArbol raiz) {
        this.raiz = raiz;
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

    // Contar el número total de nodos en el árbol
    public int contarNodos() {
        return contarNodos(raiz);
    }

    // Método recursivo auxiliar para contar nodos
    private int contarNodos(NodoArbol nodo) {
        if (nodo == null) {
            return 0;
        }

        return 1 + contarNodos(nodo.getIzquierdo()) + contarNodos(nodo.getDerecho());
    }

    // Contar solo las hojas del árbol
    public int contarHojas() {
        return contarHojas(raiz);
    }

    // Método recursivo auxiliar para contar hojas
    private int contarHojas(NodoArbol nodo) {
        if (nodo == null) {
            return 0;
        }

        if (nodo.esHoja()) {
            return 1;
        }

        return contarHojas(nodo.getIzquierdo()) + contarHojas(nodo.getDerecho());
    }

    // Recorrido en orden del árbol
    public Lista<NodoArbol> recorridoEnOrden() {
        Lista<NodoArbol> resultado = new Lista<>();
        recorridoEnOrden(raiz, resultado);
        return resultado;
    }

    // Método recursivo auxiliar para recorrido en orden
    private void recorridoEnOrden(NodoArbol nodo, Lista<NodoArbol> resultado) {
        if (nodo == null) {
            return;
        }

        recorridoEnOrden(nodo.getIzquierdo(), resultado);
        resultado.agregar(nodo);
        recorridoEnOrden(nodo.getDerecho(), resultado);
    }
}
