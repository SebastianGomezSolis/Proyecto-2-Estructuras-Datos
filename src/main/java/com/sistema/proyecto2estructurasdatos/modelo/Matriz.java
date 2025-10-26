package com.sistema.proyecto2estructurasdatos.modelo;


// Estructura de datos Matriz bidimensional
// Almacena valores double en una estructura de filas por columnas
// Utilizada principalmente para matrices de distancias en clustering
public class Matriz {
    private double[][] datos;
    private int filas;
    private int columnas;

    // Constructor que crea una matriz de dimensiones específicas
    // Inicializa todos los valores en 0.0
    public Matriz(int filas, int columnas) {
        if (filas <= 0 || columnas <= 0) {
            throw new IllegalArgumentException(
                    "Las dimensiones deben ser mayores a 0: filas=" + filas + ", columnas=" + columnas
            );
        }

        this.filas = filas;
        this.columnas = columnas;
        this.datos = new double[filas][columnas];

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                datos[i][j] = 0.0;
            }
        }
    }

    // Obtener valor en una posición específica
    public double obtener(int fila, int columna) {
        validarIndices(fila, columna);
        return datos[fila][columna];
    }

    // Inseertar valor en una posición específica
    public void insertar(int fila, int columna, double valor) {
        validarIndices(fila, columna);
        datos[fila][columna] = valor;
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

    public int getFilas() { return filas; }

    public int getColumnas() { return columnas; }

    // Clase interna para representar un par de índices con su valor
    // Usada para retornar la posición del mínimo en la matriz
    public static class ParIndices {
        public final int i;
        public final int j;
        public final double valor;

        /**
         * Constructor del par de índices
         *
         * @param i Índice de fila
         * @param j Índice de columna
         * @param valor Valor en esa posición
         */
        public ParIndices(int i, int j, double valor) {
            this.i = i;
            this.j = j;
            this.valor = valor;
        }
    }
}