package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

public interface IDistancia {

    // Método principal del contrato:
    // Calcula la distancia entre dos vectores y devuelve un número (double).
    double calcular(Vector v1, Vector v2);

    String getNombre();
}
