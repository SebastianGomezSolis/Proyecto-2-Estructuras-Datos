package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.Lista;
import com.sistema.proyecto2estructurasdatos.modelo.Dato;

/**
 *   Guarda el resultado al leer un CSV:
 * - Los datos leídos
 * - Los nombres de columnas
 * - Cuántas filas y columnas hay
 * - Cuáles columnas son numéricas (para evitar normalizar one-hot)
 */
public class ResultadoCSV {
    public Lista<Dato> datos;              // Todas las filas leídas del archivo
    public Lista<String> nombresColumnas;  // Nombres de cada columna
    public int numFilas;                   // Cantidad de filas
    public int numColumnas;                // Cantidad de columnas
    public boolean[] columnasNumericas;    // indica qué columnas son numéricas

    public ResultadoCSV() {
        this.datos = new Lista<>();
        this.nombresColumnas = new Lista<>();
        this.numFilas = 0;
        this.numColumnas = 0;
        this.columnasNumericas = null;
    }
}
