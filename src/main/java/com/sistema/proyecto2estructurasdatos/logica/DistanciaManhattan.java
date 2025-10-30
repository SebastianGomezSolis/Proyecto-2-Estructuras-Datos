package com.sistema.proyecto2estructurasdatos.logica;


import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Qué mide?
 * - Calcula qué tan “lejos” están dos vectores sumando las diferencias
 *   de cada posición, pero sin usar raíces ni potencias.
 *   No va en línea recta como la distancia Euclidiana, sino que suma
 *   los pasos horizontales y verticales necesarios para ir de un punto a otro.
 */

public class DistanciaManhattan implements IDistancia {

    // Sobrescribimos el método calcular.
    // Recibe dos vectores (v1 y v2) y devuelve un número (la distancia entre ellos).
    @Override
    public double calcular(Vector v1, Vector v2) {
        // Primero verificamos que ambos vectores tengan la misma cantidad de datos.
        // Si uno tiene más valores que el otro, no se pueden comparar correctamente.
        if (v1.tamanio() != v2.tamanio()) {
            // Si no son iguales en tamaño, lanzamos un error (excepción)
            // para avisar que algo está mal con la entrada.
            throw new IllegalArgumentException("Los vectores deben tener el mismo tamaño");
        }

        // Variable donde iremos sumando las diferencias.
        double suma = 0;

        // Recorremos todos los elementos de los vectores uno por uno.
        for (int i = 0; i < v1.tamanio(); i++) {
            // Obtenemos el valor en la posición i de cada vector.
            // Calculamos la diferencia absoluta entre ellos con Math.abs (siempre positiva).
            // Luego la sumamos al total acumulado.
            suma += Math.abs(v1.obtener(i) - v2.obtener(i));
        }

        // Cuando terminamos de recorrer todo, devolvemos la suma final.
        // Ese número representa la distancia “Manhattan” entre ambos vectores.
        return suma;
    }

    @Override
    public String getNombre() {
        return "Manhattan";
    }
}
