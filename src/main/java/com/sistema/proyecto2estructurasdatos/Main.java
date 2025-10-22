package com.sistema.proyecto2estructurasdatos;
/*
import com.sistema.proyecto2estructurasdatos.algoritmos.*;
import com.sistema.proyecto2estructurasdatos.modelo.Vector;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== PRUEBAS DE NORMALIZACIÓN ===\n");

        // Datos de prueba
        Vector datos = new Vector(new double[]{1, 2, 3, 4, 5});
        System.out.println("Datos originales: " + datos); // ✅ Ahora funciona directamente

        // Prueba Min-Max
        INormalizacion normMinMax = new NormalizacionMinMax();
        Vector resultadoMinMax = normMinMax.normalizar(datos);
        System.out.println("Min-Max: " + resultadoMinMax); // ✅ Directo
        System.out.println("  ✓ Esperado: [0.0000, 0.2500, 0.5000, 0.7500, 1.0000]");

        // Prueba Z-Score
        INormalizacion normZScore = new NormalizacionZscrore(); // ⚠️ Corregí el nombre
        Vector resultadoZScore = normZScore.normalizar(datos);
        System.out.println("\nZ-Score: " + resultadoZScore); // ✅ Directo
        System.out.println("  ✓ Media debe ser ≈0, Desv. Est. ≈1");

        // Prueba Logarítmica
        INormalizacion normLog = new NormalizacionLogaritmica();
        Vector resultadoLog = normLog.normalizar(datos);
        System.out.println("\nLogarítmica: " + resultadoLog); // ✅ Directo

        System.out.println("\n=== PRUEBAS DE DISTANCIA ===\n");

        Vector v1 = new Vector(new double[]{1, 2, 3});
        Vector v2 = new Vector(new double[]{4, 5, 6});
        System.out.println("Vector 1: " + v1); // ✅ Directo
        System.out.println("Vector 2: " + v2); // ✅ Directo

        // Prueba Euclidiana
        IDistancia distEucl = new DistanciaEuclidiana();
        double distEuclRes = distEucl.calcular(v1, v2);
        System.out.println("\nDistancia Euclidiana: " + String.format("%.6f", distEuclRes));
        System.out.println("  ✓ Esperado: ≈5.196152");

        // Prueba Manhattan
        IDistancia distManh = new DistanciaManhattan();
        double distManhRes = distManh.calcular(v1, v2);
        System.out.println("\nDistancia Manhattan: " + distManhRes);
        System.out.println("  ✓ Esperado: 9.0");

        // Prueba Coseno
        IDistancia distCos = new DistanciaCoseno();
        double distCosRes = distCos.calcular(v1, v2);
        System.out.println("\nDistancia Coseno: " + String.format("%.6f", distCosRes));
        System.out.println("  ✓ Esperado: ≈0.025368");

        // Prueba Hamming
        IDistancia distHam = new DistanciaHamming();
        double distHamRes = distHam.calcular(v1, v2);
        System.out.println("\nDistancia Hamming: " + distHamRes);
        System.out.println("  ✓ Esperado: 3.0 (todas las posiciones difieren)");

        System.out.println("\n=== PRUEBAS DE FACTORIES ===\n");

        // Prueba Factory de Normalización
        INormalizacion normFactory = FactoryNormalizacion.crear("Min-Max");
        System.out.println("Factory Normalización creó: " + normFactory.getNombre());

        // Prueba Factory de Distancia
        IDistancia distFactory = FactoryDistancia.crear("Euclidiana");
        System.out.println("Factory Distancia creó: " + distFactory.getNombre());

        System.out.println("\n✅ Todas las pruebas completadas exitosamente!");
    }
}*/


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com.sistema.sistemaprescripciondespachorecetas/view/Ventana.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Dendograma");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/com.sistema.sistemaprescripciondespachorecetas/images/Ventana.png")));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
