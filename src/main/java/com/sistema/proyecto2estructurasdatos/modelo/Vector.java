package com.sistema.proyecto2estructurasdatos.modelo;

// Estructura de datos Vector personalizada
// Implementa un array dinámico con operaciones matemáticas vectoriales
public class Vector implements Iterable<Double> {
    private Double[] datos;
    private int tamanio;
    private int capacidad;
    private static final int CAPACIDAD_INICIAL = 10;
    private static final int FACTOR_CRECIMIENTO = 2;

    public Vector() {
        this.capacidad = CAPACIDAD_INICIAL;
        this.datos = new Double[capacidad];
        this.tamanio = 0;
    }

    public Vector(int capacidadInicial) {
        if (capacidadInicial <= 0) {
            throw new IllegalArgumentException("La capacidad inicial debe ser mayor a 0");
        }
        this.capacidad = capacidadInicial;
        this.datos = new Double[capacidad];
        this.tamanio = 0;
    }


    public Vector(double[] valores) {
        if (valores == null) {
            throw new IllegalArgumentException("El array de valores no puede ser null");
        }
        this.capacidad = valores.length;
        this.tamanio = valores.length;
        this.datos = new Double[capacidad];

        // Copiar valores
        for (int i = 0; i < valores.length; i++) {
            this.datos[i] = valores[i];
        }
    }

    public void agregar(double valor) {
        // Si el array está lleno, redimensionar
        if (tamanio == capacidad) {
            redimensionar();
        }
        datos[tamanio++] = valor;
    }

    // Redimensionar el array interno cuando se llena
    private void redimensionar() {
        capacidad *= FACTOR_CRECIMIENTO;
        Double[] nuevoArray = new Double[capacidad];

        // Copiar elementos existentes
        for (int i = 0; i < tamanio; i++) {
            nuevoArray[i] = datos[i];
        }

        datos = nuevoArray;
    }

    public double obtener(int indice) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango [0, " + tamanio + ")");
        }
        return datos[indice];
    }

    public void establecer(int indice, double valor) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango [0, " + tamanio + ")");
        }
        datos[indice] = valor;
    }

    public int tamanio() { return tamanio; }


    public int capacidad() { return capacidad; }

    public boolean estaVacio() {
        return tamanio == 0;
    }

    public double[] toArray() {
        double[] resultado = new double[tamanio];
        for (int i = 0; i < tamanio; i++) {
            resultado[i] = datos[i];
        }
        return resultado;
    }

    public double productoPunto(Vector otro) {
        if (otro == null) {
            throw new IllegalArgumentException("El vector no puede ser null");
        }
        if (this.tamanio != otro.tamanio) {
            throw new IllegalArgumentException(
                    "Los vectores deben tener el mismo tamaño: " + this.tamanio + " vs " + otro.tamanio
            );
        }

        double suma = 0.0;
        for (int i = 0; i < tamanio; i++) {
            suma += this.datos[i] * otro.datos[i];
        }
        return suma;
    }

    // Calcular la magnitud (norma euclidiana) del vector
    public double magnitud() {
        double sumaCuadrados = 0.0;
        for (int i = 0; i < tamanio; i++) {
            sumaCuadrados += datos[i] * datos[i];
        }
        return Math.sqrt(sumaCuadrados);
    }

    public Vector clonar() {
        Vector nuevoVector = new Vector(this.capacidad);
        for (int i = 0; i < this.tamanio; i++) {
            nuevoVector.agregar(this.datos[i]);
        }
        return nuevoVector;
    }

    // Implementación del patrón Iterator
    // Permite recorrer el vector con foreach
    @Override
    public Iterador<Double> iterador() {
        return new VectorIterator();
    }

    // Clase interna que implementa Iterator para Vector
    // Permite recorrer los elementos del vector de forma secuencial
    private class VectorIterator implements Iterador<Double> {
        private int indiceActual = 0;

        @Override
        public boolean tieneSiguiente() {
            return indiceActual < tamanio;
        }

        @Override
        public Double siguiente() {
            if (!tieneSiguiente()) {
                throw new java.util.NoSuchElementException("No hay más elementos en el vector");
            }
            return datos[indiceActual++];
        }
    }
}
