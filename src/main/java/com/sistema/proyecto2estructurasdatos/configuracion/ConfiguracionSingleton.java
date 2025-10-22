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

    // Obtener la instancia única de la configuración
    // Thread-safe con sincronización
    public static synchronized ConfiguracionSingleton getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracionSingleton();
        }
        return instancia;
    }

    // Resetear la configuración a valores por defecto
    public void resetear() {
        this.tipoNormalizacion = "Min-Max";
        this.tipoDistancia = "Euclidiana";
        this.pesos = null;
        this.columnasIgnoradas = null;
        this.numeroMaximoIteraciones = 1000;
        this.umbralDistancia = 0.001;
        this.modoDebug = false;
    }

    // Getters y Setters
    public String getTipoNormalizacion() { return tipoNormalizacion; }

    public void setTipoNormalizacion(String tipoNormalizacion) {
        if (tipoNormalizacion == null || tipoNormalizacion.trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de normalización no puede ser null o vacío");
        }
        this.tipoNormalizacion = tipoNormalizacion;
    }

    public String getTipoDistancia() { return tipoDistancia; }

    public void setTipoDistancia(String tipoDistancia) {
        if (tipoDistancia == null || tipoDistancia.trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de distancia no puede ser null o vacío");
        }
        this.tipoDistancia = tipoDistancia;
    }

    public double[] getPesos() { return pesos; }

    public void setPesos(double[] pesos) {
        if (pesos != null) {
            // Validar que todos los pesos sean no negativos
            for (int i = 0; i < pesos.length; i++) {
                if (pesos[i] < 0) {
                    throw new IllegalArgumentException("Los pesos no pueden ser negativos");
                }
            }
            // Copiar array para evitar modificaciones externas
            this.pesos = new double[pesos.length];
            System.arraycopy(pesos, 0, this.pesos, 0, pesos.length);
        } else {
            this.pesos = null;
        }
    }

    public boolean[] getColumnasIgnoradas() { return columnasIgnoradas; }

    public void setColumnasIgnoradas(boolean[] columnasIgnoradas) {
        if (columnasIgnoradas != null) {
            // Copiar array para evitar modificaciones externas
            this.columnasIgnoradas = new boolean[columnasIgnoradas.length];
            System.arraycopy(columnasIgnoradas, 0, this.columnasIgnoradas, 0, columnasIgnoradas.length);
        } else {
            this.columnasIgnoradas = null;
        }
    }

    public int getNumeroMaximoIteraciones() { return numeroMaximoIteraciones; }

    public void setNumeroMaximoIteraciones(int numeroMaximoIteraciones) {
        if (numeroMaximoIteraciones <= 0) {
            throw new IllegalArgumentException("El número de iteraciones debe ser mayor a 0");
        }
        this.numeroMaximoIteraciones = numeroMaximoIteraciones;
    }

    public double getUmbralDistancia() { return umbralDistancia; }

    public void setUmbralDistancia(double umbralDistancia) {
        if (umbralDistancia < 0) {
            throw new IllegalArgumentException("El umbral de distancia no puede ser negativo");
        }
        this.umbralDistancia = umbralDistancia;
    }

    public boolean isModoDebug() { return modoDebug; }

    public void setModoDebug(boolean modoDebug) { this.modoDebug = modoDebug; }

}