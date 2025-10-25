package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.Lista;
import com.sistema.proyecto2estructurasdatos.modelo.Dato;

public class ResultadoCSV {
    public Lista<Dato> datos;
    public Lista<String> nombresColumnas;
    public int numFilas;
    public int numColumnas;

    public ResultadoCSV() {
        this.datos = new Lista<>();
        this.nombresColumnas = new Lista<>();
        this.numFilas = 0;
        this.numColumnas = 0;
    }
}