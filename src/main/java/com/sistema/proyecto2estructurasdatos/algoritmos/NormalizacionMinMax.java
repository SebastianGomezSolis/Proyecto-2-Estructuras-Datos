package com.sistema.proyecto2estructurasdatos.algoritmos;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Normalización Min-Max (escalado lineal)
 * Transforma los valores a un rango entre 0 y 1
 */
public class NormalizacionMinMax implements INormalizacion {

    @Override
    public Vector normalizar(Vector datos) {

        // Encontrar mínimo y máximo
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (int i = 0; i < datos.tamanio(); i++) {
            double valor = datos.obtener(i);
            if (valor < min) min = valor;
            if (valor > max) max = valor;
        }

        // Normalizar
        Vector normalizado = new Vector(datos.tamanio());
        double rango = max - min;

        // Evitar división por cero
        if (rango == 0) {
            for (int i = 0; i < datos.tamanio(); i++) {
                normalizado.agregar(0.0);
            }
        } else {
            for (int i = 0; i < datos.tamanio(); i++) {
                double valorNormalizado = (datos.obtener(i) - min) / rango;
                normalizado.agregar(valorNormalizado);
            }
        }

        return normalizado;
    }

    @Override
    public String getNombre() {
        return "Min-Max";
    }

}