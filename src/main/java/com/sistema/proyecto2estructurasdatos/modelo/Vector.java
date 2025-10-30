package com.sistema.proyecto2estructurasdatos.modelo;

// Vector dinámico con operaciones básicas (suma de productos y longitud)
public class Vector implements Iterable<Double> {
    private Double[] datos;                 // Aquí guardamos los números
    private int tamanio;                    // Cuántos números hay realmente
    private int capacidad;                  // Espacio total disponible ahora
    private static final int CAPACIDAD_INICIAL = 10; // Tamaño de inicio
    private static final int FACTOR_CRECIMIENTO = 2; // Cuando se llena, crece al doble

    // Constructor sin parámetros
    public Vector() {
        this.capacidad = CAPACIDAD_INICIAL;   // Arranca con la capacidad base
        this.datos = new Double[capacidad];   // Crea el arreglo interno
        this.tamanio = 0;                     // Aún no hay elementos
    }

    // Constructor con capacidad elegida
    public Vector(int capacidadInicial) {
        if (capacidadInicial <= 0) {                                  // Validación simple
            throw new IllegalArgumentException("La capacidad inicial debe ser mayor a 0");
        }
        this.capacidad = capacidadInicial;                            // Guarda la capacidad
        this.datos = new Double[capacidad];                           // Crea el arreglo interno
        this.tamanio = 0;                                             // Aún vacío
    }

    // Constructor a partir de un arreglo de double
    public Vector(double[] valores) {
        if (valores == null) {                                        // No aceptar null
            throw new IllegalArgumentException("El array de valores no puede ser null");
        }
        this.capacidad = valores.length;                              // Capacidad exacta al inicio
        this.tamanio = valores.length;                                // Tamaño igual a la entrada
        this.datos = new Double[capacidad];                           // Crea el arreglo interno

        // Copiar valores uno a uno
        for (int i = 0; i < valores.length; i++) {                    // Recorre
            this.datos[i] = valores[i];                               // Copia cada número
        }
    }

    // Agrega un número al final
    public void agregar(double valor) {
        // Si no cabe, agrandamos primero
        if (tamanio == capacidad) {
            redimensionar();                                          // Duplicar espacio
        }
        datos[tamanio++] = valor;                                     // Guarda y aumenta el tamaño
    }

    // Aumentar el arreglo interno cuando se llena
    private void redimensionar() {
        capacidad *= FACTOR_CRECIMIENTO;                              // Nueva capacidad (x2)
        Double[] nuevoArray = new Double[capacidad];                  // Arreglo más grande

        // Copiar los elementos existentes
        for (int i = 0; i < tamanio; i++) {                           // Recorre los actuales
            nuevoArray[i] = datos[i];                                 // Pasa cada valor
        }

        datos = nuevoArray;                                           // Reemplaza el arreglo viejo
    }

    // Obtener un valor por su posición
    public double obtener(int indice) {
        if (indice < 0 || indice >= tamanio) {                        // Validación de rango
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango [0, " + tamanio + ")");
        }
        return datos[indice];                                         // Devuelve el valor pedido
    }

    // Cambiar un valor por su posición
    public void establecer(int indice, double valor) {
        if (indice < 0 || indice >= tamanio) {                        // Validación de rango
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango [0, " + tamanio + ")");
        }
        datos[indice] = valor;                                        // Reemplaza el valor
    }

    // Devuelve cuántos elementos hay
    public int tamanio() { return tamanio; }

    // Suma de multiplicaciones elemento a elemento (producto punto)
    public double productoPunto(Vector otro) {
        if (otro == null) {                                           // No aceptar null
            throw new IllegalArgumentException("El vector no puede ser null");
        }
        if (this.tamanio != otro.tamanio) {                           // Deben tener mismo tamaño
            throw new IllegalArgumentException(
                    "Los vectores deben tener el mismo tamaño: " + this.tamanio + " vs " + otro.tamanio
            );
        }

        double suma = 0.0;                                            // Acumula el resultado
        for (int i = 0; i < tamanio; i++) {                           // Recorre cada posición
            suma += this.datos[i] * otro.datos[i];                    // Multiplica y suma
        }
        return suma;                                                  // Devuelve el total
    }

    // Longitud del vector (norma euclidiana)
    public double magnitud() {
        double sumaCuadrados = 0.0;                                   // Acumula cuadrados
        for (int i = 0; i < tamanio; i++) {                           // Recorre cada valor
            sumaCuadrados += datos[i] * datos[i];                     // Cuadrado y suma
        }
        return Math.sqrt(sumaCuadrados);                              // Raíz cuadrada del total
    }

    // Permite usar foreach con nuestro vector (usa nuestro Iterador)
    @Override
    public Iterador<Double> iterador() {
        return new VectorIterator();                                  // Devuelve el iterador
    }

    // Iterador interno para recorrer el vector
    private class VectorIterator implements Iterador<Double> {
        private int indiceActual = 0;                                  // Empieza en el primer elemento

        @Override
        public boolean tieneSiguiente() {
            return indiceActual < tamanio;
        }

        @Override
        public Double siguiente() {
            if (!tieneSiguiente()) {
                throw new java.util.NoSuchElementException("No hay más elementos en el vector");
            }
            return datos[indiceActual++];                               // retorna y avanza
        }
    }
}
