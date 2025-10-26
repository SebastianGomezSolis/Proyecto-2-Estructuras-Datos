package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 *Normalización(Transformacion) Logarítmica
 *Se usa cuando los datos son muy sesgados o tienen valores extremos
 */

public class NormalizacionLogaritmica implements INormalizacion {

    @Override
    public Vector normalizar(Vector datos) {
        Vector normalizado = new Vector(datos.tamanio());

        // Aplicar transformación logarítmica - O(n)
        for (int i = 0; i < datos.tamanio(); i++) {
            double valor = datos.obtener(i);
            // log(x + 1) para evitar log(0)
            double valorNormalizado = Math.log(Math.abs(valor) + 1);
            // Mantener el signo original
            if (valor < 0) {
                valorNormalizado = -valorNormalizado;
            }
            normalizado.agregar(valorNormalizado);
        }

        return normalizado;
    }

    @Override
    public String getNombre() {
        return "Logarítmica";
    }
}