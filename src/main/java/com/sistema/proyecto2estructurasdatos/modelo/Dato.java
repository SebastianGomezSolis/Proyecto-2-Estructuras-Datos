package com.sistema.proyecto2estructurasdatos.modelo;

// Representa una fila de datos usado en el clustering
// Guarda el nombre, los valores originales, los valores normalizados y su posición
public class Dato {
    private String etiqueta;         // Nombre o identificación del dato
    private Vector vectorOriginal;   // Valores originales del registro
    private Vector vectorProcesado;  // Valores después de aplicar normalización
    private int indice;              // Posición o número de fila dentro del dataset

    // Crea un dato con su etiqueta, vector y posición
    public Dato(String etiqueta, Vector vectorOriginal, int indice) {
        this.etiqueta = etiqueta;
        this.vectorOriginal = vectorOriginal;
        this.vectorProcesado = null; // Aún no se ha procesado
        this.indice = indice;
    }

    // Métodos para acceder o modificar sus atributos
    public String getEtiqueta() { return etiqueta; }
    public Vector getVectorOriginal() { return vectorOriginal; }
    public Vector getVectorProcesado() { return vectorProcesado; }
    public int getIndice() { return indice; }
    public void setVectorProcesado(Vector vectorProcesado) {           // Asigna los valores procesados
        this.vectorProcesado = vectorProcesado;
    }
}
