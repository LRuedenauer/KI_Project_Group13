package tsp;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for reading TSP instances from TSPLIB formatted files.
 * Supports both explicit distance matrices and Euclidean coordinate formats.
 */
public class TSPLibReader {

    /**
     * Reads a TSP instance from a file in TSPLIB format.
     *
     * @param filename The name of the file to read
     * @return A TSPInstance object representing the problem
     * @throws IOException If there's an error reading the file or parsing content
     */
    public static TSPInstance readTSPInstance(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        int dimension = -1;
        String edgeWeightType = null;
        String name = "Unknown";
        boolean readingNodeCoords = false;
        boolean readingDistMatrix = false;
        List<double[]> coordList = new ArrayList<>(); // Use a list to dynamically add coordinates
        List<List<Double>> distMatrixList = new ArrayList<>(); // Use a list of lists for matrix

        // Regex to extract value after ' : '
        Pattern headerPattern = Pattern.compile("^\\s*([A-Z_]+)\\s*:\\s*(.*)\\s*$");

        // Read the header information
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher matcher = headerPattern.matcher(line);
            if (matcher.matches()) {
                String key = matcher.group(1);
                String value = matcher.group(2);

                switch (key) {
                    case "NAME":
                        name = value;
                        break;
                    case "DIMENSION":
                        dimension = Integer.parseInt(value);
                        break;
                    case "EDGE_WEIGHT_TYPE":
                        edgeWeightType = value;
                        break;
                    // Add other header types if needed, like COMMENT, TYPE, etc.
                }
            } else if (line.equals("NODE_COORD_SECTION")) {
                readingNodeCoords = true;
                break; // Stop reading header, start reading coordinates
            } else if (line.equals("EDGE_WEIGHT_SECTION")) {
                readingDistMatrix = true;
                break; // Stop reading header, start reading distance matrix
            } else if (line.equals("EOF")) {
                break; // End of file indicator
            }
        }

        if (dimension == -1) {
            throw new IOException("DIMENSION not found or invalid in TSPLIB file: " + filename);
        }
        if (edgeWeightType == null) {
            System.err.println("WARNING: EDGE_WEIGHT_TYPE not specified in " + filename + ". Assuming EUC_2D if NODE_COORD_SECTION is present.");
        }


        if (readingNodeCoords) {
            // Read NODE_COORD_SECTION
            int cityCount = 0;
            while ((line = reader.readLine()) != null && !line.trim().equals("EOF") && !line.trim().equals("DISPLAY_DATA_SECTION")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+"); // Split by one or more spaces
                try {
                    // TSPLIB nodes are 1-indexed, so we might need to adjust if using 0-indexed arrays
                    // We discard the first part (node number) and take x, y coordinates
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    coordList.add(new double[]{x, y});
                    cityCount++;
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.err.println("Warning: Could not parse node coordinate line: " + line);
                }
            }
            if (cityCount != dimension) {
                throw new IOException("Mismatch between DIMENSION (" + dimension + ") and actual number of coordinates read (" + cityCount + ") in " + filename);
            }
        } else if (readingDistMatrix) {
            // Read EDGE_WEIGHT_SECTION
            // This section can be FULL_MATRIX, UPPER_ROW, LOWER_DIAG_ROW, etc.
            // For simplicity, we'll assume FULL_MATRIX or a format that can be parsed as a full matrix.
            // TSPLIB can be tricky here, often values are space-separated, sometimes across multiple lines.
            // We'll read all numbers and then fill the matrix.

            StringBuilder matrixData = new StringBuilder();
            while ((line = reader.readLine()) != null && !line.trim().equals("EOF") && !line.trim().equals("DISPLAY_DATA_SECTION")) {
                matrixData.append(line.trim()).append(" ");
            }

            String[] rawValues = matrixData.toString().trim().split("\\s+");
            double[][] matrix = new double[dimension][dimension];
            int valueIndex = 0;

            for (int i = 0; i < dimension; i++) {
                for (int j = 0; j < dimension; j++) {
                    if (valueIndex < rawValues.length) {
                        try {
                            matrix[i][j] = Double.parseDouble(rawValues[valueIndex]);
                        } catch (NumberFormatException e) {
                            throw new IOException("Error parsing distance matrix value: '" + rawValues[valueIndex] + "' in file " + filename);
                        }
                        valueIndex++;
                    } else {
                        throw new IOException("Not enough values in EDGE_WEIGHT_SECTION for DIMENSION " + dimension + " in file " + filename);
                    }
                }
            }
            reader.close();
            return new TSPInstance(matrix, null, name); // Return early for explicit matrix
        }

        reader.close();

        // If NODE_COORD_SECTION was read, calculate Euclidean distances
        if (!coordList.isEmpty() && "EUC_2D".equalsIgnoreCase(edgeWeightType)) {
            double[][] coordinates = coordList.toArray(new double[0][]);
            double[][] distanceMatrix = new double[dimension][dimension];
            for (int i = 0; i < dimension; i++) {
                for (int j = 0; j < dimension; j++) {
                    if (i == j) {
                        distanceMatrix[i][j] = 0;
                    } else {
                        double dx = coordinates[i][0] - coordinates[j][0];
                        double dy = coordinates[i][1] - coordinates[j][1];
                        distanceMatrix[i][j] = Math.sqrt(dx * dx + dy * dy);
                    }
                }
            }
            return new TSPInstance(distanceMatrix, coordinates, name);
        } else if (coordList.isEmpty() && !readingDistMatrix) {
            throw new IOException("Neither NODE_COORD_SECTION nor EDGE_WEIGHT_SECTION found or understood in TSPLIB file: " + filename);
        } else {
            throw new IOException("Unsupported EDGE_WEIGHT_TYPE: " + edgeWeightType + " with coordinates, or missing coordinates for EUC_2D in " + filename);
        }
    }

    /**
     * Creates a simple example TSP instance with the specified number of cities.
     * The cities are placed randomly in a 100x100 grid, and distances are Euclidean.
     *
     * @param numCities The number of cities in the TSP instance
     * @return A TSPInstance object representing the problem
     */
    public static TSPInstance createRandomInstance(int numCities) {
        Random random = new Random();
        double[][] coordinates = new double[numCities][2];

        // Generate random coordinates in a 100x100 grid
        for (int i = 0; i < numCities; i++) {
            coordinates[i][0] = random.nextDouble() * 100;
            coordinates[i][1] = random.nextDouble() * 100;
        }

        // Compute Euclidean distances
        double[][] distanceMatrix = new double[numCities][numCities];
        for (int i = 0; i < numCities; i++) {
            for (int j = 0; j < numCities; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                } else {
                    double dx = coordinates[i][0] - coordinates[j][0];
                    double dy = coordinates[i][1] - coordinates[j][1];
                    distanceMatrix[i][j] = Math.sqrt(dx * dx + dy * dy);
                }
            }
        }
        return new TSPInstance(distanceMatrix, coordinates, "Random_TSP_" + numCities);
    }
}