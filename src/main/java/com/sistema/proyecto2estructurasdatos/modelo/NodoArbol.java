package com.sistema.proyecto2estructurasdatos.modelo;

// Nodo del arbol
public class NodoArbol {
    private String etiqueta;        // Nombre del nodo (dato o "Cluster")
    private Vector datos;           // Valores del elemento (si es hoja)
    private NodoArbol izquierdo;    // Hijo izquierdo
    private NodoArbol derecho;      // Hijo derecho
    private double distancia;       // “Altura” donde se unió este nodo
    private int indiceOriginal;     // Posición original en el dataset (si aplica)

    public NodoArbol(String etiqueta, Vector datos, int indiceOriginal) {
        this.etiqueta = etiqueta;       // Guarda el nombre
        this.datos = datos;             // Guarda los valores del dato
        this.izquierdo = null;          // sin hijos
        this.derecho = null;            // sin hijos
        this.distancia = 0.0;           // Hoja: sin distancia de unión
        this.indiceOriginal = indiceOriginal; // Recuerda su fila original
    }

    public NodoArbol(NodoArbol izquierdo, NodoArbol derecho, double distancia) {
        this.etiqueta = " ";
        this.datos = null;          // Un cluster no guarda un vector directo
        this.izquierdo = izquierdo; // Enlace al hijo izquierdo
        this.derecho = derecho;     // Enlace al hijo derecho
        this.distancia = distancia; // Nivel donde se juntaron sus hijos
        this.indiceOriginal = -1;   // No corresponde a una fila específica
    }

    public boolean esHoja() {
        return izquierdo == null && derecho == null; // Es hoja si no tiene hijos
    }

    // Getters y setters
    public String getEtiqueta() { return etiqueta; }
    public Vector getDatos() { return datos; }
    public NodoArbol getIzquierdo() { return izquierdo; }
    public NodoArbol getDerecho() { return derecho; }
    public double getDistancia() { return distancia; }
    public void setDatos(Vector datos) { this.datos = datos; }
    public void setDistancia(double distancia) { this.distancia = distancia; }
}
