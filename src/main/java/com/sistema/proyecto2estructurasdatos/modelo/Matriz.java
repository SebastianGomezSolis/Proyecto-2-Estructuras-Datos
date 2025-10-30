package com.sistema.proyecto2estructurasdatos.modelo;

// Estructura de datos Matriz bidimensional
// Almacena valores double en una estructura de filas por columnas
// Utilizada principalmente para matrices de distancias en clustering
public class Matriz {
    private double[][] datos; // Contenedor de números
    private int filas;        // Cantidad de filas
    private int columnas;     // Cantidad de columnas

    // Constructor que crea una matriz de dimensiones específicas
    // Inicializa todos los valores en 0.0
    public Matriz(int filas, int columnas) {
        if (filas <= 0 || columnas <= 0) {
            throw new IllegalArgumentException(
                    "Las dimensiones deben ser mayores a 0: filas=" + filas + ", columnas=" + columnas
            );
        }
        this.filas = filas;                 // Guarda filas
        this.columnas = columnas;           // Guarda columnas
        this.datos = new double[filas][columnas]; // Crea la matriz

        // Llena con 0
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                datos[i][j] = 0.0;
            }
        }
    }

    // Obtener valor en una posición específica
    public double obtener(int fila, int columna) {
        validarIndices(fila, columna);   // Revisa que exista esa casilla
        return datos[fila][columna];     // Devuelve el número guardado
    }

    // Inseertar valor en una posición específica
    public void insertar(int fila, int columna, double valor) {
        validarIndices(fila, columna);   // Revisa que exista esa casilla
        datos[fila][columna] = valor;    // Coloca el número
    }

    // Validar que los índices estén dentro del rango válido
    private void validarIndices(int fila, int columna) {
        if (fila < 0 || fila >= filas) {
            throw new IndexOutOfBoundsException(
                    "Índice de fila " + fila + " fuera de rango [0, " + filas + ")"
            );
        }
        if (columna < 0 || columna >= columnas) {
            throw new IndexOutOfBoundsException(
                    "Índice de columna " + columna + " fuera de rango [0, " + columnas + ")"
            );
        }
    }

    public int getFilas() { return filas; }         // Devuelve cuántas filas hay

    public int getColumnas() { return columnas; }   // Devuelve cuántas columnas hay

    // Clase interna para representar un par de índices con su valor
    // Usada para retornar la posición del mínimo en la matriz
    public static class ParIndices {
        public final int i;       // Fila
        public final int j;       // Columna
        public final double valor; // Número en esa posición

        public ParIndices(int i, int j, double valor) {
            this.i = i;         // Guarda fila
            this.j = j;         // Guarda columna
            this.valor = valor; // Guarda valor
        }
    }
}
