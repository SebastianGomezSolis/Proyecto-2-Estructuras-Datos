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

    public Matriz(int dimension) {
        this(dimension, dimension);
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

    // Encontrar el valor mínimo en la matriz y su posición
    public ParIndices encontrarMinimo() {
        double minimo = Double.MAX_VALUE;
        int minI = -1;
        int minJ = -1;

        // Buscar el valor mínimo ignorando la diagonal
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                // Ignorar diagonal y valores cero o negativos (inválidos)
                if (i != j && datos[i][j] > 0 && datos[i][j] < minimo) {
                    minimo = datos[i][j];
                    minI = i;
                    minJ = j;
                }
            }
        }

        return new ParIndices(minI, minJ, minimo);
    }

    // Llenar toda la matriz con un valor específico
    public void llenar(double valor) {
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                datos[i][j] = valor;
            }
        }
    }

    // Verificar si la matriz es cuadrada
    public boolean esCuadrada() {
        return filas == columnas;
    }


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

        public boolean esValido() {
            return i >= 0 && j >= 0;
        }
    }
}