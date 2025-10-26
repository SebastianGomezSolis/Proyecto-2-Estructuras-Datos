package com.sistema.proyecto2estructurasdatos.configuracion;


// Clase Singleton para gestionar la configuración global del sistema

public class ConfiguracionSingleton {

    // Instancia única (Singleton)
    private static ConfiguracionSingleton instancia = null;

    // Configuraciones del sistema
    private String tipoNormalizacion;
    private String tipoDistancia;
    private double[] pesos;
    private boolean[] columnasIgnoradas;
    private int numeroMaximoIteraciones;
    private double umbralDistancia;
    private boolean modoDebug;


    // Constructor privado para evitar instanciación externa
    // Implementa el patrón Singleton
    private ConfiguracionSingleton() {
        // Valores por defecto
        this.tipoNormalizacion = "Min-Max";
        this.tipoDistancia = "Euclidiana";
        this.pesos = null;
        this.columnasIgnoradas = null;
        this.numeroMaximoIteraciones = 1000;
        this.umbralDistancia = 0.001;
        this.modoDebug = false;
    }
}