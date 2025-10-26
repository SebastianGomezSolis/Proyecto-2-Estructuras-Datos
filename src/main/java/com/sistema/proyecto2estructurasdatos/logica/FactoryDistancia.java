package com.sistema.proyecto2estructurasdatos.logica;

/**
 * Factory Method para crear instancias de estrategias de distancia
 */

public class FactoryDistancia {

    public enum TipoDistancia {
        EUCLIDIANA,
        MANHATTAN,
        COSENO,
        HAMMING
    }

    /**
     * Crea una instancia de distancia según el tipo especificado
     * Tipo de distancia
     * return Instancia de IDistancia
     */

    public static IDistancia crear(TipoDistancia tipo) {
        switch (tipo) {
            case EUCLIDIANA:
                return new DistanciaEuclidiana();
            case MANHATTAN:
                return new DistanciaManhattan();
            case COSENO:
                return new DistanciaCoseno();
            case HAMMING:
                return new DistanciaHamming();
            default:
                throw new IllegalArgumentException("Tipo de distancia no válido");
        }
    }

    /**
     * Crea una instancia de distancia según el nombre
     * Nombre de la distancia
     * return Instancia de IDistancia
     */
    public static IDistancia crear(String nombre) {
        switch (nombre.toUpperCase()) {
            case "EUCLIDIANA":
                return new DistanciaEuclidiana();
            case "MANHATTAN":
                return new DistanciaManhattan();
            case "COSENO":
                return new DistanciaCoseno();
            case "HAMMING":
                return new DistanciaHamming();
            default:
                throw new IllegalArgumentException("Tipo de distancia no válido: " + nombre);
        }
    }
}