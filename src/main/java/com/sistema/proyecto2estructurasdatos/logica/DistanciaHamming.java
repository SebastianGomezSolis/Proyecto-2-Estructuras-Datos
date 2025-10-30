package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Clase: DistanciaHamming
 *
 * Qué mide?
 * - Cuenta cuántas posiciones son distintas entre dos vectores.
 *   O sea: recorre ambos a la vez y suma 1 cada vez que en la misma
 *   posición el valor de v1 y el de v2 NO son iguales (dentro de una tolerancia).
 *
 * Cuándo se usa?
 * - Muy útil con datos binarios (0/1) o categóricos codificados (one-hot),
 *   donde solo importa “igual” o “distinto”, no “qué tanto” difieren.
 */

public class DistanciaHamming implements IDistancia {

    // Implementamos el método que calcula la “distancia” de Hamming.
    @Override
    public double calcular(Vector v1, Vector v2) {
        // Validación básica: para comparar elemento a elemento,
        // los dos vectores deben tener la misma cantidad de posiciones.
        if (v1.tamanio() != v2.tamanio()) {
            // Si no, avisamos con un error claro.
            throw new IllegalArgumentException("Los vectores deben tener el mismo tamaño");
        }

        // Aquí contaremos cuántas posiciones difieren.
        int diferencias = 0;

        // Como trabajamos con doubles, a veces ocurren mini errores de coma flotante.
        // Esta "tolerancia" define a partir de qué tan diferente consideramos
        // que dos números NO son iguales (si la diferencia absoluta supera esto).
        double tolerancia = 0.0001;

        // Recorremos todas las posiciones de los vectores.
        for (int i = 0; i < v1.tamanio(); i++) {
            // Diferencia absoluta entre los valores de la misma posición.
            // Si es mayor a la tolerancia, los consideramos “distintos”.
            if (Math.abs(v1.obtener(i) - v2.obtener(i)) > tolerancia) {
                diferencias++; // sumamos 1 porque en esta posición no coinciden
            }
        }

        // La distancia de Hamming es “cuántas posiciones difieren”.
        // Por eso devolvemos el contador.
        return diferencias;
    }

    @Override
    public String getNombre() {
        return "Hamming";
    }
}
