package tsp;

/**
 * Represents a Traveling Salesperson Problem instance.
 * Stores both the distance matrix between cities and optionally their coordinates.
 */
public class TSPInstance {
    private final double[][] distanceMatrix;
    private final double[][] coordinates;
    private final String name;

    /**
     * Creates a TSP instance with the given distance matrix.
     *
     * @param distanceMatrix The distance matrix between cities
     */
    public TSPInstance(double[][] distanceMatrix) {
        this(distanceMatrix, null, "Unknown");
    }

    /**
     * Creates a TSP instance with the given distance matrix and coordinates.
     *
     * @param distanceMatrix The distance matrix between cities
     * @param coordinates The x,y coordinates of each city (can be null)
     * @param name The name of the TSP instance
     */
    public TSPInstance(double[][] distanceMatrix, double[][] coordinates, String name) {
        if (distanceMatrix == null || distanceMatrix.length == 0 || distanceMatrix[0].length == 0) {
            throw new IllegalArgumentException("Distance matrix cannot be null or empty.");
        }
        if (distanceMatrix.length != distanceMatrix[0].length) {
            throw new IllegalArgumentException("Distance matrix must be square.");
        }
        this.distanceMatrix = distanceMatrix;
        this.coordinates = coordinates; // Can be null
        this.name = name != null ? name : "Unknown"; // Ensure name is not null
    }

    /**
     * Returns the number of cities in this TSP instance.
     *
     * @return The number of cities
     */
    public int getNumCities() {
        return distanceMatrix.length;
    }

    /**
     * Returns the distance between cities i and j.
     *
     * @param i The index of the first city
     * @param j The index of the second city
     * @return The distance between the cities
     * @throws IllegalArgumentException if city indices are out of bounds
     */
    public double getDistance(int i, int j) {
        if (i < 0 || i >= getNumCities() || j < 0 || j >= getNumCities()) {
            throw new IllegalArgumentException("City indices out of bounds: i=" + i + ", j=" + j);
        }
        return distanceMatrix[i][j];
    }

    /**
     * Calculates the total distance (fitness) of a given tour.
     *
     * @param tour An array representing the sequence of cities in the tour
     * @return The total distance of the tour
     * @throws IllegalArgumentException if the tour is null or does not contain all cities
     */
    public double calculateTourDistance(int[] tour) {
        if (tour == null || tour.length != getNumCities()) {
            throw new IllegalArgumentException("Tour must contain all " + getNumCities() + " cities.");
        }

        double dist = 0;
        for (int i = 0; i < tour.length; i++) {
            int from = tour[i];
            int to = tour[(i + 1) % tour.length]; // Connect last city back to the first
            dist += getDistance(from, to);
        }
        return dist;
    }

    /**
     * Returns the coordinates of the cities if available.
     *
     * @return The coordinates of the cities, or null if not available
     */
    public double[][] getCoordinates() {
        // Return a defensive copy to prevent external modification
        if (coordinates == null) {
            return null;
        }
        double[][] copy = new double[coordinates.length][2];
        for (int i = 0; i < coordinates.length; i++) {
            System.arraycopy(coordinates[i], 0, copy[i], 0, 2);
        }
        return copy;
    }

    /**
     * Checks if coordinates are available for this TSP instance.
     *
     * @return true if coordinates are available, false otherwise
     */
    public boolean hasCoordinates() {
        return coordinates != null;
    }

    /**
     * Returns the name of this TSP instance.
     *
     * @return The name of the TSP instance
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this TSP instance.
     *
     * @return A string representation of the TSP instance
     */
    @Override
    public String toString() {
        return "TSP Instance: " + name + " (" + getNumCities() + " cities)";
    }

    /**
     * Prints the distance matrix to stdout.
     */
    public void printDistanceMatrix() {
        System.out.println("Distance Matrix for " + name + ":");
        for (int i = 0; i < getNumCities(); i++) {
            for (int j = 0; j < getNumCities(); j++) {
                System.out.printf("%7.2f ", distanceMatrix[i][j]);
            }
            System.out.println();
        }
    }
}