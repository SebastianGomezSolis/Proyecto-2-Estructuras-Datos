package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector; // Usamos nuestro Vector

/**
 * Normalización Min-Max (escala valores entre 0 y 1).
 * Toma el menor como 0 y el mayor como 1. El resto queda proporcional.
 */
public class NormalizacionMinMax implements INormalizacion {

    @Override
    public Vector normalizar(Vector datos) {

        // Buscamos el valor más pequeño y el más grande
        double min = Double.MAX_VALUE;   // Arranca con un “muy grande” para encontrar el mínimo real
        double max = Double.MIN_VALUE;   // Arranca con un “muy pequeño” para encontrar el máximo real

        for (int i = 0; i < datos.tamanio(); i++) { // Recorremos todos los datos
            double valor = datos.obtener(i);        // Tomamos el número actual
            if (valor < min) min = valor;           // Actualiza mínimo
            if (valor > max) max = valor;           // Actualiza máximo
        }

        // Preparamos el vector de salida con el mismo tamaño
        Vector normalizado = new Vector(datos.tamanio());
        double rango = max - min; // Diferencia entre mayor y menor

        // Si todos los valores son iguales, el rango es 0
        if (rango == 0) {
            // En ese caso, queda en 0.0
            for (int i = 0; i < datos.tamanio(); i++) {
                normalizado.agregar(0.0);
            }
        } else {
            // Fórmula Min-Max: (valor - mínimo) / rango
            for (int i = 0; i < datos.tamanio(); i++) {
                double valorNormalizado = (datos.obtener(i) - min) / rango; // Escala a [0,1]
                normalizado.agregar(valorNormalizado);                      // Lo guardamos
            }
        }

        return normalizado; // retornamos el resultado
    }

    @Override
    public String getNombre() {
        return "Min-Max";
    }

}
