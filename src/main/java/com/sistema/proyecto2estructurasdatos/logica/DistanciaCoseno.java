package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Distancia Coseno
 * Mide el ángulo entre dos vectores, no su magnitud
 */

public class DistanciaCoseno implements IDistancia {

    @Override
    public double calcular(Vector v1, Vector v2) {
        if (v1.tamanio() != v2.tamanio()) {
            throw new IllegalArgumentException("Los vectores deben tener el mismo tamaño");
        }

        // Calcular producto punto
        double productoPunto = v1.productoPunto(v2);

        // Calcular magnitudes
        double magnitudV1 = v1.magnitud();
        double magnitudV2 = v2.magnitud();

        // Evitar división por cero
        if (magnitudV1 == 0 || magnitudV2 == 0) {
            return 1.0; // Máxima distancia
        }

        // Distancia coseno = 1 - similitud coseno
        double similitudCoseno = productoPunto / (magnitudV1 * magnitudV2);
        return 1.0 - similitudCoseno;
    }

    @Override
    public String getNombre() {
        return "Coseno";
    }
}