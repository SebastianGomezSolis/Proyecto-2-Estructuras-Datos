package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.*;

// Implementa el algoritmo de Clustering Jerárquico Aglomerativo.
// Flujo general:
//  1. Toma una lista de {@link Dato} con su vectorOriginal lleno.
//  2. Procesa/normaliza cada columna según:
//      - Estrategias de normalización por columna (normalizaciones[]).
//      - Pesos por columna (pesos[]).
//      - Columnas ignoradas (columnasIgnoradas).
//      - Marcado de columnas realmente numéricas (columnasNumericas).
//     El resultado se guarda en el vectorProcesado de cada Dato.
//  3. Construye una matriz de distancias entre todos los datos usando IDistancia.
//  4. Aplica clustering jerárquico (enlace simple / single-link):
//      - Une iterativamente los dos clusters más cercanos.
//      - Crea nodos internos (NodoArbol) para ir formando el dendrograma.
//  5. Devuelve un {@link ArbolBinario} representando el dendrograma final.
public class AlgoritmoClustering {
    private INormalizacion[] normalizaciones; // CAMBIO: array
    private IDistancia distancia;
    private double[] pesos;
    private boolean[] columnasIgnoradas;
    private boolean[] columnasNumericas;

    public void setColumnasNumericas(boolean[] columnasNumericas) {
        this.columnasNumericas = columnasNumericas;
    }

    // Constructor principal del algoritmo.
    public AlgoritmoClustering(
            INormalizacion[] normalizaciones, // CAMBIO
            IDistancia distancia,
            double[] pesos,
            boolean[] columnasIgnoradas
    ) {
        if (normalizaciones == null)
            throw new IllegalArgumentException("Normalizaciones no puede ser null");
        if (distancia == null)
            throw new IllegalArgumentException("Distancia no puede ser null");

        this.normalizaciones = normalizaciones;
        this.distancia = distancia;
        this.pesos = pesos;
        this.columnasIgnoradas = columnasIgnoradas;
    }

    // Punto de entrada del algoritmo de clustering.
    // Pasos:
    // 1. Verifica que haya datos.
    //  2. Procesa/normaliza los vectores originales → vectorProcesado.
    //  3. Construye la matriz de distancias.
    //  4. Construye y devuelve el dendrograma como {@link ArbolBinario}.
    public ArbolBinario ejecutar(Lista<Dato> datos) {
        if (datos == null || datos.tamanio() == 0)
            throw new IllegalArgumentException("La lista de datos está vacía");

        // Normalizar/pesar columnas y llenar vectorProcesado de cada Dato
        procesarDatos(datos);

        // Matriz de distancias entre todos los datos
        Matriz matriz = crearMatrizDistancias(datos);

        // Construir el dendrograma a partir de la matriz de distancias
        return construirDendrograma(datos, matriz);
    }

    // Procesa los datos originales para generar el vectorProcesado en cada {@link Dato}.
    // Para cada dimensión (columna) del vector original:
    //  - Si no es numérica o está marcada para ignorar → se salta.
    //  - Si la columna es constante → se llena con ceros (no aporta al clustering).
    //  - Si no es constante → se normaliza con la INormalizacion correspondiente
    //    (o MinMax por defecto) y luego se aplica el peso.
    //
     // El resultado se almacena en Dato.vectorProcesado.
    private void procesarDatos(Lista<Dato> datos) {
        // Suponemos que todos los datos tienen la misma dimensión
        int dimension = datos.obtener(0).getVectorOriginal().tamanio();

        // Recorremos cada dimensión (columna)
        for (int dim = 0; dim < dimension; dim++) {
            // 1) Saltar columnas que NO son numéricas (ej. partes one-hot, etc.)
            if (columnasNumericas != null && dim < columnasNumericas.length &&
                    !columnasNumericas[dim]) {
                continue;
            }

            // 2) Saltar columnas que el usuario marcó como "ignoradas"
            if (columnasIgnoradas != null && dim < columnasIgnoradas.length &&
                    columnasIgnoradas[dim]) {
                continue;
            }

            // 3) Extraer todos los valores de esta columna en un solo Vector
            Vector valores = new Vector(datos.tamanio());
            for (int i = 0; i < datos.tamanio(); i++) {
                valores.agregar(datos.obtener(i).getVectorOriginal().obtener(dim));
            }

            // 4) Decidir si la columna es constante
            Vector normalizados;
            boolean esConstante = esColumnaConstante(valores);

            if (esConstante) {
                // Si todos los valores son iguales, normalizamos como todo 0.0
                normalizados = new Vector(datos.tamanio());
                for (int i = 0; i < datos.tamanio(); i++)
                    normalizados.agregar(0.0);
            } else {
                // Seleccionamos la normalización específica para esta columna
                INormalizacion normParaEstaCol =
                        (normalizaciones != null && dim < normalizaciones.length &&
                                normalizaciones[dim] != null)
                                ? normalizaciones[dim]
                                : new NormalizacionMinMax(); // por defecto

                // Aplicamos la normalización
                normalizados = normParaEstaCol.normalizar(valores);
            }

            // 5) Aplicar peso de la columna (si existe)
            double peso = (pesos != null && dim < pesos.length) ? pesos[dim] : 1.0;

            // 6) Guardar el valor final (normalizado * peso) en el vectorProcesado
            for (int i = 0; i < datos.tamanio(); i++) {
                Dato d = datos.obtener(i);

                // Si aún no existe el vectorProcesado, lo creamos con capacidad = dimensión
                if (d.getVectorProcesado() == null)
                    d.setVectorProcesado(new Vector(dimension));

                double valor = normalizados.obtener(i) * peso;

                // Si ya hay un valor en esa posición, lo reemplazamos;
                // si no, lo agregamos al final
                if (d.getVectorProcesado().tamanio() > dim)
                    d.getVectorProcesado().establecer(dim, valor);
                else
                    d.getVectorProcesado().agregar(valor);
            }
        }

        // 7) Para las columnas marcadas como ignoradas, forzamos el valor 0, por si el vectorProcesado ya tenía algo en esa posición.
        if (columnasIgnoradas != null) {
            for (int i = 0; i < datos.tamanio(); i++) {
                Dato d = datos.obtener(i);
                for (int dim = 0; dim < dimension; dim++) {
                    if (columnasIgnoradas[dim]) {
                        if (d.getVectorProcesado().tamanio() <= dim)
                            d.getVectorProcesado().agregar(0.0);
                        else
                            d.getVectorProcesado().establecer(dim, 0.0);
                    }
                }
            }
        }
    }

    // Indica si todos los valores de una columna son prácticamente iguales.
    // Se usa una pequeña tolerancia (1e-10) para evitar problemas de doble precisión.
    private boolean esColumnaConstante(Vector valores) {
        if (valores.tamanio() == 0) return true;
        double base = valores.obtener(0);
        for (int i = 1; i < valores.tamanio(); i++) {
            if (Math.abs(valores.obtener(i) - base) > 1e-10)
                return false;
        }
        return true;
    }

    // Construye una matriz de distancias simétrica NxN a partir de los vectoresProcesados de los datos.
    private Matriz crearMatrizDistancias(Lista<Dato> datos) {
        int n = datos.tamanio();
        Matriz m = new Matriz(n, n);

        for (int i = 0; i < n; i++) {
            // Distancia de un elemento consigo mismo es 0
            m.insertar(i, i, 0.0);
            Vector v1 = datos.obtener(i).getVectorProcesado();

            for (int j = i + 1; j < n; j++) {
                Vector v2 = datos.obtener(j).getVectorProcesado();
                double d = distancia.calcular(v1, v2);
                // Guardamos en ambas posiciones para mantener simetría
                m.insertar(i, j, d);
                m.insertar(j, i, d);
            }
        }
        return m;
    }

    // Construye el dendrograma (árbol de clustering) usando una matriz de distancias.
    // Estrategia:
    //  - Cada Dato comienza como un cluster independiente (hoja del árbol).
    //  - En cada paso se busca el par de clusters activos más cercanos.
    //  - Se crea un nuevo NodoArbol como padre de ambos y se actualizan distancias
    //    usando enlace simple (mínimo entre distancias anteriores).
    //  - Se repite hasta dejar solo un cluster activo, que será la raíz.
    private ArbolBinario construirDendrograma(Lista<Dato> datos, Matriz matriz) {
        int n = datos.tamanio();

        // Array de clusters (cada uno guarda un NodoArbol raíz para ese cluster)
        Cluster[] clusters = new Cluster[n];
        // Marca si el cluster i sigue activo o ya fue fusionado
        boolean[] activo = new boolean[n];

        // Inicialmente, cada dato es un cluster hoja
        for (int i = 0; i < n; i++) {
            Dato d = datos.obtener(i);
            NodoArbol hoja = new NodoArbol(
                    d.getEtiqueta(),
                    d.getVectorProcesado(),
                    d.getIndice()
            );
            clusters[i] = new Cluster(hoja, i);
            activo[i] = true;
        }

        // nearest / distVecino: vecino más cercano de cada cluster activo
        int[] vecino = new int[n];
        double[] distVecino = new double[n];

        // Inicializamos la información de "vecino más cercano" para cada cluster
        inicializarMasCercano(matriz, activo, vecino, distVecino, n);

        // Tendremos exactamente n-1 fusiones para llegar a un solo cluster
        for (int paso = 0; paso < n - 1; paso++) {
            int a = -1, b = -1;
            double mejor = Double.MAX_VALUE;

            // Buscar el par (a,b) de clusters activos más cercano
            for (int i = 0; i < n; i++) {
                if (!activo[i]) continue;
                int j = vecino[i];
                if (j != -1 && activo[j]) {
                    double dist = distVecino[i];
                    if (dist > 0 && dist < mejor) {
                        mejor = dist;
                        a = i;
                        b = j;
                    }
                }
            }

            // Si no se encontró par válido, terminamos el proceso
            if (a == -1 || b == -1) break;

            // Aseguramos que a < b (solo por comodidad al actualizar)
            if (a > b) { int t = a; a = b; b = t; }

            // Creamos un nuevo nodo interno que une a los clusters a y b
            NodoArbol nodoA = clusters[a].nodo;
            NodoArbol nodoB = clusters[b].nodo;
            NodoArbol nuevo = new NodoArbol(null, null, -1.0);
            nuevo.setIzquierdo(nodoA);
            nuevo.setDerecho(nodoB);
            nuevo.setDistancia(mejor); // altura del nodo = distancia entre los clusters

            // El cluster a ahora representa al cluster fusionado
            clusters[a].nodo = nuevo;
            // Marcamos cluster b como ya fusionado (inactivo)
            activo[b] = false;

            // Actualizamos distancias usando enlace simple:
            // dist(a,k) = min(dist(a,k), dist(b,k)) para todo k activo
            for (int k = 0; k < n; k++) {
                if (!activo[k] || k == a) continue;
                double nuevaDist = Math.min(matriz.obtener(a, k), matriz.obtener(b, k));
                matriz.insertar(a, k, nuevaDist);
                matriz.insertar(k, a, nuevaDist);
            }

            // Recalcular el vecino más cercano para el cluster a
            recomputarMasCercanoDe(matriz, activo, vecino, distVecino, a, n);

            // Recalcular vecinos para todos aquellos que tenían a b como mejor vecino
            for (int i = 0; i < n; i++) {
                if (!activo[i] || vecino[i] == b)
                    recomputarMasCercanoDe(matriz, activo, vecino, distVecino, i, n);
            }
        }

        // Al final, solo queda un cluster activo: su nodo será la raíz del árbol
        NodoArbol raiz = null;
        for (int i = 0; i < n; i++)
            if (activo[i]) raiz = clusters[i].nodo;

        ArbolBinario arbol = new ArbolBinario();
        arbol.setRaiz(raiz);
        return arbol;
    }

    // Inicializa, para cada cluster activo, quién es su vecino más cercano y a qué distancia se encuentra.
    private void inicializarMasCercano(Matriz m, boolean[] activo, int[] nearest,
                                       double[] nearestDist, int n) {
        for (int i = 0; i < n; i++) {
            if (!activo[i]) {
                nearest[i] = -1;
                nearestDist[i] = -1;
                continue;
            }

            double best = Double.MAX_VALUE;
            int idx = -1;

            // Buscar j distinto de i tal que la distancia sea mínima
            for (int j = 0; j < n; j++) {
                if (i == j || !activo[j]) continue;
                double d = m.obtener(i, j);
                if (d > 0 && d < best) {
                    best = d;
                    idx = j;
                }
            }
            nearest[i] = idx;
            nearestDist[i] = (idx == -1) ? -1 : best;
        }
    }

    // Recalcula el vecino más cercano para un cluster i concreto, considerando solo los clusters que sigan activos.
    private void recomputarMasCercanoDe(Matriz m, boolean[] activo, int[] nearest,
                                        double[] nearestDist, int i, int n) {
        double best = Double.MAX_VALUE;
        int idx = -1;

        for (int j = 0; j < n; j++) {
            if (i == j || !activo[j]) continue;
            double d = m.obtener(i, j);
            if (d > 0 && d < best) {
                best = d;
                idx = j;
            }
        }
        nearest[i] = idx;
        nearestDist[i] = (idx == -1) ? -1 : best;
    }
}
