package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * - Aplica una transformación logarítmica a cada valor de un vector.
 * - Sirve para "aplastar" valores muy grandes y reducir el efecto de outliers
 *   (datos extremos), haciendo la distribución menos sesgada.
 */

public class NormalizacionLogaritmica implements INormalizacion {

    // Implementación del método que transforma todos los datos del vector.
    @Override
    public Vector normalizar(Vector datos) {
        // Creamos un nuevo vector para guardar el resultado.
        // Usamos el mismo tamaño (o capacidad) que el de entrada.
        Vector normalizado = new Vector(datos.tamanio());

        // Recorremos todos los elementos una sola vez
        for (int i = 0; i < datos.tamanio(); i++) {
            // Tomamos el valor original en la posición i.
            double valor = datos.obtener(i);

            // Transformación logarítmica:
            // - Usamos Math.log (logaritmo natural, base e).
            // - Hacemos log(|x| + 1) para:
            //     * Evitar log(0) cuando x = 0.
            //     * Poder usar log con números negativos (tomando su valor absoluto).
            double valorNormalizado = Math.log(Math.abs(valor) + 1);

            // Conservamos el signo original:
            // si el valor era negativo, hacemos negativo también el resultado.
            if (valor < 0) {
                valorNormalizado = -valorNormalizado;
            }

            // Guardamos el valor transformado al final del vector de salida.
            normalizado.agregar(valorNormalizado);
        }

        // Devolvemos el nuevo vector con todos los valores ya transformados.
        return normalizado;
    }

    @Override
    public String getNombre() {
        return "Logarítmica";
    }
}
