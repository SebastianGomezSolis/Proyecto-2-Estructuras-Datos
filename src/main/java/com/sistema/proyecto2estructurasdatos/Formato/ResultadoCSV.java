package com.sistema.proyecto2estructurasdatos.Formato;

import com.sistema.proyecto2estructurasdatos.modelo.Lista;
import com.sistema.proyecto2estructurasdatos.modelo.Dato;
import com.sistema.proyecto2estructurasdatos.modelo.HashMapa;

// Esta clase representa el "resultado" de leer un archivo CSV.
//
// La idea es tener en un solo objeto toda la información necesaria para trabajar
// luego en el clustering y en la interfaz:
//  - Las filas del archivo, ya convertidas en objetos {@link Dato}.
//  - Los nombres de las columnas tal como venían en el CSV.
//  - Cantidad de filas y columnas.
//  - Información sobre qué columnas son numéricas o cualitativas.
//  - Los mapeos que se usan para la codificación one-hot de columnas categóricas.
//
 // Funciona como un contenedor de datos (DTO), sin lógica compleja:
//  Solo guarda y transporta información entre las distintas capas del programa.
public class ResultadoCSV {
    public Lista<Dato> datos;
    public Lista<String> nombresColumnas;
    public int numFilas;
    public int numColumnas;
    public boolean[] columnasNumericas;
    public boolean[] columnasCualitativas; // NUEVO
    public HashMapa<Integer, HashMapa<String, Integer>> mapeosOneHot; // NUEVO

    // Constructor por defecto.
    //
    // Deja el objeto en un estado inicial "vacío" pero consistente:
    //  - Listas creadas pero sin elementos.
    //  - Contadores de filas y columnas en 0.
    //  - Arreglos de columnas en null (se rellenan después al leer el CSV).
    //  - Mapa de one-hot vacío, listo para llenarse con las categorías encontradas.
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
