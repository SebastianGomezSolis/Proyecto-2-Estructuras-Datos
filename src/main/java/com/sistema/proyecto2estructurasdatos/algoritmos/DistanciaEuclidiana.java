package com.sistema.proyecto2estructurasdatos.algoritmos;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Distancia Euclidiana
 * Mide la distancia en línea recta entre dos puntos
 */

public class DistanciaEuclidiana implements IDistancia {

    @Override
    public double calcular(Vector v1, Vector v2) {
        if (v1.tamanio() != v2.tamanio()) {
            throw new IllegalArgumentException("Los vectores deben tener el mismo tamaño");
        }

        double suma = 0;
        for (int i = 0; i < v1.tamanio(); i++) {
            double diferencia = v1.obtener(i) - v2.obtener(i);
            suma += diferencia * diferencia;
        }

        return Math.sqrt(suma);
    }

    @Override
    public String getNombre() {
        return "Euclidiana";
    }
}