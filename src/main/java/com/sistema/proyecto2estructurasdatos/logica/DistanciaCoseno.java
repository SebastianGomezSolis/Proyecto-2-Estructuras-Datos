package com.sistema.proyecto2estructurasdatos.logica;

import com.sistema.proyecto2estructurasdatos.modelo.Vector;

/**
 * Esta clase calcula la "Distancia Coseno" entre dos vectores.
 *
 * Qué es la distancia coseno?
 * Imaginemos que hay dos flechas (vectores) en el espacio. La distancia coseno
 * mide qué tan diferente es la DIRECCIÓN de esas flechas, sin importar su largo.
 *
 * Por ejemplo:
 * - Si las flechas apuntan en la misma dirección → distancia = 0 (muy similares)
 * - Si apuntan en direcciones opuestas → distancia = 2 (muy diferentes)
 * - Si forman un ángulo de 90° → distancia = 1 (neutral)
 *
 */
public class DistanciaCoseno implements IDistancia {

    /**
     * Este método calcula qué tan diferentes son dos vectores basándose
     * en el ángulo que forman entre sí.
     *
     * v1 El primer vector
     * v2 El segundo vector
     * return Un número entre 0 y 2, donde 0 = muy similares, 2 = muy diferentes
     */
    @Override
    public double calcular(Vector v1, Vector v2) {
        // VALIDACIÓN: Ambos vectores deben tener la misma cantidad de elementos
        if (v1.tamanio() != v2.tamanio()) {
            throw new IllegalArgumentException("Los vectores deben tener el mismo tamaño");
        }

        // === PASO 1: Calcular el "producto punto" ===
        // El producto punto es una operación matemática que multiplica
        // cada par de elementos y suma todo.
        double productoPunto = v1.productoPunto(v2);

        // === PASO 2: Calcular las "magnitudes" (largos) de cada vector ===
        // La magnitud es como el "largo" de la flecha.
        double magnitudV1 = v1.magnitud();
        double magnitudV2 = v2.magnitud();

        // === PASO 3: Evitar división por cero ===
        // Si algún vector tiene magnitud 0, significa que es un vector vacío [0, 0, 0]
        // No podemos calcular el ángulo con un vector vacío, así que devolvemos
        // la distancia máxima (1.0), indicando que son completamente diferentes
        if (magnitudV1 == 0 || magnitudV2 == 0) {
            return 1.0; // Máxima distancia posible
        }

        // === PASO 4: Calcular la "similitud coseno" ===
        // La similitud coseno es una medida de qué tan parecidos son los vectores
        // Esta fórmula nos da un valor entre -1 y 1:
        // - Si similitud = 1 → Los vectores apuntan exactamente en la misma dirección
        // - Si similitud = 0 → Los vectores son perpendiculares (ángulo de 90°)
        // - Si similitud = -1 → Los vectores apuntan en direcciones opuestas
        double similitudCoseno = productoPunto / (magnitudV1 * magnitudV2);

        // === PASO 5: Convertir similitud a distancia ===
        // Como queremos una "distancia" (donde 0 = igual y números grandes = diferentes),
        // convertimos la similitud usando: distancia = 1 - similitud
        //
        // Resultado final:
        // - Si similitud era 1 → distancia = 0 (idénticos)
        // - Si similitud era 0 → distancia = 1 (diferentes)
        // - Si similitud era -1 → distancia = 2 (opuestos)
        return 1.0 - similitudCoseno;
    }

    /**
     * Devuelve el nombre de este tipo de distancia.
     * Se usa para mostrar en la interfaz o para identificar qué método se está usando.
     */
    @Override
    public String getNombre() {
        return "Coseno";
    }
}