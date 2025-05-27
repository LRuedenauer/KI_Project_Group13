package tsp;

import java.io.*;
import java.util.*;

/**
 * Utility class for generating TSP instances in TSPLIB format.
 * This can be used to create test files for the EA.
 */
public class TSPGenerator {

    private static final Random random = new Random(); // Use a single Random instance

    /**
     * Generates a random Euclidean TSP instance with cities scattered randomly.
     * Cities are placed within a specified width and height.
     *
     * @param filename  The name of the file to create (e.g., "random_100.tsp")
     * @param numCities The number of cities to generate
     * @param width     The maximum x-coordinate for cities
     * @param height    The maximum y-coordinate for cities
     * @throws IOException If there's an error writing the file
     */
    public static void generateRandomEuclideanTSP(String filename, int numCities, double width, double height) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Write header
            writer.println("NAME : " + filename.replace(".tsp", "").toUpperCase());
            writer.println("COMMENT : Randomly generated Euclidean TSP instance");
            writer.println("TYPE : TSP");
            writer.println("DIMENSION : " + numCities);
            writer.println("EDGE_WEIGHT_TYPE : EUC_2D");
            writer.println("NODE_COORD_SECTION");

            // Generate city coordinates
            for (int i = 1; i <= numCities; i++) {
                double x = random.nextDouble() * width;
                double y = random.nextDouble() * height;
                writer.printf("%d %.6f %.6f%n", i, x, y); // Use %n for platform-independent newline
            }

            writer.println("EOF");
        }
        System.out.println("Generated random Euclidean TSP instance with " + numCities + " cities in " + filename);
    }

    /**
     * Generates a TSP instance with cities clustered in a specified number of groups.
     * This can create more challenging instances for some algorithms.
     *
     * @param filename  The name of the file to create
     * @param numClusters The number of clusters
     * @param citiesPerCluster The approximate number of cities per cluster
     * @param totalWidth The total width of the area
     * @param totalHeight The total height of the area
     * @param clusterSpread The maximum spread of cities within a cluster
     * @throws IOException If there's an error writing the file
     */
    public static void generateClusteredTSP(String filename, int numClusters, int citiesPerCluster,
                                            double totalWidth, double totalHeight, double clusterSpread) throws IOException {
        int numCities = numClusters * citiesPerCluster;
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("NAME : " + filename.replace(".tsp", "").toUpperCase());
            writer.println("COMMENT : Clustered Euclidean TSP instance");
            writer.println("TYPE : TSP");
            writer.println("DIMENSION : " + numCities);
            writer.println("EDGE_WEIGHT_TYPE : EUC_2D");
            writer.println("NODE_COORD_SECTION");

            List<double[]> clusterCenters = new ArrayList<>();
            for (int i = 0; i < numClusters; i++) {
                clusterCenters.add(new double[]{random.nextDouble() * totalWidth, random.nextDouble() * totalHeight});
            }

            int cityId = 1;
            for (int i = 0; i < numClusters; i++) {
                double[] center = clusterCenters.get(i);
                for (int j = 0; j < citiesPerCluster; j++) {
                    double x = center[0] + (random.nextDouble() - 0.5) * 2 * clusterSpread;
                    double y = center[1] + (random.nextDouble() - 0.5) * 2 * clusterSpread;
                    // Ensure coordinates stay within bounds (optional)
                    x = Math.max(0, Math.min(totalWidth, x));
                    y = Math.max(0, Math.min(totalHeight, y));
                    writer.printf("%d %.6f %.6f%n", cityId++, x, y);
                }
            }
            writer.println("EOF");
        }
        System.out.println("Generated clustered TSP instance with " + numCities + " cities in " + filename);
    }

    /**
     * Generates a TSP instance with cities arranged in a grid pattern.
     *
     * @param filename  The name of the file to create
     * @param gridX     Number of cities along the X-axis
     * @param gridY     Number of cities along the Y-axis
     * @param spacingX  Distance between cities along X-axis
     * @param spacingY  Distance between cities along Y-axis
     * @throws IOException If there's an error writing the file
     */
    public static void generateGridTSP(String filename, int gridX, int gridY, double spacingX, double spacingY) throws IOException {
        int numCities = gridX * gridY;
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("NAME : " + filename.replace(".tsp", "").toUpperCase());
            writer.println("COMMENT : Grid-patterned Euclidean TSP instance");
            writer.println("TYPE : TSP");
            writer.println("DIMENSION : " + numCities);
            writer.println("EDGE_WEIGHT_TYPE : EUC_2D");
            writer.println("NODE_COORD_SECTION");

            int cityId = 1;
            for (int i = 0; i < gridX; i++) {
                for (int j = 0; j < gridY; j++) {
                    double x = i * spacingX;
                    double y = j * spacingY;
                    writer.printf("%d %.6f %.6f%n", cityId++, x, y);
                }
            }
            writer.println("EOF");
        }
        System.out.println("Generated grid TSP instance with " + numCities + " cities in " + filename);
    }

    /**
     * Converts a given distance matrix into a TSPLIB formatted file (EXPLICIT type).
     *
     * @param filename The name of the file to create
     * @param matrix   The square distance matrix
     * @param name     The name of the instance (used in the TSPLIB file header)
     * @throws IOException If there's an error writing the file
     * @throws IllegalArgumentException If the matrix is not square
     */
    public static void convertMatrixToTSPLIB(String filename, double[][] matrix, String name) throws IOException {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0 || matrix.length != matrix[0].length) {
            throw new IllegalArgumentException("Distance matrix must be a non-empty square matrix.");
        }
        int dimension = matrix.length;

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("NAME : " + (name != null ? name.toUpperCase() : filename.replace(".tsp", "").toUpperCase()));
            writer.println("COMMENT : Explicit distance matrix TSP instance");
            writer.println("TYPE : TSP");
            writer.println("DIMENSION : " + dimension);
            writer.println("EDGE_WEIGHT_TYPE : EXPLICIT");
            writer.println("EDGE_WEIGHT_FORMAT : FULL_MATRIX"); // Assuming FULL_MATRIX for simplicity
            writer.println("EDGE_WEIGHT_SECTION");

            // Write the distance matrix
            for (int i = 0; i < dimension; i++) {
                for (int j = 0; j < dimension; j++) {
                    writer.printf("%.6f ", matrix[i][j]);
                }
                writer.println(); // New line after each row
            }
            writer.println("EOF");
        }
        System.out.println("Converted " + dimension + "x" + dimension + " distance matrix to TSPLIB format in " + filename);
    }

    /**
     * Main method to test the generator.
     */
    public static void main(String[] args) {
        try {
            // Generate some test instances
            generateRandomEuclideanTSP("test_random_50.tsp", 50, 100, 100);
            generateClusteredTSP("test_clustered_100.tsp", 5, 20, 1000, 1000, 50);
            generateGridTSP("test_grid_100.tsp", 10, 10, 100, 100);

            // Generate a small example distance matrix and convert it
            double[][] matrix = {
                    {0, 2, 9, 10},
                    {1, 0, 6, 4},
                    {15, 7, 0, 8},
                    {6, 3, 12, 0}
            };
            convertMatrixToTSPLIB("test_example_matrix.tsp", matrix, "Example_Matrix_4");

        } catch (IOException e) {
            System.err.println("Error generating TSP files: " + e.getMessage());
            e.printStackTrace();
        }
    }
}