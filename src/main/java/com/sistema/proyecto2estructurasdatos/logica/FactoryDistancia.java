package com.sistema.proyecto2estructurasdatos.logica;

/**
 * Factory Method para crear instancias de estrategias de distancia
 * Patrón de diseño: Factory Method + Singleton
 *
 * SINGLETON: Solo existe una instancia de la factory en el sistema
 */
public class FactoryDistancia {

    // ===== PATRÓN SINGLETON =====
    private static final FactoryDistancia INSTANCIA = new FactoryDistancia();

    /**
     * Constructor privado para evitar instanciación externa
     */
    private FactoryDistancia() {
        // Constructor privado - parte del patrón Singleton
    }

    /**
     * Obtiene la única instancia de FactoryDistancia
     */
    public static FactoryDistancia obtenerInstancia() {
        return INSTANCIA;
    }
    // ===== FIN SINGLETON =====


    public enum TipoDistancia {
        EUCLIDIANA,
        MANHATTAN,
        COSENO,
        HAMMING
    }

    /**
     * Crea una instancia de distancia según el tipo especificado
     */
    public IDistancia crear(TipoDistancia tipo) {
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
     */
    public IDistancia crear(String nombre) {
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