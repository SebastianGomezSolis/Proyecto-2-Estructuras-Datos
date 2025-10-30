package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Clase: DistanciaEuclidiana
 *
 * - Calcula qué tan lejos están dos vectores si trazáramos una línea recta entre ellos.
 *
 * Ejemplo mental:
 * - Si solo tuviéramos 2 números por vector (x, y), sería la fórmula del triángulo:
 *
 * Para qué se usa?
 * - En cosas como clustering (agrupar datos), para comparar si dos filas/elementos
 *   se parecen o no: cuanto más pequeña la distancia, más “parecidos”.
 */

public class DistanciaEuclidiana implements IDistancia {

    // Sobrescribimos (implementamos) el método que calcula la distancia entre dos vectores.
    @Override
    public double calcular(Vector v1, Vector v2) {
        // Primer control: si los vectores no tienen la misma cantidad de elementos,
        // no se puede comparar posición a posición. Por eso lanzamos un error claro.
        if (v1.tamanio() != v2.tamanio()) {
            // Mensaje fácil de entender si alguien usa mal este método.
            throw new IllegalArgumentException("Los vectores deben tener el mismo tamaño");
        }

        // Aquí vamos a ir sumando los cuadrados de las diferencias de cada posición.
        // Empezamos en 0 por obvias razones (no hay nada sumado aún).
        double suma = 0;

        // Recorremos todas las posiciones del vector.
        // Como ya verificamos que miden lo mismo, usamos v1.tamanio() para el límite.
        for (int i = 0; i < v1.tamanio(); i++) {
            // Tomamos el valor de la posición i en cada vector y calculamos su diferencia.
            // "diferencia" puede ser positiva o negativa; no importa, luego la elevamos al cuadrado.
            double diferencia = v1.obtener(i) - v2.obtener(i);

            // Elevamos la diferencia al cuadrado y la sumamos al acumulado.
            // Por qué al cuadrado?
            // - Para que los negativos no resten.
            // - Para que diferencias más grandes “pesen” más en el resultado.
            suma += diferencia * diferencia;
        }

        // La distancia euclidiana es la raíz cuadrada de esa suma de cuadrados.
        // Esto nos devuelve un número >= 0 (nunca negativo).
        return Math.sqrt(suma);
    }

    // Método útil para mostrar el nombre de la distancia en combos/labels/logs.
    @Override
    public String getNombre() {
        return "Euclidiana";
    }
}
