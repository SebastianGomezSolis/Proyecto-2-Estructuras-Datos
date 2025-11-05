package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.Lista;
import com.sistema.proyecto2estructurasdatos.modelo.Dato;
import com.sistema.proyecto2estructurasdatos.modelo.HashMapa;

public class ResultadoCSV {
    public Lista<Dato> datos;
    public Lista<String> nombresColumnas;
    public int numFilas;
    public int numColumnas;
    public boolean[] columnasNumericas;
    public boolean[] columnasCualitativas; // NUEVO
    public HashMapa<Integer, HashMapa<String, Integer>> mapeosOneHot; // NUEVO

    public ResultadoCSV() {
        this.datos = new Lista<>();
        this.nombresColumnas = new Lista<>();
        this.numFilas = 0;
        this.numColumnas = 0;
        this.columnasNumericas = null;
        this.columnasCualitativas = null;
        this.mapeosOneHot = new HashMapa<>();
    }
}
