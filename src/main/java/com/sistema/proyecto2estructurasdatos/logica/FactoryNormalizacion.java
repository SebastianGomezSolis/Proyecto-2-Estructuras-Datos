package com.sistema.proyecto2estructurasdatos.logica;


/**
 * Factory Method para crear instancias de estrategias de normalización
 * Patrón de diseño: Factory Method
 */

public class FactoryNormalizacion {

    public enum TipoNormalizacion {
        MIN_MAX,
        Z_SCORE,
        LOGARITMICA
    }

    /**
     * Crea una instancia de normalización según el tipo especificado
     * @param tipo Tipo de normalización
     * @return Instancia de INormalizacion
     */
    public static INormalizacion crear(TipoNormalizacion tipo) {
        switch (tipo) {
            case MIN_MAX:
                return new NormalizacionMinMax();
            case Z_SCORE:
                return new NormalizacionZscore();
            case LOGARITMICA:
                return new NormalizacionLogaritmica();
            default:
                throw new IllegalArgumentException("Tipo de normalización no válido");
        }
    }

    /**
     * Crea una instancia de normalización según el nombre
     * Nombre de la normalización
     * return Instancia de INormalizacion
     */
    public static INormalizacion crear(String nombre) {
        switch (nombre.toUpperCase()) {
            case "MIN-MAX":
                return new NormalizacionMinMax();
            case "Z-SCORE":
                return new NormalizacionZscore();
            case "LOGARÍTMICA":
            case "LOGARITMICA":
                return new NormalizacionLogaritmica();
            default:
                throw new IllegalArgumentException("Tipo de normalización no válido: " + nombre);
        }
    }
}