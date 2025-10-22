package com.sistema.proyecto2estructurasdatos.algoritmos;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Distancia Manhattan (L1 o de bloque)
 * Suma las diferencias absolutas de cada dimensión
 */

public class DistanciaManhattan implements IDistancia {

    @Override
    public double calcular(Vector v1, Vector v2) {
        if (v1.tamanio() != v2.tamanio()) {
            throw new IllegalArgumentException("Los vectores deben tener el mismo tamaño");
        }

        double suma = 0;
        for (int i = 0; i < v1.tamanio(); i++) {
            suma += Math.abs(v1.obtener(i) - v2.obtener(i));
        }

        return suma;
    }

    @Override
    public String getNombre() {
        return "Manhattan";
    }
}