package com.sistema.proyecto2estructurasdatos.Formato; // Carpeta lógica donde está esta clase

// Importamos nuestras estructuras y el tipo de dato que guarda cada fila
import com.sistema.proyecto2estructurasdatos.modelo.Lista; // Nuestra lista simple
import com.sistema.proyecto2estructurasdatos.modelo.Dato;  // Cada fila del CSV como objeto Dato

// Esta clase guarda el “paquete” con lo que salió al leer un CSV
public class ResultadoCSV {
    public Lista<Dato> datos;              // Todas las filas leídas del archivo
    public Lista<String> nombresColumnas;  // Los nombres de cada columna
    public int numFilas;                   // Cuántas filas hay
    public int numColumnas;                // Cuántas columnas hay

    // Al crear el resultado, dejamos listo y vacío
    public ResultadoCSV() {
        this.datos = new Lista<>();            // Empezamos con una lista de filas vacía
        this.nombresColumnas = new Lista<>();  // Empezamos con una lista de nombres vacía
        this.numFilas = 0;                     // Aún no contamos filas
        this.numColumnas = 0;                  // Aún no contamos columnas
    }
}
