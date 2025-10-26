package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.*;

// Algoritmo de Clustering Jerárquico Aglomerativo
// Implementa el método bottom-up para construcción del dendrograma
// Proceso:
// 1. Normalizar datos según estrategia seleccionada
// 2. Aplicar pesos a las variables
// 3. Calcular matriz de distancias
// 4. Fusionar clusters más cercanos iterativamente
// 5. Construir dendrograma con estructura de árbol binario
public class AlgoritmoClustering {
    private INormalizacion normalizacion;
    private IDistancia distancia;
    private double[] pesos;
    private boolean[] columnasIgnoradas;

    // Constructor del algoritmo
    public AlgoritmoClustering(INormalizacion normalizacion, IDistancia distancia, double[] pesos, boolean[] columnasIgnoradas) {
        if (normalizacion == null) {
            throw new IllegalArgumentException("La estrategia de normalización no puede ser null");
        }
        if (distancia == null) {
            throw new IllegalArgumentException("La estrategia de distancia no puede ser null");
        }

        this.normalizacion = normalizacion;
        this.distancia = distancia;
        this.pesos = pesos;
        this.columnasIgnoradas = columnasIgnoradas;
    }

    // Ejecuta el algoritmo completo de clustering jerárquico
    public ArbolBinario ejecutar(Lista<Dato> datos) {
        if (datos == null || datos.tamanio() == 0) {
            throw new IllegalArgumentException("La lista de datos está vacía");
        }

        // Paso 1: Normalizar y aplicar pesos - O(n·m)
        procesarDatos(datos);

        // Paso 2: Crear matriz de distancias - O(n²·m)
        Matriz matrizDistancias = crearMatrizDistancias(datos);

        // Paso 3: Construir dendrograma - O(n³)
        ArbolBinario dendrograma = construirDendrograma(datos, matrizDistancias);

        return dendrograma;
    }

    // Normaliza los datos y aplica pesos a cada dimensión
    private void procesarDatos(Lista<Dato> datos) {
        // Obtener dimensión de los vectores
        int dimension = datos.obtener(0).getVectorOriginal().tamanio();

        // Normalizar cada dimensión por separado
        for (int dim = 0; dim < dimension; dim++) {

            // Si la columna está ignorada, continuar con la siguiente
            if (columnasIgnoradas != null && dim < columnasIgnoradas.length &&
                    columnasIgnoradas[dim]) {
                continue;
            }

            // Extraer valores de esta dimensión - O(n)
            Vector valoresDimension = new Vector(datos.tamanio());
            for (int i = 0; i < datos.tamanio(); i++) {
                valoresDimension.agregar(datos.obtener(i).getVectorOriginal().obtener(dim));
            }

            // Normalizar valores - O(n)
            Vector valoresNormalizados = normalizacion.normalizar(valoresDimension);

            // Obtener peso para esta dimensión
            double peso = (pesos != null && dim < pesos.length) ? pesos[dim] : 1.0;

            // Actualizar datos con valores normalizados y ponderados - O(n)
            for (int i = 0; i < datos.tamanio(); i++) {
                Dato dato = datos.obtener(i);

                // Inicializar vector procesado si no existe
                if (dato.getVectorProcesado() == null) {
                    dato.setVectorProcesado(new Vector(dimension));
                }

                double valorPonderado = valoresNormalizados.obtener(i) * peso;

                // Agregar o actualizar valor
                if (dato.getVectorProcesado().tamanio() > dim) {
                    dato.getVectorProcesado().establecer(dim, valorPonderado);
                } else {
                    dato.getVectorProcesado().agregar(valorPonderado);
                }
            }
        }

        // Para columnas ignoradas, establecer valores en 0
        if (columnasIgnoradas != null) {
            for (int i = 0; i < datos.tamanio(); i++) {
                Dato dato = datos.obtener(i);
                for (int dim = 0; dim < dimension; dim++) {
                    if (columnasIgnoradas[dim]) {
                        if (dato.getVectorProcesado().tamanio() <= dim) {
                            dato.getVectorProcesado().agregar(0.0);
                        } else {
                            dato.getVectorProcesado().establecer(dim, 0.0);
                        }
                    }
                }
            }
        }
    }

    // Crea la matriz de distancias entre todos los pares de datos
    private Matriz crearMatrizDistancias(Lista<Dato> datos) {
        int n = datos.tamanio();
        Matriz matriz = new Matriz(n, n);

        // Calcular distancias para todos los pares - O(n²)
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    // Distancia a sí mismo es 0
                    matriz.insertar(i, j, 0.0);
                } else {
                    // Calcular distancia entre vectores procesados - O(m)
                    Vector v1 = datos.obtener(i).getVectorProcesado();
                    Vector v2 = datos.obtener(j).getVectorProcesado();
                    double dist = distancia.calcular(v1, v2);
                    matriz.insertar(i, j, dist);
                }
            }
        }

        return matriz;
    }

    // Construye el dendrograma usando clustering jerárquico aglomerativo
    private ArbolBinario construirDendrograma(Lista<Dato> datos, Matriz matrizDistancias) {
        int n = datos.tamanio();

        // Crear lista de clusters activos
        // Inicialmente, cada dato es su propio cluster - O(n)
        Lista<Cluster> clustersActivos = new Lista<>();
        for (int i = 0; i < n; i++) {
            Dato dato = datos.obtener(i);
            NodoArbol nodo = new NodoArbol(dato.getEtiqueta(), dato.getVectorProcesado(), dato.getIndice());
            clustersActivos.agregar(new Cluster(nodo, i));
        }

        // Copiar matriz de distancias para modificarla - O(n²)
        Matriz matrizActual = copiarMatriz(matrizDistancias);

        // Iterar hasta que quede un solo cluster - O(n) iteraciones
        while (clustersActivos.tamanio() > 1) {

            // Encontrar el par de clusters más cercano - O(n²)
            Matriz.ParIndices parMinimo = encontrarMinimo(matrizActual, clustersActivos);

            if (parMinimo.i == -1) {
                // No hay más pares válidos para unir
                break;
            }

            // Obtener los dos clusters a fusionar
            Cluster cluster1 = clustersActivos.obtener(parMinimo.i);
            Cluster cluster2 = clustersActivos.obtener(parMinimo.j);

            // Crear nuevo nodo que fusiona ambos clusters
            NodoArbol nuevoNodo = new NodoArbol(cluster1.nodo, cluster2.nodo, parMinimo.valor);

            // Crear nuevo cluster
            Cluster nuevoCluster = new Cluster(nuevoNodo, -1);

            // Actualizar matriz de distancias con el nuevo cluster - O(n)
            actualizarMatrizDistancias(matrizActual, clustersActivos,
                    parMinimo.i, parMinimo.j, nuevoCluster);

            // Eliminar clusters fusionados de la lista activa
            // Eliminar el de mayor índice primero para no alterar índices
            int mayor = Math.max(parMinimo.i, parMinimo.j);
            int menor = Math.min(parMinimo.i, parMinimo.j);

            clustersActivos.eliminar(mayor);
            clustersActivos.eliminar(menor);

            // Agregar el nuevo cluster
            clustersActivos.agregar(nuevoCluster);
        }

        // El último cluster en la lista es la raíz del dendrograma
        return new ArbolBinario(clustersActivos.obtener(0).nodo);
    }

    // Encuentra el par de clusters con menor distancia
    private Matriz.ParIndices encontrarMinimo(Matriz matriz, Lista<Cluster> clustersActivos) {
        double minimo = Double.MAX_VALUE;
        int minI = -1, minJ = -1;

        // Buscar el par con distancia mínima - O(n²)
        for (int i = 0; i < clustersActivos.tamanio(); i++) {
            for (int j = i + 1; j < clustersActivos.tamanio(); j++) {

                int indiceI = clustersActivos.obtener(i).indiceMatriz;
                int indiceJ = clustersActivos.obtener(j).indiceMatriz;

                double dist;

                // Si algún cluster es nuevo (índice -1), calcular distancia entre centroides
                if (indiceI == -1 || indiceJ == -1) {
                    dist = calcularDistanciaEntreClusters(
                            clustersActivos.obtener(i).nodo,
                            clustersActivos.obtener(j).nodo
                    );
                } else if (indiceI < matriz.getFilas() && indiceJ < matriz.getColumnas()) {
                    // Obtener distancia de la matriz
                    dist = matriz.obtener(indiceI, indiceJ);
                } else {
                    continue;
                }

                // Actualizar mínimo si encontramos una distancia menor
                if (dist > 0 && dist < minimo) {
                    minimo = dist;
                    minI = i;
                    minJ = j;
                }
            }
        }

        return new Matriz.ParIndices(minI, minJ, minimo);
    }

    // Calcula distancia entre dos clusters usando sus centroidesDistancia entre centroides
    private double calcularDistanciaEntreClusters(NodoArbol nodo1,
                                                  NodoArbol nodo2) {
        Vector centroide1 = calcularCentroide(nodo1);
        Vector centroide2 = calcularCentroide(nodo2);
        return distancia.calcular(centroide1, centroide2);
    }

    // Calcula el promedio de las hojas del centroide
    private Vector calcularCentroide(NodoArbol nodo) {
        // Si es hoja, su centroide es su propio vector
        if (nodo.esHoja()) {
            return nodo.getDatos();
        }

        // Recolectar todas las hojas del subárbol - O(n)
        Lista<Vector> hojas = new Lista<>();
        recolectarHojas(nodo, hojas);

        if (hojas.tamanio() == 0) {
            return new Vector();
        }

        // Calcular promedio de todas las dimensiones - O(n·m)
        int dimension = hojas.obtener(0).tamanio();
        Vector centroide = new Vector(dimension);

        for (int d = 0; d < dimension; d++) {
            double suma = 0.0;
            for (int i = 0; i < hojas.tamanio(); i++) {
                suma += hojas.obtener(i).obtener(d);
            }
            centroide.agregar(suma / hojas.tamanio());
        }

        return centroide;
    }

    // Recolecta todas las hojas de un subárbol
    private void recolectarHojas(NodoArbol nodo, Lista<Vector> hojas) {
        if (nodo == null) {
            return;
        }

        if (nodo.esHoja()) {
            hojas.agregar(nodo.getDatos());
        } else {
            recolectarHojas(nodo.getIzquierdo(), hojas);
            recolectarHojas(nodo.getDerecho(), hojas);
        }
    }

    // Actualiza la matriz de distancias después de fusionar dos clusters
    // Implementación simplificada: marca filas/columnas como inválidas
    private void actualizarMatrizDistancias(Matriz matriz, Lista<Cluster> clusters, int i, int j, Cluster nuevoCluster) {
        // Obtener los índices en la matriz de los clusters a fusionar
        int indiceI = clusters.obtener(i).indiceMatriz;
        int indiceJ = clusters.obtener(j).indiceMatriz;

        // Marcar las filas y columnas de los clusters fusionados como inválidas
        // Esto se hace estableciendo distancias negativas (-1)
        if (indiceI >= 0 && indiceI < matriz.getFilas()) {
            for (int k = 0; k < matriz.getColumnas(); k++) {
                matriz.insertar(indiceI, k, -1.0);
                matriz.insertar(k, indiceI, -1.0);
            }
        }

        if (indiceJ >= 0 && indiceJ < matriz.getFilas()) {
            for (int k = 0; k < matriz.getColumnas(); k++) {
                matriz.insertar(indiceJ, k, -1.0);
                matriz.insertar(k, indiceJ, -1.0);
            }
        }
    }

    // Metodo para copiar una matriz completa
    private Matriz copiarMatriz(Matriz original) {
        Matriz copia = new Matriz(original.getFilas(), original.getColumnas());

        for (int i = 0; i < original.getFilas(); i++) {
            for (int j = 0; j < original.getColumnas(); j++) {
                copia.insertar(i, j, original.obtener(i, j));
            }
        }

        return copia;
    }


    // Clase interna para representar un cluster durante el proceso
    // Mantiene referencia al nodo del árbol y su índice en la matriz
    private static class Cluster {
        NodoArbol nodo;
        int indiceMatriz;

        Cluster(NodoArbol nodo, int indiceMatriz) {
            this.nodo = nodo;
            this.indiceMatriz = indiceMatriz;
        }
    }
}
