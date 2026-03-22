# 🌳 Dendrograma – Clasificación Jerárquica Aglomerativa

Sistema de escritorio desarrollado en Java con JavaFX para generar dendrogramas mediante el algoritmo de **clustering jerárquico aglomerativo (bottom-up)**. Carga datos desde archivos CSV, aplica normalización y cálculo de distancias, y exporta el resultado al formato JSON compatible con un visor de dendrogramas externo.

> **Curso:** Estructura de Datos  
> **Profesor:** Steven Brenes Chavarría  
> **Proyecto:** Segundo Proyecto de Programación  
> **Universidad Nacional** – Escuela de Informática

---

## 📋 Tabla de contenidos

- [Descripción general](#descripción-general)
- [Tecnologías utilizadas](#tecnologías-utilizadas)
- [Arquitectura y patrones de diseño](#arquitectura-y-patrones-de-diseño)
- [Estructuras de datos implementadas](#estructuras-de-datos-implementadas)
- [Algoritmo de clustering](#algoritmo-de-clustering)
- [Funciones de normalización](#funciones-de-normalización)
- [Métricas de distancia](#métricas-de-distancia)
- [Flujo del programa](#flujo-del-programa)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Cómo ejecutar](#cómo-ejecutar)
- [Formato del JSON exportado](#formato-del-json-exportado)

---

## Descripción general

Un **dendrograma** es un diagrama en forma de árbol que representa la relación jerárquica entre elementos según su similitud. Este sistema implementa el enfoque **aglomerativo** (bottom-up): parte de elementos individuales y los une progresivamente en clústeres hasta obtener un único grupo raíz.

El sistema permite:
- Cargar cualquier dataset en formato `.csv`.
- Detectar automáticamente columnas numéricas y cualitativas.
- Configurar por variable: si se incluye, su peso, y su método de normalización.
- Seleccionar la métrica de distancia a usar.
- Exportar el dendrograma a `.json` para visualizarlo en un visor externo.

> **Nota importante:** Todo el código fue desarrollado desde cero por el equipo. No se utilizaron librerías externas, colecciones de Java (`ArrayList`, `HashMap`, etc.) ni herramientas de IA. Todas las estructuras de datos son implementaciones propias.

---

## Tecnologías utilizadas

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 24 |
| UI | JavaFX 21 (FXML + controlador MVC) |
| Build | Maven (Maven Wrapper 3.8.5) |
| Testing | JUnit Jupiter 5.12.1 |

---

## Arquitectura y patrones de diseño

El proyecto respeta principios **SOLID** y aplica los siguientes patrones de diseño:

### MVC (Model-View-Controller)
La vista es el archivo `Ventana.fxml`, el controlador es `VentanaController`, y el modelo son los paquetes `modelo` y `logica`.

### Strategy
Las interfaces `IDistancia` e `INormalizacion` definen contratos intercambiables en tiempo de ejecución. Cada métrica de distancia y cada método de normalización es una clase independiente que implementa su interfaz correspondiente, sin que el algoritmo principal necesite saber cuál se usa.

```
IDistancia              INormalizacion
├── DistanciaEuclidiana ├── NormalizacionMinMax
├── DistanciaManhattan  ├── NormalizacionZscore
├── DistanciaCoseno     └── NormalizacionLogaritmica
└── DistanciaHamming
```

### Factory Method + Singleton
`FactoryDistancia` y `FactoryNormalizacion` son Singletons con un método `crear(nombre)` que devuelve la instancia correcta de la estrategia según el nombre elegido en la interfaz. Esto centraliza la creación y desacopla el controlador de las clases concretas.

### Iterator
La interfaz `Iterador<T>` junto con `Iterable<T>` permiten recorrer las estructuras propias (`Lista`, `Vector`) de forma genérica, sin exponer su implementación interna.

---

## Estructuras de datos implementadas

Todas las estructuras son implementaciones propias, sin usar `java.util.*`:

| Clase | Tipo | Descripción |
|---|---|---|
| `Vector` | Arreglo dinámico de `double` | Almacena los componentes numéricos de cada dato. Crece automáticamente al doble cuando se llena. Incluye `productoPunto()` y `magnitud()`. |
| `Lista<T>` | Lista enlazada simple | Colección genérica con inserción al final en O(1) usando puntero a la cola. Implementa el patrón Iterator propio. |
| `Matriz` | Arreglo bidimensional de `double` | Representa la matriz de distancias N×N entre todos los elementos. |
| `ArbolBinario` | Árbol binario | Almacena la raíz del dendrograma resultante. |
| `NodoArbol` | Nodo de árbol binario | Puede ser hoja (dato original) o nodo interno (fusión de clústeres). Guarda etiqueta, vector de datos, distancia de unión e hijos izquierdo/derecho. |
| `HashMapa<K,V>` | Tabla hash con encadenamiento | Implementación propia de un mapa llave-valor. Soporta rehash automático al superar el 75% de carga. Usada para mapeos de categorías one-hot y para IDs de hojas en el JSON. |
| `NodoSimple<T>` | Nodo de lista enlazada | Nodo genérico con dato y puntero al siguiente. |
| `NodoMapa<K,V>` | Nodo de tabla hash | Guarda llave, valor y referencia al siguiente nodo en la misma cubeta. |

---

## Algoritmo de clustering

El sistema implementa **clustering jerárquico aglomerativo con enlace simple (single-link)**:

### Pipeline completo

```
CSV (filas)
    ↓
Vectorización automática
  - Valores numéricos → se toman directamente
  - Valores cualitativos → codificación One-Hot
    ↓
Normalización por columna (Min-Max / Z-Score / Logarítmica)
    ↓
Aplicación de pesos por columna
    ↓
Matriz de distancias N×N (Euclidiana / Manhattan / Coseno / Hamming)
    ↓
Clustering aglomerativo (enlace simple)
  - Cada dato empieza como clúster individual (hoja del árbol)
  - Se fusionan iterativamente los dos más cercanos
  - Se actualiza la matriz con min(dist_a_k, dist_b_k)
  - Se repite hasta tener un único clúster raíz
    ↓
ArbolBinario (dendrograma)
    ↓
Exportación JSON
```

### Complejidad del algoritmo principal

| Operación | Complejidad |
|---|---|
| Construcción de la matriz de distancias | O(n²) |
| Inicialización del vecino más cercano | O(n²) |
| Bucle de fusiones (n-1 iteraciones) | O(n²) por iteración → O(n³) total |
| Exportación JSON (recorrido del árbol) | O(n) |

Para datasets grandes (> 1000 filas) el tiempo de ejecución puede ser considerable debido a la complejidad cúbica. La interfaz ejecuta el clustering en un hilo aparte para no bloquear la UI.

---

## Funciones de normalización

Cada columna numérica puede normalizarse independientemente:

| Método | Fórmula | Cuándo usarlo |
|---|---|---|
| **Min-Max** | `(x - min) / (max - min)` | Variables en la misma escala; escala a [0, 1] |
| **Z-Score** | `(x - μ) / σ` | Variables con distintas unidades o escalas |
| **Logarítmica** | `log(|x| + 1)` con signo conservado | Datos muy sesgados o con valores extremos |

Las columnas cualitativas se codifican automáticamente con **One-Hot** al leer el CSV, no permiten cambiar la normalización.

---

## Métricas de distancia

| Métrica | Fórmula | Uso típico |
|---|---|---|
| **Euclidiana** | `√Σ(xᵢ - yᵢ)²` | Distancia en línea recta; uso general |
| **Manhattan** | `Σ|xᵢ - yᵢ|` | Modelos lineales, datos con ruido |
| **Coseno** | `1 - (x·y) / (‖x‖·‖y‖)` | Cuando importa la dirección, no la magnitud |
| **Hamming** | `Σ(xᵢ ≠ yᵢ)` | Vectores binarios o categóricos (one-hot) |

---

## Flujo del programa

1. El usuario presiona **Cargar CSV** → se abre un selector de archivos.
2. El CSV se procesa en un hilo aparte (`Task<ResultadoCSV>`):
   - Se detectan columnas numéricas vs. cualitativas.
   - Las categóricas se codifican con One-Hot.
   - Se construyen objetos `Dato` con su `vectorOriginal`.
3. La interfaz genera dinámicamente una fila de controles por cada columna:
   - **CheckBox** para incluir/ignorar la variable.
   - **TextField** para el peso (por defecto 1.0).
   - **ComboBox** para la normalización (Min-Max / Z-Score / Logarítmica).
4. El usuario selecciona la **métrica de distancia** en el ComboBox superior.
5. Presiona **Generar JSON** → el clustering corre en segundo plano:
   - Normaliza y pondera los vectores.
   - Construye la matriz de distancias.
   - Ejecuta el algoritmo aglomerativo.
   - Serializa el `ArbolBinario` resultante a un archivo JSON temporal.
6. El botón **Descargar JSON** se habilita para guardar el archivo donde el usuario elija.
7. El JSON puede cargarse en el visor de dendrogramas externo para visualizarlo.

---

## Estructura del proyecto

```
src/
├── main/
│   ├── java/com/sistema/proyecto2estructurasdatos/
│   │   ├── Main.java                          # Punto de entrada JavaFX
│   │   ├── controller/
│   │   │   └── VentanaController.java         # Controlador MVC principal
│   │   ├── Formato/
│   │   │   ├── CSV.java                       # Lector y vectorizador de CSV
│   │   │   ├── JSON.java                      # Exportador de árbol a JSON
│   │   │   └── ResultadoCSV.java              # DTO con los datos procesados
│   │   ├── logica/
│   │   │   ├── AlgoritmoClustering.java       # Clustering aglomerativo principal
│   │   │   ├── Cluster.java                   # Representación de un clúster
│   │   │   ├── IDistancia.java                # Interfaz Strategy de distancia
│   │   │   ├── DistanciaEuclidiana.java
│   │   │   ├── DistanciaManhattan.java
│   │   │   ├── DistanciaCoseno.java
│   │   │   ├── DistanciaHamming.java
│   │   │   ├── FactoryDistancia.java          # Factory + Singleton para distancias
│   │   │   ├── INormalizacion.java            # Interfaz Strategy de normalización
│   │   │   ├── NormalizacionMinMax.java
│   │   │   ├── NormalizacionZscore.java
│   │   │   ├── NormalizacionLogaritmica.java
│   │   │   └── FactoryNormalizacion.java      # Factory + Singleton para normalizaciones
│   │   └── modelo/
│   │       ├── Vector.java                    # Arreglo dinámico de double (propio)
│   │       ├── Lista.java                     # Lista enlazada simple genérica (propia)
│   │       ├── Matriz.java                    # Matriz de distancias N×N (propia)
│   │       ├── ArbolBinario.java              # Árbol binario (propio)
│   │       ├── NodoArbol.java                 # Nodo del árbol (hoja o nodo interno)
│   │       ├── HashMapa.java                  # Tabla hash con encadenamiento (propia)
│   │       ├── NodoSimple.java                # Nodo de lista enlazada
│   │       ├── NodoMapa.java                  # Nodo de tabla hash
│   │       ├── Dato.java                      # Fila del CSV ya vectorizada
│   │       ├── Iterable.java                  # Interfaz Iterator (propia)
│   │       └── Iterador.java                  # Interfaz Iterador (propia)
│   └── resources/
│       └── com/sistema/proyecto2estructurasdatos/view/
│           └── Ventana.fxml                   # Vista principal
```

---

## Cómo ejecutar

### Prerrequisitos

- Java 24 o superior
- Maven (o usar el wrapper incluido `./mvnw`)
- JavaFX 21 (incluido como dependencia en el `pom.xml`)

### Ejecutar la aplicación

```bash
./mvnw javafx:run
```

O en Windows:

```cmd
mvnw.cmd javafx:run
```

También puedes ejecutar `Main.main()` directamente desde tu IDE.

### Ejecutar pruebas

```bash
./mvnw test
```

---

## Formato del JSON exportado

El JSON generado representa el árbol jerárquico de forma recursiva. Cada nodo tiene tres campos:

```json
{
  "n": "(1,2,3)",
  "d": 0.142857,
  "c": [
    {
      "n": "(1,2)",
      "d": 0.071428,
      "c": [
        { "n": "(1)", "d": 0.000000, "c": [] },
        { "n": "(2)", "d": 0.000000, "c": [] }
      ]
    },
    { "n": "(3)", "d": 0.000000, "c": [] }
  ]
}
```

| Campo | Descripción |
|---|---|
| `"n"` | Nombre del nodo. Para hojas: `"(id)"`. Para nodos internos: `"(id1,id2,...)"` con todos los IDs de sus hojas descendientes, ordenados. |
| `"d"` | Distancia (altura) a la que se formó este nodo. Para hojas siempre es `0.0`. |
| `"c"` | Lista de hijos (array). Para hojas es `[]`. Para nodos internos contiene exactamente dos hijos. |

Los IDs de las hojas se asignan de izquierda a derecha en orden de aparición en el árbol (recorrido in-order), empezando desde 1.

---

## Detección automática de columnas CSV

Al cargar un CSV, la clase `CSV` analiza automáticamente cada columna usando una muestra de hasta 150 filas y decide su tipo:

- **Numérica** → si el 80% o más de los valores son números válidos.
- **Cualitativa (One-Hot)** → si hay 50 o menos categorías distintas, la proporción de distintos no supera el 90%, y el largo promedio de los textos no supera 40 caracteres.
- **Texto libre (ignorada)** → si no cumple ninguna de las condiciones anteriores (por ejemplo, comentarios largos, URLs, IDs de alta cardinalidad).

Las columnas llamadas `title`, `name`, `nombre`, `id` o `original_title` se usan como **etiqueta legible** del dato y no forman parte del vector numérico.
