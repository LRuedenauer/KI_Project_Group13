package tsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * This class represents an individual solution in the TSP problem.
 * An individual is a permutation of city indices representing a tour.
 */
public class Individual implements Comparable<Individual> {
    public int[] tour;        // The tour represented as a permutation of city indices
    public double fitness;    // Lower is better for TSP (the total distance)
    private static final Random random = new Random(); // Use a single Random instance

    /**
     * Creates an individual with the given tour. The fitness must be calculated separately.
     *
     * @param tour The tour represented as an array of city indices
     */
    public Individual(int[] tour) {
        // Defensive copy of the tour array to prevent external modification
        this.tour = tour.clone();
        this.fitness = Double.MAX_VALUE; // Initialize with a high value, fitness must be calculated
    }

    /**
     * Creates a random individual for a TSP instance with n cities.
     *
     * @param n The number of cities
     * @return A random individual
     */
    public static Individual random(int n) {
        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        Collections.shuffle(list, random); // Use the class-level Random instance
        int[] t = list.stream().mapToInt(Integer::intValue).toArray();
        return new Individual(t);
    }

    /**
     * Creates a greedy nearest-neighbor individual for a TSP instance.
     * This provides a good initial solution for the EA.
     *
     * @param tsp The TSP instance
     * @param startCity The index of the city to start from (or -1 for random start)
     * @return A greedy individual
     */
    public static Individual greedy(TSPInstance tsp, int startCity) {
        int numCities = tsp.getNumCities();
        int[] tour = new int[numCities];
        boolean[] visited = new boolean[numCities];

        // Choose a random start city if -1 is provided
        int currentCity = (startCity == -1) ? random.nextInt(numCities) : startCity;
        tour[0] = currentCity;
        visited[currentCity] = true;

        for (int i = 1; i < numCities; i++) {
            int nextCity = -1;
            double minDistance = Double.MAX_VALUE;

            for (int j = 0; j < numCities; j++) {
                if (!visited[j]) {
                    double dist = tsp.getDistance(currentCity, j);
                    if (dist < minDistance) {
                        minDistance = dist;
                        nextCity = j;
                    }
                }
            }
            if (nextCity != -1) {
                tour[i] = nextCity;
                visited[nextCity] = true;
                currentCity = nextCity;
            } else {
                // This case should ideally not happen if numCities > 0 and graph is connected
                // Fallback: find any unvisited city
                for (int j = 0; j < numCities; j++) {
                    if (!visited[j]) {
                        tour[i] = j;
                        visited[j] = true;
                        currentCity = j;
                        break;
                    }
                }
            }
        }
        Individual individual = new Individual(tour);
        individual.calculateFitness(tsp); // Calculate fitness immediately
        return individual;
    }

    /**
     * Calculates and sets the fitness of this individual based on the given TSP instance.
     *
     * @param tsp The TSP instance
     */
    public void calculateFitness(TSPInstance tsp) {
        this.fitness = tsp.calculateTourDistance(this.tour);
    }

    /**
     * Returns a copy of the tour array.
     *
     * @return A copy of the tour array
     */
    public int[] getTour() {
        return tour.clone(); // Return a defensive copy
    }

    /**
     * Returns a string representation of this individual.
     *
     * @return A string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tour: ");
        for (int i = 0; i < tour.length; i++) {
            sb.append(tour[i]);
            if (i < tour.length - 1) {
                sb.append(" -> ");
            }
        }
        sb.append(" -> ").append(tour[0]);  // Return to starting city
        sb.append(String.format(" (Distance: %.2f)", fitness));
        return sb.toString();
    }

    /**
     * Checks if this individual is equal to the given object.
     *
     * @param obj The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Individual that = (Individual) obj;
        // Two individuals are equal if their tours are the same permutation,
        // considering rotations and reversals for TSP tours.
        // However, for simplicity in EA, we usually compare exact permutations.
        // If a true TSP tour equality check (rotations/reversals) is needed,
        // it would be more complex (e.g., normalize the tour first).
        return Arrays.equals(tour, that.tour);
    }

    /**
     * Returns a hash code for this individual.
     *
     * @return A hash code
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(tour);
    }

    /**
     * Compares this individual with the given individual based on fitness.
     * Lower fitness is better for TSP.
     *
     * @param other The individual to compare with
     * @return A negative integer, zero, or a positive integer as this individual
     * is less than, equal to, or greater than the given individual
     */
    @Override
    public int compareTo(Individual other) {
        return Double.compare(this.fitness, other.fitness);
    }
}