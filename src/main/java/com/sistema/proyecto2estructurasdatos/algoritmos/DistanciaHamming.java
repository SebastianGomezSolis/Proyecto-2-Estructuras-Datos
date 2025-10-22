package com.sistema.proyecto2estructurasdatos.algoritmos;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Distancia de Hamming
 * Cuenta el número de posiciones en donde los vectores difieren
 */

public class DistanciaHamming implements IDistancia {

    @Override
    public double calcular(Vector v1, Vector v2) {
        if (v1.tamanio() != v2.tamanio()) {
            throw new IllegalArgumentException("Los vectores deben tener el mismo tamaño");
        }

        int diferencias = 0;
        double tolerancia = 0.0001; // Tolerancia para comparación de doubles

        for (int i = 0; i < v1.tamanio(); i++) {
            if (Math.abs(v1.obtener(i) - v2.obtener(i)) > tolerancia) {
                diferencias++;
            }
        }

        return diferencias;
    }

    @Override
    public String getNombre() {
        return "Hamming";
    }
}