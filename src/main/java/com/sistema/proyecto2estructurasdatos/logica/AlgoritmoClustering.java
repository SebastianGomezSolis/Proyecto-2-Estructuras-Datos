package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.*;

/**
 * Esta clase implementa el Algoritmo de Clustering Jerárquico Aglomerativo.
 *
 * Qué es clustering?
 * Es como organizar cosas en grupos. Imaginemos que hay muchas fotos y tenemos que
 * agruparlas automáticamente por parecido. Este algoritmo hace exactamente eso
 * con cualquier tipo de datos.
 *
 * Qué es "jerárquico aglomerativo"?
 * - Jerárquico: Crea una jerarquía (como un árbol genealógico)
 * - Aglomerativo: Va de abajo hacia arriba, juntando cosas poco a poco
 *
 * Proceso que sigue:
 * 1. Normalizar datos (ponerlos en la misma escala)
 * 2. Aplicar pesos a las variables (darle más importancia a algunas)
 * 3. Calcular qué tan diferentes son todos entre sí (matriz de distancias)
 * 4. Ir juntando los más parecidos, paso a paso
 * 5. Construir un árbol que muestra cómo se agruparon
 */

public class AlgoritmoClustering {
    // Estrategia para normalizar los datos (ponerlos en la misma escala)
    private INormalizacion normalizacion;

    // Estrategia para medir qué tan diferentes son dos cosas
    private IDistancia distancia;

    // Pesos: qué tanta importancia le damos a cada característica
    private double[] pesos;

    // Columnas que queremos ignorar (no usar en el análisis)
    private boolean[] columnasIgnoradas;

    /**
     * Constructor: configura el algoritmo con todas sus opciones
     *
     *  normalizacion Cómo normalizar los datos
     *  distancia Cómo medir diferencias
     *  pesos Importancia de cada característica
     *  columnasIgnoradas Cuáles columnas no usar
     */
    public AlgoritmoClustering(INormalizacion normalizacion, IDistancia distancia, double[] pesos, boolean[] columnasIgnoradas) {
        // Validación: no podemos trabajar sin estos dos elementos esenciales
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

    /**
     * Este es el método principal que ejecuta TODO el algoritmo.
     *  datos La lista de datos que queremos agrupar
     * return Un árbol (dendrograma) que muestra cómo se agruparon los datos
     */
    public ArbolBinario ejecutar(Lista<Dato> datos) {
        // Validar que tengamos datos para procesar
        if (datos == null || datos.tamanio() == 0) {
            throw new IllegalArgumentException("La lista de datos está vacía");
        }

        // === PASO 1: Preparar los datos ===
        // Normalizar (escalar) y aplicar pesos
        // Complejidad: O(n·m) donde n = número de datos, m = número de características
        procesarDatos(datos);

        // === PASO 2: Calcular todas las distancias ===
        // Crear una tabla que dice qué tan diferente es cada dato de todos los demás
        // Complejidad: O(n²·m)
        Matriz matrizDistancias = crearMatrizDistancias(datos);

        // === PASO 3: Construir el árbol de agrupamiento ===
        // Ir juntando los más parecidos paso a paso
        // Complejidad: O(n²·m)
        ArbolBinario dendrograma = construirDendrograma(datos, matrizDistancias);

        return dendrograma;
    }

    /**
     * Este método procesa los datos: normaliza y aplica pesos.
     *
     * Por qué normalizar?
     * Imaginemos que tenemos datos de altura (en cm: 160-190) y peso (en kg: 50-90).
     * La altura tiene números más grandes, así que tendría más influencia.
     * Normalizar pone todo en la misma escala (por ejemplo, de 0 a 1).
     *
     * Por qué pesos?
     * Podemos decidir que algunas características son más importantes que otras.
     * Por ejemplo, en películas, el género podría ser más importante que el año.
     */
    private void procesarDatos(Lista<Dato> datos) {
        // Obtener cuántas características tiene cada dato
        int dimension = datos.obtener(0).getVectorOriginal().tamanio();

        // Procesar cada característica (dimensión) por separado
        for (int dim = 0; dim < dimension; dim++) {
            // Si esta columna está marcada como "ignorada", la saltamos
            if (columnasIgnoradas != null && dim < columnasIgnoradas.length &&
                    columnasIgnoradas[dim]) {
                continue;
            }

            // === PASO 1: Extraer todos los valores de esta característica ===
            // Por ejemplo, si dim=0 es "edad", extraemos todas las edades
            Vector valoresDimension = new Vector(datos.tamanio());
            for (int i = 0; i < datos.tamanio(); i++) {
                valoresDimension.agregar(datos.obtener(i).getVectorOriginal().obtener(dim));
            }

            // === PASO 2: Verificar si todos los valores son iguales ===
            // Si todos tienen la misma edad (ej: todos 25), no aporta información
            boolean esConstante = esColumnaConstante(valoresDimension);

            // === PASO 3: Normalizar los valores ===
            Vector valoresNormalizados;
            if (esConstante) {
                // Si todos son iguales, ponemos todos en 0 (no aporta nada)
                valoresNormalizados = new Vector(datos.tamanio());
                for (int i = 0; i < datos.tamanio(); i++) {
                    valoresNormalizados.agregar(0.0);
                }
            } else {
                // Normalizar usando la estrategia elegida (Min-Max, Z-Score, etc.)
                valoresNormalizados = normalizacion.normalizar(valoresDimension);
            }

            // === PASO 4: Aplicar el peso ===
            // Si esta característica es importante, le damos más peso
            double peso = (pesos != null && dim < pesos.length) ? pesos[dim] : 1.0;

            // === PASO 5: Guardar los valores procesados ===
            for (int i = 0; i < datos.tamanio(); i++) {
                Dato dato = datos.obtener(i);

                // Crear el vector procesado si no existe
                if (dato.getVectorProcesado() == null) {
                    dato.setVectorProcesado(new Vector(dimension));
                }

                // Calcular valor final: normalizado × peso
                double valorPonderado = valoresNormalizados.obtener(i) * peso;

                // Guardar el valor procesado
                if (dato.getVectorProcesado().tamanio() > dim) {
                    dato.getVectorProcesado().establecer(dim, valorPonderado);
                } else {
                    dato.getVectorProcesado().agregar(valorPonderado);
                }
            }
        }

        // Para columnas ignoradas, poner valores en 0
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
     * Verifica si una columna tiene todos sus valores iguales.
     * Por ejemplo: si todos tienen edad 25, o todos están en la misma ciudad.
     *
     * Usamos una tolerancia pequeña para comparar números decimales,
     * porque a veces 1.0000001 y 1.0 deberían considerarse iguales.
     */
    private boolean esColumnaConstante(Vector valores) {
        if (valores.tamanio() == 0) return true;

        double primerValor = valores.obtener(0);
        double tolerancia = 1e-10; // 0.0000000001 - muy pequeño

        // Comparamos todos los valores con el primero
        for (int i = 1; i < valores.tamanio(); i++) {
            if (Math.abs(valores.obtener(i) - primerValor) > tolerancia) {
                return false; // Encontramos uno diferente
            }
        }

        return true; // Todos son iguales
    }

    /**
     * Crea una matriz de distancias: una tabla que dice qué tan diferente
     * es cada dato de todos los demás.
     *
     * Por ejemplo, si tenemos 5 películas, creamos una tabla 5×5 donde
     * cada celda dice qué tan diferentes son dos películas.
     *
     * La matriz es simétrica: la distancia de A a B es igual que de B a A.
     * Complejidad: O(n² × m)
     */
    private Matriz crearMatrizDistancias(Lista<Dato> datos) {
        int n = datos.tamanio();
        Matriz m = new Matriz(n, n);

        // Recorremos todos los pares de datos
        for (int i = 0; i < n; i++) {
            // La distancia de algo consigo mismo es 0
            m.insertar(i, i, 0.0);

            Vector v1 = datos.obtener(i).getVectorProcesado();

            // Solo calculamos el triángulo superior (porque es simétrica)
            for (int j = i + 1; j < n; j++) {
                Vector v2 = datos.obtener(j).getVectorProcesado();

                // Calcular la distancia entre estos dos vectores
                double d = distancia.calcular(v1, v2);

                // Guardar en ambas direcciones (simétrica)
                m.insertar(i, j, d);
                m.insertar(j, i, d);
            }
        }
        return m;
    }

    /**
     * Inicializa el vecino más cercano de cada elemento activo.
     *
     * Para cada dato, encuentra cuál es el dato más cercano a él.
     * Esto acelera el algoritmo porque no tenemos que buscar cada vez.
     */
    private void inicializarMasCercano(Matriz m, boolean[] activo, int[] nearest,
                                    double[] nearestDist, int n) {
        for (int i = 0; i < n; i++) {
            // Si este dato ya fue fusionado (no activo), lo saltamos
            if (!activo[i]) {
                nearest[i] = -1;
                nearestDist[i] = -1;
                continue;
            }

            // Buscar el vecino más cercano a 'i'
            double best = Double.MAX_VALUE;
            int idx = -1;

            for (int j = 0; j < n; j++) {
                if (i == j || !activo[j]) continue; // Saltar si es el mismo o no está activo

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

    /**
     * Recalcula completamente el vecino más cercano de un elemento específico.
     * Se usa después de fusionar clusters para actualizar las distancias.
     */
    private void recomputarMasCercanoDe(Matriz m, boolean[] activo, int[] nearest,
                                     double[] nearestDist, int i, int n) {
        double best = Double.MAX_VALUE;
        int idx = -1;

        // Buscar el nuevo vecino más cercano
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

    /**
     * Este es el corazón del algoritmo: construye el dendrograma.
     *
     * Cómo funciona?
     * 1. Al inicio, cada dato es su propio cluster (grupo)
     * 2. En cada paso, juntamos los dos clusters más cercanos
     * 3. Repetimos n-1 veces hasta que todo esté en un solo cluster
     * 4. El resultado es un árbol que muestra cómo se fueron agrupando
     *
     * Complejidad: O(n² × m)
     */
    private ArbolBinario construirDendrograma(Lista<Dato> datos, Matriz matrizDistancias) {
        int n = datos.tamanio();

        // Array de clusters: al inicio, cada dato es su propio cluster
        Cluster[] clusters = new Cluster[n];

        // Array que dice si un cluster está activo (no fusionado aún)
        boolean[] activo = new boolean[n];

        // Crear un cluster inicial (hoja) para cada dato
        for (int i = 0; i < n; i++) {
            Dato d = datos.obtener(i);
            // Crear nodo hoja del árbol
            NodoArbol hoja = new NodoArbol(d.getEtiqueta(), d.getVectorProcesado(), d.getIndice());
            // Crear cluster con este nodo
            clusters[i] = new Cluster(hoja, i);
            activo[i] = true; // Marcar como activo
        }

        // En vez de buscar el mínimo cada vez, guardamos quién es el vecino de cada uno
        int[] mascercano = new int[n];       // índice del vecino más cercano
        double[] distanciamascercano = new double[n]; // distancia al vecino más cercano
        inicializarMasCercano(matrizDistancias, activo, mascercano, distanciamascercano, n);

        // Necesitamos n-1 fusiones para juntar n elementos en uno solo
        for (int paso = 0; paso < n - 1; paso++) {

            // PASO 1: Encontrar el par de clusters más cercanos
            int a = -1, b = -1;
            double mejor = Double.MAX_VALUE;

            // Revisar todos los clusters activos y sus vecinos
            for (int i = 0; i < n; i++) {
                if (!activo[i]) continue; // Saltar si no está activo

                int j = mascercano[i]; // Su vecino más cercano
                if (j != -1 && activo[j]) {
                    double dist = distanciamascercano[i];
                    if (dist > 0 && dist < mejor) {
                        mejor = dist;
                        a = i;
                        b = j;
                    }
                }
            }

            // Si no encontramos nada que fusionar, terminamos
            if (a == -1 || b == -1) break;

            // Asegurar que a < b para consistencia
            if (a > b) {
                int tmp = a;
                a = b;
                b = tmp;
            }

            // PASO 2: Fusionar los clusters 'a' y 'b'
            // Crear un nodo padre que une ambos clusters
            NodoArbol nuevoNodo = new NodoArbol(clusters[a].nodo, clusters[b].nodo, mejor);

            // Crear cluster fusionado en la posición 'a'
            Cluster fusionado = Cluster.fusionar(clusters[a], clusters[b], nuevoNodo, a);
            clusters[a] = fusionado;

            // Marcar 'b' como inactivo (ya fue fusionado)
            activo[b] = false;

            // PASO 3: Actualizar distancias
            // El nuevo cluster 'a' necesita nuevas distancias a todos los demás
            Vector cenA = clusters[a].centroide();

            for (int t = 0; t < n; t++) {
                if (t == a || !activo[t]) continue;

                // Calcular distancia entre el centroide de 'a' y el de 't'
                Vector cenT = clusters[t].centroide();
                double distAT = distancia.calcular(cenA, cenT);

                // Actualizar matriz (en ambas direcciones)
                matrizDistancias.insertar(a, t, distAT);
                matrizDistancias.insertar(t, a, distAT);

                // Invalidar distancias de 'b' (ya no existe como cluster separado)
                matrizDistancias.insertar(b, t, -1.0);
                matrizDistancias.insertar(t, b, -1.0);
            }

            matrizDistancias.insertar(a, a, 0.0); // Distancia consigo mismo = 0

            // Marcar todas las distancias de 'b' como inválidas
            for (int t = 0; t < n; t++) {
                matrizDistancias.insertar(b, t, -1.0);
                matrizDistancias.insertar(t, b, -1.0);
            }

            // PASO 4: Actualizar caché de vecinos más cercanos
            // Solo actualizamos los que se vieron afectados por la fusión

            // Recalcular para 'a' (porque cambió)
            recomputarMasCercanoDe(matrizDistancias, activo, mascercano, distanciamascercano, a, n);

            // Recalcular para cualquiera que apuntaba a 'a' o 'b'
            for (int i = 0; i < n; i++) {
                if (!activo[i] || i == a) continue;

                // Si su vecino era 'a' o 'b', necesita recalcular
                if (mascercano[i] == a || mascercano[i] == b) {
                    recomputarMasCercanoDe(matrizDistancias, activo, mascercano, distanciamascercano, i, n);
                    continue;
                }

                // Si la nueva distancia a 'a' es mejor, actualizar
                double cand = matrizDistancias.obtener(i, a);
                if (cand > 0 && cand < distanciamascercano[i]) {
                    mascercano[i] = a;
                    distanciamascercano[i] = cand;
                }
            }
        }

        // === RESULTADO FINAL ===
        // Al terminar, solo queda un cluster activo
        int raizIdx = -1;
        for (int i = 0; i < n; i++) {
            if (activo[i]) {
                raizIdx = i;
                break;
            }
        }

        return new ArbolBinario(clusters[raizIdx].nodo);
    }

    /**
     * Encuentra el par de clusters con menor distancia.
     * Es una búsqueda exhaustiva entre todos los pares posibles.
     *
     * Este método se usa en versiones anteriores o para validación.
     */
    private Matriz.ParIndices encontrarMinimo(Matriz matriz, Lista<Cluster> clustersActivos) {
        double minimo = Double.MAX_VALUE;
        int minI = -1, minJ = -1;

        // Revisar todos los pares posibles
        for (int i = 0; i < clustersActivos.tamanio(); i++) {
            for (int j = i + 1; j < clustersActivos.tamanio(); j++) {

                int indiceI = clustersActivos.obtener(i).indiceMatriz;
                int indiceJ = clustersActivos.obtener(j).indiceMatriz;

                double dist;

                // Si es un cluster nuevo (índice -1), calcular distancia directamente
                if (indiceI == -1 || indiceJ == -1) {
                    dist = calcularDistanciaEntreClusters(
                            clustersActivos.obtener(i).nodo,
                            clustersActivos.obtener(j).nodo
                    );
                } else if (indiceI < matriz.getFilas() && indiceJ < matriz.getColumnas()) {
                    // Obtener distancia de la matriz pre-calculada
                    dist = matriz.obtener(indiceI, indiceJ);
                } else {
                    continue;
                }

                // Si encontramos una distancia menor, actualizar
                if (dist > 0 && dist < minimo) {
                    minimo = dist;
                    minI = i;
                    minJ = j;
                }
            }
        }

        return new Matriz.ParIndices(minI, minJ, minimo);
    }

    /**
     * Calcula la distancia entre dos clusters usando sus centroides.
     *
     * El centroide es como el "punto promedio" de un grupo.
     * Si tienes 3 puntos en (1,2), (3,4), (5,6), el centroide está en (3,4).
     */
    private double calcularDistanciaEntreClusters(NodoArbol nodo1, NodoArbol nodo2) {
        Vector centroide1 = calcularCentroide(nodo1);
        Vector centroide2 = calcularCentroide(nodo2);
        return distancia.calcular(centroide1, centroide2);
    }

    /**
     * Calcula el centroide (punto promedio) de todas las hojas bajo un nodo.
     *
     * Por ejemplo, si un cluster tiene 3 películas con ratings [5, 7, 8],
     * el centroide sería [6.67] (el promedio).
     */
    private Vector calcularCentroide(NodoArbol nodo) {
        // Si es una hoja, su centroide es su propio vector
        if (nodo.esHoja()) {
            return nodo.getDatos();
        }

        // Recolectar todas las hojas del subárbol
        Lista<Vector> hojas = new Lista<>();
        recolectarHojas(nodo, hojas);

        if (hojas.tamanio() == 0) {
            return new Vector();
        }

        // Calcular el promedio de todas las dimensiones
        int dimension = hojas.obtener(0).tamanio();
        Vector centroide = new Vector(dimension);

        for (int d = 0; d < dimension; d++) {
            double suma = 0.0;
            // Sumar todos los valores de esta dimensión
            for (int i = 0; i < hojas.tamanio(); i++) {
                suma += hojas.obtener(i).obtener(d);
            }
            // Agregar el promedio
            centroide.agregar(suma / hojas.tamanio());
        }

        return centroide;
    }

    /**
     * Recolecta recursivamente todas las hojas de un subárbol.
     * Es como recorrer todas las ramas hasta llegar a las puntas.
     */
    private void recolectarHojas(NodoArbol nodo, Lista<Vector> hojas) {
        if (nodo == null) {
            return;
        }

        if (nodo.esHoja()) {
            // Si es hoja, agregarla a la lista
            hojas.agregar(nodo.getDatos());
        } else {
            // Si no es hoja, seguir explorando sus hijos
            recolectarHojas(nodo.getIzquierdo(), hojas);
            recolectarHojas(nodo.getDerecho(), hojas);
        }
    }

    /**
     * Actualiza la matriz de distancias después de fusionar dos clusters.
     * Marca las filas/columnas de los clusters fusionados como inválidas (-1).
     */
    private void actualizarMatrizDistancias(Matriz matriz, Lista<Cluster> clusters,
                                            int i, int j, Cluster nuevoCluster) {
        // Obtener índices en la matriz
        int indiceI = clusters.obtener(i).indiceMatriz;
        int indiceJ = clusters.obtener(j).indiceMatriz;

        // Marcar fila y columna de 'i' como inválidas
        if (indiceI >= 0 && indiceI < matriz.getFilas()) {
            for (int k = 0; k < matriz.getColumnas(); k++) {
                matriz.insertar(indiceI, k, -1.0);
                matriz.insertar(k, indiceI, -1.0);
            }
        }

        // Marcar fila y columna de 'j' como inválidas
        if (indiceJ >= 0 && indiceJ < matriz.getFilas()) {
            for (int k = 0; k < matriz.getColumnas(); k++) {
                matriz.insertar(indiceJ, k, -1.0);
                matriz.insertar(k, indiceJ, -1.0);
            }
        }
    }

    /**
     * Clase interna que representa un cluster durante el proceso de agrupamiento.
     *
     * Un cluster puede ser:
     * - Un dato individual (hoja del árbol)
     * - Un grupo de datos fusionados (nodo interno del árbol)
     *
     * Mantiene información para calcular centroides eficientemente sin
     * recorrer todas las hojas cada vez.
     */
    private static class Cluster {
        NodoArbol nodo;        // Nodo del árbol que representa este cluster
        int indiceMatriz;      // Posición en la matriz de distancias

        // === Optimización para centroides ===
        // En vez de recalcular el centroide cada vez, guardamos la suma acumulada
        Vector suma;   // Suma de todos los vectores de las hojas
        int count;     // Cantidad de hojas en el cluster

        /**
         * Constructor: crea un cluster a partir de un nodo (generalmente una hoja).
         */
        Cluster(NodoArbol nodo, int indiceMatriz) {
            this.nodo = nodo;
            this.indiceMatriz = indiceMatriz;

            // Inicializar suma y contador
            if (nodo != null && nodo.getDatos() != null) {
                Vector v = nodo.getDatos();
                this.suma = new Vector(v.tamanio());
                // Copiar el vector a 'suma'
                for (int k = 0; k < v.tamanio(); k++) {
                    this.suma.agregar(v.obtener(k));
                }
                this.count = 1; // Es una hoja, así que count = 1
            } else {
                this.suma = new Vector();
                this.count = 0;
            }
        }

        /**
         * Fusiona dos clusters en uno nuevo.
         *
         * La suma del nuevo cluster = suma1 + suma2
         * El count del nuevo cluster = count1 + count2
         *
         * Esto permite calcular el centroide en O(m) sin recorrer hojas.
         */
        static Cluster fusionar(Cluster c1, Cluster c2, NodoArbol nuevoNodo, int indiceMatriz) {
            Cluster c = new Cluster(nuevoNodo, indiceMatriz);

            // Sumar componente a componente
            Vector s = new Vector(c1.suma.tamanio());
            for (int i = 0; i < c1.suma.tamanio(); i++) {
                s.agregar(c1.suma.obtener(i) + c2.suma.obtener(i));
            }

            c.suma = s;
            c.count = c1.count + c2.count;
            return c;
        }

        /**
         * Calcula el centroide del cluster en tiempo O(m).
         *
         * Centroide = suma / count
         *
         * Por ejemplo, si suma = [60, 80] y count = 3,
         * entonces centroide = [20, 26.67]
         */
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