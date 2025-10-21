package com.sistema.proyecto2estructurasdatos.modelo;

// Nodo del arbol
public class NodoArbol {
    private String etiqueta;
    private Vector datos;
    private NodoArbol izquierdo;
    private NodoArbol derecho;
    private double distancia;
    private int indiceOriginal;

    public NodoArbol(String etiqueta, Vector datos, int indiceOriginal) {
        this.etiqueta = etiqueta;
        this.datos = datos;
        this.izquierdo = null;
        this.derecho = null;
        this.distancia = 0.0;
        this.indiceOriginal = indiceOriginal;
    }

    public NodoArbol(NodoArbol izquierdo, NodoArbol derecho, double distancia) {
        this.etiqueta = "Cluster";
        this.datos = null;
        this.izquierdo = izquierdo;
        this.derecho = derecho;
        this.distancia = distancia;
        this.indiceOriginal = -1;
    }

    public boolean esHoja() {
        return izquierdo == null && derecho == null;
    }

    // Getters y setters
    public String getEtiqueta() { return etiqueta; }
    public Vector getDatos() { return datos; }
    public NodoArbol getIzquierdo() { return izquierdo; }
    public NodoArbol getDerecho() { return derecho; }
    public double getDistancia() { return distancia; }
    public int getIndiceOriginal() { return indiceOriginal; }

    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
    public void setDatos(Vector datos) { this.datos = datos; }
    public void setIzquierdo(NodoArbol izquierdo) { this.izquierdo = izquierdo; }
    public void setDerecho(NodoArbol derecho) { this.derecho = derecho; }
    public void setDistancia(double distancia) { this.distancia = distancia; }
    public void setIndiceOriginal(int indiceOriginal) { this.indiceOriginal = indiceOriginal; }

}
