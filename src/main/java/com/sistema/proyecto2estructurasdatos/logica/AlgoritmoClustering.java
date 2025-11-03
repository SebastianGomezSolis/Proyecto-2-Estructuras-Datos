package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.*;

/**
 * Algoritmo de Clustering Jerárquico Aglomerativo (HAC).
 *
 * Idea general:
 * - Cada fila del dataset inicia como un "cluster" individual (una hoja).
 * - En cada paso se juntan los dos clusters más cercanos.
 * - Al unirlos se crea un nodo padre con una "distancia" (altura en el dendrograma).
 * - Se repite hasta quedar un solo cluster (la raíz del árbol).
 *
 * Particularidades de esta implementación:
 * - Normaliza SOLO columnas numéricas (para no dañar variables one-hot/categóricas).
 * - Permite pesos por columna (importancia relativa) y columnas ignoradas.
 * - Usa "enlace simple" (single linkage) al actualizar distancias entre clusters.
 */
public class AlgoritmoClustering {

    /** Estrategia para normalizar columnas numéricas (Min-Max, Z-Score, Log, etc.). */
    private INormalizacion normalizacion;

    /** Estrategia para medir distancia entre vectores (Euclidiana, Manhattan, Coseno, etc.). */
    private IDistancia distancia;

    /** Peso por columna (misma longitud que el vector de cada dato; 1.0 si no se indica). */
    private double[] pesos;

    /** Marcador de columnas ignoradas: true = ignorar (se fuerza a 0), false = usar. */
    private boolean[] columnasIgnoradas;

    /**
     * Columnas numéricas "reales".
     * true  = numérica → se normaliza
     * false = no numérica (ej. one-hot) → NO se normaliza
     */
    private boolean[] columnasNumericas;

    /** Se configura desde el controlador para indicar cuáles columnas son realmente numéricas. */
    public void setColumnasNumericas(boolean[] columnasNumericas) {
        this.columnasNumericas = columnasNumericas;
    }

    /**
     * Constructor: requiere estrategias válidas de normalización y distancia.
     * También recibe pesos e ignoradas (pueden ser null si no se usan).
     */
    public AlgoritmoClustering(INormalizacion normalizacion, IDistancia distancia,
                               double[] pesos, boolean[] columnasIgnoradas) {
        if (normalizacion == null)
            throw new IllegalArgumentException("Normalización no puede ser null");
        if (distancia == null)
            throw new IllegalArgumentException("Distancia no puede ser null");

        this.normalizacion = normalizacion;
        this.distancia = distancia;
        this.pesos = pesos;
        this.columnasIgnoradas = columnasIgnoradas;
    }

    /**
     * Ejecuta el proceso completo:
     * 1) Prepara los datos (normaliza numéricas, aplica pesos, respeta ignoradas).
     * 2) Calcula la matriz de distancias entre todos los pares.
     * 3) Construye el dendrograma uniendo de a dos los más cercanos.
     */
    public ArbolBinario ejecutar(Lista<Dato> datos) {
        if (datos == null || datos.tamanio() == 0)
            throw new IllegalArgumentException("La lista de datos está vacía");

        // normalización y pesos (solo numéricas) + obligación de 0 para ignoradas
        procesarDatos(datos);

        //distancias entre todos (matriz simétrica n x n)
        Matriz matriz = crearMatrizDistancias(datos);

        //liga progresivamente los más cercanos y arma el árbol
        return construirDendrograma(datos, matriz);
    }

    /**
     * Recorre columna por columna y:
     * - Si la columna es numérica (según columnasNumericas) y no está ignorada:
     *     • extrae los valores
     *     • si todos son iguales → normaliza a 0 (evita dividir por 0 en Z-Score, etc.)
     *     • si no, aplica la normalización elegida
     *     • aplica el peso de la columna
     *     • guarda en el vector "procesado" del Dato
     * - Si la columna está ignorada → más adelante se fuerza a 0 en todos los datos.
     */
    private void procesarDatos(Lista<Dato> datos) {
        int dimension = datos.obtener(0).getVectorOriginal().tamanio();

        // Recorremos dimensión por dimensión (columna por columna)
        for (int dim = 0; dim < dimension; dim++) {

            //Saltar columnas no numéricas (ej. one-hot) para NO normalizarlas
            if (columnasNumericas != null && dim < columnasNumericas.length && !columnasNumericas[dim]) {
                continue; // se dejan tal cual (más abajo, si están ignoradas, se pondrán en 0)
            }

            //Saltar columnas marcadas como ignoradas (las dejaremos en 0 al final)
            if (columnasIgnoradas != null && dim < columnasIgnoradas.length && columnasIgnoradas[dim]) {
                continue;
            }

            //Extraemos todos los valores de esta columna para normalizar
            Vector valores = new Vector(datos.tamanio());
            for (int i = 0; i < datos.tamanio(); i++) {
                valores.agregar(datos.obtener(i).getVectorOriginal().obtener(dim));
            }

            //Si la columna es constante, la normalizamos a 0 para todos.
            Vector normalizados;
            boolean esConstante = esColumnaConstante(valores);
            if (esConstante) {
                normalizados = new Vector(datos.tamanio());
                for (int i = 0; i < datos.tamanio(); i++) normalizados.agregar(0.0);
            } else {
                normalizados = normalizacion.normalizar(valores);
            }

            //Leemos el peso para esta columna (1.0 por defecto si no hay)
            double peso = (pesos != null && dim < pesos.length) ? pesos[dim] : 1.0;

            //Guardamos el valor procesado (normalizado * peso) en cada Dato
            for (int i = 0; i < datos.tamanio(); i++) {
                Dato d = datos.obtener(i);

                // Creamos vector procesado si aún no existe
                if (d.getVectorProcesado() == null)
                    d.setVectorProcesado(new Vector(dimension));

                double valor = normalizados.obtener(i) * peso;

                // Si el vector ya tiene esa posición, se establece; si no, se agrega
                if (d.getVectorProcesado().tamanio() > dim)
                    d.getVectorProcesado().establecer(dim, valor);
                else
                    d.getVectorProcesado().agregar(valor);
            }
        }

        //Al final: si una columna está marcada como ignorada, se fuerza a 0 SIEMPRE.
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

    /**
     * Devuelve true si todos los valores del vector son (prácticamente) iguales.
     * Se usa un umbral pequeño (1e-10) para cubrir errores de punto flotante.
     */
    private boolean esColumnaConstante(Vector valores) {
        if (valores.tamanio() == 0) return true;
        double base = valores.obtener(0);
        for (int i = 1; i < valores.tamanio(); i++) {
            if (Math.abs(valores.obtener(i) - base) > 1e-10)
                return false;
        }
        return true;
    }

    /**
     * Construye una matriz n×n con la distancia entre cada par de puntos
     * (usando los vectores *procesados*).
     * Propiedades:
     * - diagonal = 0
     * - simétrica: m[i,j] = m[j,i]
     */
    private Matriz crearMatrizDistancias(Lista<Dato> datos) {
        int n = datos.tamanio();
        Matriz m = new Matriz(n, n);

        for (int i = 0; i < n; i++) {
            m.insertar(i, i, 0.0); // distancia de un punto consigo mismo
            Vector v1 = datos.obtener(i).getVectorProcesado();

            // Solo calculamos la mitad superior y copiamos a la inferior (simetría)
            for (int j = i + 1; j < n; j++) {
                Vector v2 = datos.obtener(j).getVectorProcesado();
                double d = distancia.calcular(v1, v2);
                m.insertar(i, j, d);
                m.insertar(j, i, d);
            }
        }
        return m;
    }

    /**
     * Une repetidamente los dos clusters activos más cercanos.
     * - Cada dato inicia como un cluster/hoja.
     * - Se mantiene quién es el vecino más cercano de cada cluster para acelerar la búsqueda.
     * - Al unir A y B:
     *     • se crea un nodo padre con distancia = matriz[A,B]
     *     • A pasa a representar al nuevo cluster
     *     • B se desactiva
     *     • se actualizan distancias A–k con "enlace simple": min(dist(A,k), dist(B,k))
     *     • se recalculan vecinos cercanos donde haga falta
     */
    private ArbolBinario construirDendrograma(Lista<Dato> datos, Matriz matriz) {
        int n = datos.tamanio();

        // Estructuras por cluster:
        Cluster[] clusters = new Cluster[n]; // guarda el nodo actual de cada cluster
        boolean[] activo = new boolean[n];   // indica si ese cluster sigue "vivo" (no unido)

        //Inicializar: cada dato es su propio cluster con una hoja como nodo
        for (int i = 0; i < n; i++) {
            Dato d = datos.obtener(i);
            // Creamos una hoja con etiqueta, vector y un índice/identificador
            NodoArbol hoja = new NodoArbol(d.getEtiqueta(), d.getVectorProcesado(), d.getIndice());
            clusters[i] = new Cluster(hoja, i);
            activo[i] = true;
        }

        //Para cada cluster activo, guardamos cuál es su vecino más cercano y a qué distancia
        int[] vecino = new int[n];        // índice del vecino más cercano
        double[] distVecino = new double[n]; // distancia a ese vecino
        inicializarMasCercano(matriz, activo, vecino, distVecino, n);

        // Hacemos n-1 uniones como máximo (hasta que quede 1 cluster activo)
        for (int paso = 0; paso < n - 1; paso++) {
            int a = -1, b = -1;
            double mejor = Double.MAX_VALUE;

            // Buscamos el par activo con la menor distancia positiva
            for (int i = 0; i < n; i++) {
                if (!activo[i]) continue;
                int j = vecino[i];
                if (j != -1 && activo[j]) {
                    double dist = distVecino[i];
                    if (dist > 0 && dist < mejor) {
                        mejor = dist; // nueva mejor distancia
                        a = i;        // cluster A
                        b = j;        // cluster B
                    }
                }
            }

            // Si no hallamos pareja válida, salimos (puede pasar por datos duplicados, etc.)
            if (a == -1 || b == -1) break;

            // Aseguramos a < b (solo por comodidad al actualizar matrices)
            if (a > b) { int t = a; a = b; b = t; }

            //Creamos el nodo padre que une a los dos clusters más cercanos
            NodoArbol nodoA = clusters[a].nodo;
            NodoArbol nodoB = clusters[b].nodo;
            NodoArbol nuevo = new NodoArbol(null, null, -1.0); // nodo interno (sin etiqueta)
            nuevo.setIzquierdo(nodoA);
            nuevo.setDerecho(nodoB);
            nuevo.setDistancia(mejor); // altura = distancia a la que se unieron

            // El cluster "a" ahora representa al cluster unido; "b" se desactiva
            clusters[a].nodo = nuevo;
            activo[b] = false;

            //Actualizamos distancias del nuevo cluster "a" al resto con ENLACE SIMPLE:
            // dist(a,k) = min(dist(a,k), dist(b,k))
            for (int k = 0; k < n; k++) {
                if (!activo[k] || k == a) continue;
                double nuevaDist = Math.min(matriz.obtener(a, k), matriz.obtener(b, k));
                matriz.insertar(a, k, nuevaDist);
                matriz.insertar(k, a, nuevaDist);
            }

            //Recalcular el vecino más cercano para:
            //    - el propio 'a' (cambió su perfil de distancias),
            //    - cualquiera cuyo vecino fuera 'b' (ya no existe).
            recomputarMasCercanoDe(matriz, activo, vecino, distVecino, a, n);
            for (int i = 0; i < n; i++) {
                if (!activo[i] || vecino[i] == b)
                    recomputarMasCercanoDe(matriz, activo, vecino, distVecino, i, n);
            }
        }

        //La raíz del árbol es el nodo del único cluster que queda activo
        NodoArbol raiz = null;
        for (int i = 0; i < n; i++) if (activo[i]) raiz = clusters[i].nodo;

        ArbolBinario arbol = new ArbolBinario();
        arbol.setRaiz(raiz);
        return arbol;
    }

    /**
     * Para cada cluster activo, determina su vecino más cercano inicial.
     * Si no hay candidato válido, deja -1 y distancia -1.
     */
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
            for (int j = 0; j < n; j++) {
                if (i == j || !activo[j]) continue;
                double d = m.obtener(i, j);
                // ignoramos 0 (mismo punto) y nos quedamos con la menor distancia positiva
                if (d > 0 && d < best) {
                    best = d;
                    idx = j;
                }
            }
            nearest[i] = idx;
            nearestDist[i] = (idx == -1) ? -1 : best;
        }
    }

    /**
     * Recalcula el vecino más cercano del cluster i (siempre que esté activo).
     * Se usa cuando cambian sus distancias por una unión reciente.
     */
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
