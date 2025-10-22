package com.sistema.proyecto2estructurasdatos.modelo;

// Clase que representa un registro de datos para el clustering
// Contiene tanto el vector original como el vector procesado
public class Dato {
    private String etiqueta;
    private Vector vectorOriginal;
    private Vector vectorProcesado;
    private int indice;

    public Dato() {}

    public Dato(String etiqueta, Vector vectorOriginal, int indice) {
        this.etiqueta = etiqueta;
        this.vectorOriginal = vectorOriginal;
        this.vectorProcesado = null; // Se establecerá después del procesamiento
        this.indice = indice;
    }

//    public Dato(String etiqueta, Vector vectorOriginal) {
//        this(etiqueta, vectorOriginal, -1);
//    }

    // Getters y Setters
    public String getEtiqueta() { return etiqueta; }
    public Vector getVectorOriginal() { return vectorOriginal; }
    public Vector getVectorProcesado() { return vectorProcesado; }
    public int getIndice() { return indice; }

    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
    public void setVectorOriginal(Vector vectorOriginal) { this.vectorOriginal = vectorOriginal; }
    public void setVectorProcesado(Vector vectorProcesado) { this.vectorProcesado = vectorProcesado; }
    public void setIndice(int indice) { this.indice = indice; }

    // Verificar si el dato ha sido procesado
    public boolean estaProcesado() { return vectorProcesado != null; }

}
