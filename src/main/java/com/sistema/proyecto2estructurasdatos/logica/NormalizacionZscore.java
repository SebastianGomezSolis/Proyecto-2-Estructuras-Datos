package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector; // Usamos nuestro Vector

/**
 * Normalización Z-Score (estandarización):
 * Deja los datos “centrados” en 0 y con “escala” 1.
 * - Después de esto, la media queda 0.
 * - La dispersión (desviación estándar) queda 1.
 */
public class NormalizacionZscore implements INormalizacion {

    @Override
    public Vector normalizar(Vector datos) {

        // Sacar la media (promedio) de todos los valores
        double suma = 0;
        for (int i = 0; i < datos.tamanio(); i++) {
            suma += datos.obtener(i); // vamos sumando
        }
        double media = suma / datos.tamanio(); // promedio simple

        //  Sacar la desviación estándar (qué tanto se alejan del promedio)
        double sumaCuadrados = 0;
        for (int i = 0; i < datos.tamanio(); i++) {
            double diferencia = datos.obtener(i) - media; // distancia al promedio
            sumaCuadrados += diferencia * diferencia;     // cuadrado y acumular
        }
        double desviacion = Math.sqrt(sumaCuadrados / datos.tamanio()); // raíz del promedio de cuadrados

        // Construimos el vector de salida
        Vector normalizado = new Vector(datos.tamanio());

        // Si todos los valores son iguales, la desviación es 0
        // Para evitar dividir entre 0, dejamos en 0.0
        if (desviacion == 0) {
            for (int i = 0; i < datos.tamanio(); i++) {
                normalizado.agregar(0.0);
            }
        } else {
            // Fórmula Z-Score: (valor - media) / desviación
            for (int i = 0; i < datos.tamanio(); i++) {
                double valorNormalizado = (datos.obtener(i) - media) / desviacion;
                normalizado.agregar(valorNormalizado); // guardamos el resultado
            }
        }

        // retornamos los datos ya estandarizados
        return normalizado;
    }

    @Override
    public String getNombre() {
        return "Z-Score";
    }
}
