package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
   Normalización Z-Score (estandarización)
   Convierte los datos para que tengan media 0 y desviación estándar 1
 */

public class NormalizacionZscore implements INormalizacion {

    @Override
    public Vector normalizar(Vector datos) {

        // Calcular media
        double suma = 0;
        for (int i = 0; i < datos.tamanio(); i++) {
            suma += datos.obtener(i);
        }
        double media = suma / datos.tamanio();

        // Calcular desviación estándar
        double sumaCuadrados = 0;
        for (int i = 0; i < datos.tamanio(); i++) {
            double diferencia = datos.obtener(i) - media;
            sumaCuadrados += diferencia * diferencia;
        }
        double desviacion = Math.sqrt(sumaCuadrados / datos.tamanio());

        // Normalizar
        Vector normalizado = new Vector(datos.tamanio());

        // Evitar división por cero
        if (desviacion == 0) {
            for (int i = 0; i < datos.tamanio(); i++) {
                normalizado.agregar(0.0);
            }
        } else {
            for (int i = 0; i < datos.tamanio(); i++) {
                double valorNormalizado = (datos.obtener(i) - media) / desviacion;
                normalizado.agregar(valorNormalizado);
            }
        }

        return normalizado;
    }


    @Override
    public String getNombre() {
        return "Z-Score";
    }
}