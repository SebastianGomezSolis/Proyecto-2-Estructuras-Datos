package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.*;


public class AlgoritmoClustering {

    private INormalizacion[] normalizaciones; // CAMBIO: array
    private IDistancia distancia;
    private double[] pesos;
    private boolean[] columnasIgnoradas;
    private boolean[] columnasNumericas;

    public void setColumnasNumericas(boolean[] columnasNumericas) {
        this.columnasNumericas = columnasNumericas;
    }

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

    public ArbolBinario ejecutar(Lista<Dato> datos) {
        if (datos == null || datos.tamanio() == 0)
            throw new IllegalArgumentException("La lista de datos está vacía");

        procesarDatos(datos);
        Matriz matriz = crearMatrizDistancias(datos);
        return construirDendrograma(datos, matriz);
    }

    private void procesarDatos(Lista<Dato> datos) {
        int dimension = datos.obtener(0).getVectorOriginal().tamanio();

        for (int dim = 0; dim < dimension; dim++) {
            // Saltar no numéricas
            if (columnasNumericas != null && dim < columnasNumericas.length &&
                    !columnasNumericas[dim]) {
                continue;
            }

            // Saltar ignoradas
            if (columnasIgnoradas != null && dim < columnasIgnoradas.length &&
                    columnasIgnoradas[dim]) {
                continue;
            }

            // Extraer valores
            Vector valores = new Vector(datos.tamanio());
            for (int i = 0; i < datos.tamanio(); i++) {
                valores.agregar(datos.obtener(i).getVectorOriginal().obtener(dim));
            }

            // Normalizar con estrategia específica
            Vector normalizados;
            boolean esConstante = esColumnaConstante(valores);

            if (esConstante) {
                normalizados = new Vector(datos.tamanio());
                for (int i = 0; i < datos.tamanio(); i++)
                    normalizados.agregar(0.0);
            } else {
                // CAMBIO CRÍTICO: usar normalizaciones[dim]
                INormalizacion normParaEstaCol =
                        (normalizaciones != null && dim < normalizaciones.length &&
                                normalizaciones[dim] != null)
                                ? normalizaciones[dim]
                                : new NormalizacionMinMax();

                normalizados = normParaEstaCol.normalizar(valores);
            }

            // Aplicar peso
            double peso = (pesos != null && dim < pesos.length) ? pesos[dim] : 1.0;

            for (int i = 0; i < datos.tamanio(); i++) {
                Dato d = datos.obtener(i);
                if (d.getVectorProcesado() == null)
                    d.setVectorProcesado(new Vector(dimension));

                double valor = normalizados.obtener(i) * peso;

                if (d.getVectorProcesado().tamanio() > dim)
                    d.getVectorProcesado().establecer(dim, valor);
                else
                    d.getVectorProcesado().agregar(valor);
            }
        }

        // Forzar a 0 columnas ignoradas
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

    private boolean esColumnaConstante(Vector valores) {
        if (valores.tamanio() == 0) return true;
        double base = valores.obtener(0);
        for (int i = 1; i < valores.tamanio(); i++) {
            if (Math.abs(valores.obtener(i) - base) > 1e-10)
                return false;
        }
        return true;
    }

    private Matriz crearMatrizDistancias(Lista<Dato> datos) {
        int n = datos.tamanio();
        Matriz m = new Matriz(n, n);

        for (int i = 0; i < n; i++) {
            m.insertar(i, i, 0.0);
            Vector v1 = datos.obtener(i).getVectorProcesado();

            for (int j = i + 1; j < n; j++) {
                Vector v2 = datos.obtener(j).getVectorProcesado();
                double d = distancia.calcular(v1, v2);
                m.insertar(i, j, d);
                m.insertar(j, i, d);
            }
        }
        return m;
    }

    private ArbolBinario construirDendrograma(Lista<Dato> datos, Matriz matriz) {
        int n = datos.tamanio();

        Cluster[] clusters = new Cluster[n];
        boolean[] activo = new boolean[n];

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

        int[] vecino = new int[n];
        double[] distVecino = new double[n];
        inicializarMasCercano(matriz, activo, vecino, distVecino, n);

        for (int paso = 0; paso < n - 1; paso++) {
            int a = -1, b = -1;
            double mejor = Double.MAX_VALUE;

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

            if (a == -1 || b == -1) break;

            if (a > b) { int t = a; a = b; b = t; }

            NodoArbol nodoA = clusters[a].nodo;
            NodoArbol nodoB = clusters[b].nodo;
            NodoArbol nuevo = new NodoArbol(null, null, -1.0);
            nuevo.setIzquierdo(nodoA);
            nuevo.setDerecho(nodoB);
            nuevo.setDistancia(mejor);

            clusters[a].nodo = nuevo;
            activo[b] = false;

            for (int k = 0; k < n; k++) {
                if (!activo[k] || k == a) continue;
                double nuevaDist = Math.min(matriz.obtener(a, k), matriz.obtener(b, k));
                matriz.insertar(a, k, nuevaDist);
                matriz.insertar(k, a, nuevaDist);
            }

            recomputarMasCercanoDe(matriz, activo, vecino, distVecino, a, n);
            for (int i = 0; i < n; i++) {
                if (!activo[i] || vecino[i] == b)
                    recomputarMasCercanoDe(matriz, activo, vecino, distVecino, i, n);
            }
        }

        NodoArbol raiz = null;
        for (int i = 0; i < n; i++)
            if (activo[i]) raiz = clusters[i].nodo;

        ArbolBinario arbol = new ArbolBinario();
        arbol.setRaiz(raiz);
        return arbol;
    }

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
                if (d > 0 && d < best) {
                    best = d;

                    idx = j;
                }
            }
            nearest[i] = idx;
            nearestDist[i] = (idx == -1) ? -1 : best;
        }
    }

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
