/*
 * IMPLEMENTACIÓN COMPLETA DE MERGESORT SECUENCIAL Y PARALELO
 * 
 * Fuentes principales:
 * - Implementación secuencial: https://www.baeldung.com/java-merge-sort
 * - Implementación paralela: https://github.com/ahmet-uyar/parallel-merge-sort
 * - Documentación ForkJoin: https://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html
 * 
 * Adaptado y comentado línea por línea en castellano para trabajo universitario
 */

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class MergeSortComparison {
    
    public static void main(String[] args) {
        // Crear arrays de diferentes tamaños para las pruebas
        int[] tamanios = {10000, 50000, 100000, 500000, 1000000};
        
        System.out.println("=== COMPARACION MERGESORT SECUENCIAL VS CONCURRENTE ===\n");
        System.out.printf("%-12s %-15s %-15s %-10s\n", "Tamaño", "Secuencial(ms)", "Concurrente(ms)", "Speedup");
        System.out.println("-------------------------------------------------------");

        // Mostrar información del sistema
        int availableCores = Runtime.getRuntime().availableProcessors();
        System.out.println("Procesadores disponibles: " + availableCores);
        System.out.println("------------------------------------------------------------------------");
        
        // Probar cada tamaño de array
        for (int tam : tamanios) {
            // Generar array aleatorio para la prueba
            int[] originalArray = generateRandomArray(tam);
            
            // Crear copias para cada algoritmo
            int[] arraySecuencial = Arrays.copyOf(originalArray, originalArray.length);
            int[] arrayConcurrente = Arrays.copyOf(originalArray, originalArray.length);
            
            // Medir tiempo del algoritmo secuencial
            long startTime = System.nanoTime();
            mergeSortSecuencial(arraySecuencial);
            long tiempoSecuencial = (System.nanoTime() - startTime) / 1_000_000;

            //Medir tiempo del algoritmo concurrente
            startTime = System.nanoTime();
            mergeSortConcurrente(arrayConcurrente);
            long tiempoConcurrente = (System.nanoTime() - startTime) / 1_000_000;
            
            // Calcular speedup (mejora de velocidad)
            double speedup = (double) tiempoSecuencial / tiempoConcurrente;
            
            // Mostrar resultados
            System.out.printf("%-12d %-15d %-15d %-10.2fx\n", 
                            tam, tiempoSecuencial, tiempoConcurrente, speedup);
            
            // Verificar que ambos arrays están correctamente ordenados
            if (!Arrays.equals(arraySecuencial, arrayConcurrente)) {
                System.err.println("ERROR: Los arrays no coinciden para tamaño " + tam);
            }
        }
        
        // Ejecutar pruebas adicionales con diferentes tipos de arrays
        MergeSortTesting.testDifferentArrayTypes();
    }
    
    /**
     * IMPLEMENTACIÓN SECUENCIAL DE MERGESORT
     * Fuente: https://www.baeldung.com/java-merge-sort
     * Adaptada con comentarios en castellano
     */
    public static void mergeSortSecuencial(int[] array) {
        // Verificar si el array tiene más de un elemento
        if (array.length < 2) {
            return; // Array ya está ordenado (caso base)
        }
        
        // Calcular el punto medio del array
        int medio = array.length / 2;
        
        // Crear sub-array izquierdo desde el inicio hasta el medio
        int[] izq = new int[medio];
        
        // Crear sub-array derecho desde el medio hasta el final
        int[] der = new int[array.length - medio];
        
        // Copiar elementos del array original al sub-array izquierdo
        for (int i = 0; i < medio; i++) {
            izq[i] = array[i];
        }
        
        // Copiar elementos del array original al sub-array derecho
        for (int i = medio; i < array.length; i++) {
            der[i - medio] = array[i];
        }
        
        // Llamada recursiva para ordenar el sub-array izquierdo
        mergeSortSecuencial(izq);
        
        // Llamada recursiva para ordenar el sub-array derecho
        mergeSortSecuencial(der);
        
        // Fusionar los dos sub-arrays ordenados en el array original
        merge(array, izq, der);
    }
    
    /**
     * MÉTODO DE FUSIÓN (MERGE) PARA COMBINAR DOS ARRAYS ORDENADOS
     * Fuente: https://www.baeldung.com/java-merge-sort
     */
    private static void merge(int[] result, int[] izq, int[] der) {
        int i = 0; // Índice para recorrer el array izquierdo
        int j = 0; // Índice para recorrer el array derecho
        int k = 0; // Índice para recorrer el array resultado
        
        // Comparar elementos de ambos arrays y copiar el menor al resultado
        while (i < izq.length && j < der.length) {
            if (izq[i] <= der[j]) {
                result[k++] = izq[i++]; // Copiar elemento del array izquierdo
            } else {
                result[k++] = der[j++]; // Copiar elemento del array derecho
            }
        }
        
        // Copiar elementos restantes del array izquierdo (si los hay)
        while (i < izq.length) {
            result[k++] = izq[i++];
        }
        
        // Copiar elementos restantes del array derecho (si los hay)
        while (j < der.length) {
            result[k++] = der[j++];
        }
    }
    
        /**
     * IMPLEMENTACIÓN 100% CONCURRENTE DE MERGESORT USANDO FORKJOIN
     * MODIFICADO: Sin THRESHOLD - concurrencia completa hasta el caso base
     * Fuente: https://github.com/ahmet-uyar/parallel-merge-sort
     * Adaptada con comentarios en castellano
     */
    public static int mergeSortConcurrente(int[] array) {
        // Crear un pool de threads ForkJoin con todos los procesadores disponibles
        // Usar try-with-resources para garantizar que el pool se cierre automáticamente
        try (ForkJoinPool pool = new ForkJoinPool()) {
            // Crear la tarea principal y ejecutarla
            MergeSortTask mainTask = new MergeSortTask(array, 0, array.length - 1);
            pool.invoke(mainTask);
            // Retornar el número aproximado de threads utilizados
            return pool.getPoolSize();
        } // El pool se cierra automáticamente aquí, incluso si ocurre una excepción
    }
    
    /**
     * CLASE PARA TAREA RECURSIVA DE MERGESORT 100% CONCURRENTE
     * Extiende RecursiveTask para poder ser ejecutada en ForkJoinPool
     * MODIFICADO: Sin umbral - siempre concurrente hasta caso base
     * Fuente: https://github.com/ahmet-uyar/parallel-merge-sort/blob/master/MergeSortWithForkJoinSTM2.java
     */
    static class MergeSortTask extends RecursiveTask<Void> {
        private final int[] array;      // Array a ordenar
        private final int inicio;       // Índice de inicio del segmento
        private final int fin;          // Índice de fin del segmento
        
        // Constructor de la tarea
        public MergeSortTask(int[] array, int inicio, int fin) {
            this.array = array;        // Asignar referencia al array
            this.inicio = inicio;        // Asignar índice de inicio
            this.fin = fin;           // Asignar índice de fin
        }
        
        @Override
        protected Void compute() {
            // CASO BASE: Si el segmento tiene 1 elemento o menos, ya está ordenado
            if (inicio >= fin) {
                return null; // No hay nada que ordenar
            }

            // CASO BASE: Si el segmento tiene exactamente 2 elementos
            if (fin - inicio == 1) {
                // Comparar y intercambiar si es necesario
                if (array[inicio] > array[fin]) {
                    int temp = array[inicio];
                    array[inicio] = array[fin];
                    array[fin] = temp;
                }
                return null;
            }

            // CASO RECURSIVO: Dividir el segmento en dos mitades
            // Calcular punto medio del segmento
            int medio = inicio + (fin - inicio) / 2;
            
            // Crear tarea para la mitad izquierda del segmento
            MergeSortTask tareaIzq = new MergeSortTask(array, inicio, medio);
            
            // Crear tarea para la mitad derecha del segmento
            MergeSortTask tareaDer = new MergeSortTask(array, medio + 1, fin);
            
            // CONCURRENCIA COMPLETA:
            // Ejecutar la tarea izquierda de forma asíncrona (fork)
            tareaIzq.fork();
            
            // Ejecutar la tarea derecha en el thread actual
            tareaDer.compute();
            
            // Esperar a que termine la tarea izquierda (join)
            tareaIzq.join();
            
            // Fusionar las dos mitades ya ordenadas
            mergeInPlace(array, inicio, medio, fin);
            
            return null;
        }
    }
    
        /**
     * MÉTODO DE FUSIÓN IN-PLACE PARA IMPLEMENTACIÓN CONCURRENTE
     * Fuente: Adaptado de https://github.com/ahmet-uyar/parallel-merge-sort
     */
    private static void mergeInPlace(int[] array, int inicio, int medio, int fin) {
        // Crear array temporal para la mitad izquierda
        int[] arrayIzq = Arrays.copyOfRange(array, inicio, medio + 1);
        
        // Crear array temporal para la mitad derecha  
        int[] arrayDer = Arrays.copyOfRange(array, medio + 1, fin + 1);
        
        int i = 0;          // Índice para recorrer array izquierdo
        int j = 0;          // Índice para recorrer array derecho
        int k = inicio;      // Índice para recorrer array original
        
        // Fusionar elementos comparando el menor de cada array
        while (i < arrayIzq.length && j < arrayDer.length) {
            if (arrayIzq[i] <= arrayDer[j]) {
                array[k++] = arrayIzq[i++];    // Tomar elemento del array izquierdo
            } else {
                array[k++] = arrayDer[j++];   // Tomar elemento del array derecho
            }
        }
        
        // Copiar elementos restantes del array izquierdo
        while (i < arrayIzq.length) {
            array[k++] = arrayIzq[i++];
        }
        
        // Copiar elementos restantes del array derecho
        while (j < arrayDer.length) {
            array[k++] = arrayDer[j++];
        }
    }
    
    /**
     * MÉTODO AUXILIAR PARA GENERAR ARRAYS ALEATORIOS
     * Para pruebas de rendimiento
     */
    private static int[] generateRandomArray(int tam) {
        Random random = new Random(42); // Seed fijo para reproducibilidad
        int[] array = new int[tam];
        
        // Llenar array con números aleatorios entre -50000 y 50000
        for (int i = 0; i < tam; i++) {
            array[i] = random.nextInt(100000) - 50000;
        }
        
        return array;
    }
    
    /**
     * MÉTODO AUXILIAR PARA VERIFICAR SI UN ARRAY ESTÁ ORDENADO
     * Útil para testing y validación
     */
    public static boolean estaOrdenado(int[] array) {
        // Recorrer array verificando que cada elemento sea <= al siguiente
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i] > array[i + 1]) {
                return false; // Array no está ordenado
            }
        }
        return true; // Array está correctamente ordenado
    }
}

/*
 * CLASE ADICIONAL PARA PRUEBAS DETALLADAS
 * Incluye diferentes tipos de arrays y mediciones más precisas
 */
class MergeSortTesting {
    
    /**
     * MÉTODO PARA PROBAR DIFERENTES TIPOS DE ARRAYS
     */
    public static void testDifferentArrayTypes() {
        // PRUEBA 1: Arrays pequeños (donde el overhead domina)
        int smallTam = 10000;
        System.out.println("\n=== ARRAYS PEQUEÑOS - Overhead de concurrencia visible ===");
        System.out.printf("Tamaño: %d elementos\n", smallTam);
        System.out.printf("%-20s %-15s %-15s %-10s\n", "Tipo Array", "Secuencial(ms)", "Concurrente(ms)", "Speedup");
        System.out.println("----------------------------------------------------------");
        
        testArrayType("Aleatorio", generateRandomArray(smallTam), smallTam);
        testArrayType("Ordenado", generateSortedArray(smallTam), smallTam);
        testArrayType("Invertido", generateReversedArray(smallTam), smallTam);
        testArrayType("Parcial. Ordenado", generatePartiallySortedArray(smallTam), smallTam);
        
        System.out.println("ANALISIS: Speedup < 1.0x indica que el overhead de threads");
        System.out.println("es mayor que el beneficio de la concurrencia.");
        
        // PRUEBA 2: Arrays grandes (donde la concurrencia beneficia)
        int largeTam = 500000;
        System.out.println("\n=== ARRAYS GRANDES - Beneficio de la concurrencia visible ===");
        System.out.printf("Tamaño: %d elementos\n", largeTam);
        System.out.printf("%-20s %-15s %-15s %-10s\n", "Tipo Array", "Secuencial(ms)", "Concurrente(ms)", "Speedup");
        System.out.println("----------------------------------------------------------");
        
        testArrayType("Aleatorio", generateRandomArray(largeTam), largeTam);
        testArrayType("Ordenado", generateSortedArray(largeTam), largeTam);
        testArrayType("Invertido", generateReversedArray(largeTam), largeTam);
        testArrayType("Parcial. Ordenado", generatePartiallySortedArray(largeTam), largeTam);
        
        System.out.println("ANALISIS: Speedup > 1.0x indica que el beneficio de la concurrencia");
        System.out.println("supera el overhead de creación y gestión de threads.");
        
        // PRUEBA 3: Punto de equilibrio
        System.out.println("\n=== ANALISIS DE PUNTO DE EQUILIBRIO ===");
        findBreakevenPoint();
    }
    
    /**
     * MÉTODO PARA ENCONTRAR EL PUNTO DE EQUILIBRIO
     * Donde la concurrencia comienza a ser beneficiosa
     */
    private static void findBreakevenPoint() {
        int[] testTam = {1000, 5000, 25000, 50000, 100000, 250000};
        
        System.out.printf("%-12s %-15s %-15s %-10s %-15s\n", 
                         "Tamaño", "Secuencial(ms)", "Concurrente(ms)", "Speedup", "¿Beneficioso?");
        System.out.println("-----------------------------------------------------------------------");
        
        for (int tam : testTam) {
            int[] originalArray = generateRandomArray(tam);
            int[] arraySeq = Arrays.copyOf(originalArray, originalArray.length);
            int[] arrayPar = Arrays.copyOf(originalArray, originalArray.length);
            
            // Medir secuencial
            long startTime = System.nanoTime();
            MergeSortComparison.mergeSortSecuencial(arraySeq);
            long timeSeq = (System.nanoTime() - startTime) / 1_000_000;
            
            // Medir concurrente
            startTime = System.nanoTime();
            MergeSortComparison.mergeSortConcurrente(arrayPar);
            long timePar = (System.nanoTime() - startTime) / 1_000_000;
            
            double speedup = (double) timeSeq / timePar;
            String beneficial = speedup > 1.0 ? "SI" : "NO";
            
            System.out.printf("%-12d %-15d %-15d %-10.2fx %-15s\n", 
                             tam, timeSeq, timePar, speedup, beneficial);
        }
        
        System.out.println("\nCONCLUSION: El punto de equilibrio muestra desde qué tamaño");
        System.out.println("de array la concurrencia comienza a ser beneficioso.");
    }
    
    // Generar array aleatorio
    private static int[] generateRandomArray(int tam) {
        Random random = new Random(42);
        int[] array = new int[tam];
        for (int i = 0; i < tam; i++) {
            array[i] = random.nextInt(100000);
        }
        return array;
    }
    
    // Generar array ordenado
    private static int[] generateSortedArray(int tam) {
        int[] array = new int[tam];
        for (int i = 0; i < tam; i++) {
            array[i] = i;
        }
        return array;
    }
    
    // Generar array en orden inverso
    private static int[] generateReversedArray(int tam) {
        int[] array = new int[tam];
        for (int i = 0; i < tam; i++) {
            array[i] = tam - i;
        }
        return array;
    }
    
    // Generar array parcialmente ordenado
    private static int[] generatePartiallySortedArray(int tam) {
        int[] array = generateSortedArray(tam);
        Random random = new Random(42);
        
        // Desordenar 20% de los elementos
        for (int i = 0; i < tam * 0.2; i++) {
            int pos1 = random.nextInt(tam);
            int pos2 = random.nextInt(tam);
            
            // Intercambiar elementos
            int temp = array[pos1];
            array[pos1] = array[pos2];
            array[pos2] = temp;
        }
        return array;
    }

    // Probar un tipo específico de array
    private static void testArrayType(String type, int[] originalArray, int tam) {
        // Crear copias para cada algoritmo
        int[] arraySeq = Arrays.copyOf(originalArray, originalArray.length);
        int[] arrayPar = Arrays.copyOf(originalArray, originalArray.length);
        
        // Medir algoritmo secuencial
        long startTime = System.nanoTime();
        MergeSortComparison.mergeSortSecuencial(arraySeq);
        long timeSeq = (System.nanoTime() - startTime) / 1_000_000; // Convertir a ms
        
        // Medir algoritmo 100% concurrente
        startTime = System.nanoTime();
        MergeSortComparison.mergeSortConcurrente(arrayPar);
        long timePar = (System.nanoTime() - startTime) / 1_000_000; // Convertir a ms
        
        // Calcular speedup
        double speedup = (double) timeSeq / timePar;
        
        // Mostrar resultados
        System.out.printf("%-20s %-15d %-15d %-10.2fx\n", type, timeSeq, timePar, speedup);
        
        // Verificar que ambos arrays están ordenados
        if (!MergeSortComparison.estaOrdenado(arraySeq) || !MergeSortComparison.estaOrdenado(arrayPar)) {
            System.err.println("ERROR: Array no está correctamente ordenado para tipo " + type);
        }
    }
}
