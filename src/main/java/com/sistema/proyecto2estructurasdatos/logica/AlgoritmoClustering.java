package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.*;

// Algoritmo de Clustering Jerárquico Aglomerativo
// Implementa el metodo bottom-up para construcción del dendrograma
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

    private void procesarDatos(Lista<Dato> datos) {
        int dimension = datos.obtener(0).getVectorOriginal().tamanio();

        // Normalizar cada dimensión por separado
        for (int dim = 0; dim < dimension; dim++) {
            // Si la columna está ignorada, continuar
            if (columnasIgnoradas != null && dim < columnasIgnoradas.length &&
                    columnasIgnoradas[dim]) {
                continue;
            }

            // Extraer valores de esta dimensión
            Vector valoresDimension = new Vector(datos.tamanio());
            for (int i = 0; i < datos.tamanio(); i++) {
                valoresDimension.agregar(datos.obtener(i).getVectorOriginal().obtener(dim));
            }

            // Detectar si la columna es constante (todos valores iguales)
            boolean esConstante = esColumnaConstante(valoresDimension);

            // Normalizar valores
            Vector valoresNormalizados;
            if (esConstante) {
                // Si es constante, dejar todos en 0
                valoresNormalizados = new Vector(datos.tamanio());
                for (int i = 0; i < datos.tamanio(); i++) {
                    valoresNormalizados.agregar(0.0);
                }
            } else {
                valoresNormalizados = normalizacion.normalizar(valoresDimension);
            }

            // Obtener peso para esta dimensión
            double peso = (pesos != null && dim < pesos.length) ? pesos[dim] : 1.0;

            // Actualizar datos con valores normalizados y ponderados
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


    /**
     * Verifica si una columna tiene todos sus valores iguales
     */
    private boolean esColumnaConstante(Vector valores) {
        if (valores.tamanio() == 0) return true;

        double primerValor = valores.obtener(0);
        double tolerancia = 1e-10;

        for (int i = 1; i < valores.tamanio(); i++) {
            if (Math.abs(valores.obtener(i) - primerValor) > tolerancia) {
                return false;
            }
        }

        return true;
    }


    // Crea matriz de distancias simétrica (triángulo superior) O(n^2 · m)
    // O(n^2 · m), simétrica
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

    // Inicializa el vecino más cercano de cada i activo
    private void inicializarNearest(Matriz m, boolean[] activo, int[] nearest, double[] nearestDist, int n) {
        for (int i = 0; i < n; i++) {
            if (!activo[i]) { nearest[i] = -1; nearestDist[i] = -1; continue; }
            double best = Double.MAX_VALUE;
            int idx = -1;
            for (int j = 0; j < n; j++) {
                if (i == j || !activo[j]) continue;
                double d = m.obtener(i, j);
                if (d > 0 && d < best) { best = d; idx = j; }
            }
            nearest[i] = idx;
            nearestDist[i] = (idx == -1) ? -1 : best;
        }
    }

    // Recomputar completamente el vecino de 'i'
    private void recomputarNearestDe(Matriz m, boolean[] activo, int[] nearest, double[] nearestDist, int i, int n) {
        double best = Double.MAX_VALUE;
        int idx = -1;
        for (int j = 0; j < n; j++) {
            if (i == j || !activo[j]) continue;
            double d = m.obtener(i, j);
            if (d > 0 && d < best) { best = d; idx = j; }
        }
        nearest[i] = idx;
        nearestDist[i] = (idx == -1) ? -1 : best;
    }

    // Construye el dendrograma usando clustering jerárquico aglomerativo
    // Construye el dendrograma en O(n^2 · m)
    private ArbolBinario construirDendrograma(Lista<Dato> datos, Matriz matrizDistancias) {
        int n = datos.tamanio();

        // Clusters por índice de matriz (reutilizaremos índices)
        Cluster[] clusters = new Cluster[n];
        boolean[] activo = new boolean[n];

        // Inicializar clusters hoja
        for (int i = 0; i < n; i++) {
            Dato d = datos.obtener(i);
            NodoArbol hoja = new NodoArbol(d.getEtiqueta(), d.getVectorProcesado(), d.getIndice());
            clusters[i] = new Cluster(hoja, i);
            activo[i] = true;
        }

        // Vecino más cercano cacheado
        int[] nearest = new int[n];
        double[] nearestDist = new double[n];
        inicializarNearest(matrizDistancias, activo, nearest, nearestDist, n);

        // Repetir n-1 fusiones
        for (int paso = 0; paso < n - 1; paso++) {
            // 1) Encontrar el mejor par global desde el cache
            int a = -1, b = -1;
            double best = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (!activo[i]) continue;
                int j = nearest[i];
                if (j != -1 && activo[j]) {
                    double dist = nearestDist[i];
                    if (dist > 0 && dist < best) {
                        best = dist; a = i; b = j;
                    }
                }
            }
            if (a == -1 || b == -1) break; // nada que fusionar

            // Asegurar a < b para consistencia
            if (a > b) { int tmp = a; a = b; b = tmp; }

            // 2) Crear nodo unión y cluster fusionado en índice 'a'
            NodoArbol nuevoNodo = new NodoArbol(clusters[a].nodo, clusters[b].nodo, best);
            Cluster fusionado = Cluster.fusionar(clusters[a], clusters[b], nuevoNodo, a);
            clusters[a] = fusionado;
            activo[b] = false;

            // 3) Actualizar distancias desde 'a' hacia todos los activos (usando centroides)
            Vector cenA = clusters[a].centroide();
            for (int t = 0; t < n; t++) {
                if (t == a || !activo[t]) continue;
                Vector cenT = clusters[t].centroide();
                double distAT = distancia.calcular(cenA, cenT); // O(m)
                matrizDistancias.insertar(a, t, distAT);
                matrizDistancias.insertar(t, a, distAT);

                // invalidar fila/columna de b (opcional, por claridad)
                matrizDistancias.insertar(b, t, -1.0);
                matrizDistancias.insertar(t, b, -1.0);
            }
            matrizDistancias.insertar(a, a, 0.0);
            // fila/col b marcada inactiva
            for (int t = 0; t < n; t++) {
                matrizDistancias.insertar(b, t, -1.0);
                matrizDistancias.insertar(t, b, -1.0);
            }

            // 4) Recalcular nearest SOLO para:
            //    - i = a (porque cambió)
            //    - cualquier i cuyo vecino era a o b
            //    - y cualquiera donde nueva dist a-i sea mejor
            recomputarNearestDe(matrizDistancias, activo, nearest, nearestDist, a, n);
            for (int i = 0; i < n; i++) {
                if (!activo[i] || i == a) continue;

                // Si su vecino apuntaba a 'a' o 'b', hay que recalcular
                if (nearest[i] == a || nearest[i] == b) {
                    recomputarNearestDe(matrizDistancias, activo, nearest, nearestDist, i, n);
                    continue;
                }

                // Si la nueva distancia i-a mejora, actualiza
                double cand = matrizDistancias.obtener(i, a);
                if (cand > 0 && cand < nearestDist[i]) {
                    nearest[i] = a;
                    nearestDist[i] = cand;
                }
            }
        }

        // La raíz es el único activo que queda
        int raizIdx = -1;
        for (int i = 0; i < n; i++) if (activo[i]) { raizIdx = i; break; }
        return new ArbolBinario(clusters[raizIdx].nodo);
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



    // Clase interna para representar un cluster durante el proceso
// Mantiene nodo del árbol, índice en la matriz, y estado para centroides.
    private static class Cluster {
        NodoArbol nodo;
        int indiceMatriz;

        // Para centroides “en vivo”
        Vector suma;  // suma componente a componente de todas las hojas
        int count;    // cantidad de hojas en el cluster

        Cluster(NodoArbol nodo, int indiceMatriz) {
            this.nodo = nodo;
            this.indiceMatriz = indiceMatriz;

            // Inicializar suma y count a partir del nodo (hoja)
            if (nodo != null && nodo.getDatos() != null) {
                Vector v = nodo.getDatos();
                this.suma = new Vector(v.tamanio());
                for (int k = 0; k < v.tamanio(); k++) this.suma.agregar(v.obtener(k));
                this.count = 1;
            } else {
                this.suma = new Vector();
                this.count = 0;
            }
        }

        // Fusionar dos clusters: suma = suma1 + suma2, count = count1 + count2
        static Cluster fusionar(Cluster c1, Cluster c2, NodoArbol nuevoNodo, int indiceMatriz) {
            Cluster c = new Cluster(nuevoNodo, indiceMatriz);
            // inicializa suma con dimensión de c1
            Vector s = new Vector(c1.suma.tamanio());
            for (int i = 0; i < c1.suma.tamanio(); i++) {
                s.agregar(c1.suma.obtener(i) + c2.suma.obtener(i));
            }
            c.suma = s;
            c.count = c1.count + c2.count;
            return c;
        }

        // Devuelve el centroide en O(m) (sin recorrer hojas)
        Vector centroide() {
            Vector cen = new Vector(suma.tamanio());
            double inv = (count == 0) ? 0.0 : (1.0 / count);
            for (int i = 0; i < suma.tamanio(); i++) {
                cen.agregar(suma.obtener(i) * inv);
            }
            return cen;
        }
    }

}
