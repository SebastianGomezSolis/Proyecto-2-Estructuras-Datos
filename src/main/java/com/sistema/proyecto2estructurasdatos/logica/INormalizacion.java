package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Interfaz para el patrón Strategy de normalización
 * Permite cambiar el algoritmo de normalización en tiempo de ejecución
 */
public interface INormalizacion {

     //Normaliza un vector de datos
    Vector normalizar(Vector datos);

    //Obtiene el nombre de la estrategia
    String getNombre();
}
