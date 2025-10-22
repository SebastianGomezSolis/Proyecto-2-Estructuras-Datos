package com.sistema.proyecto2estructurasdatos.algoritmos;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Interfaz para el patrón Strategy de cálculo de distancias
 * Permite cambiar el algoritmo de distancia en tiempo de ejecución
 */

public interface IDistancia {

     //Calcula la distancia entre dos vectores
    double calcular(Vector v1, Vector v2);

    //Obtiene el nombre de la estrategia
    String getNombre();
}