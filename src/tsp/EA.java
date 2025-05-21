package tsp;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements an Evolutionary Algorithm (EA) for the Traveling Salesperson Problem.
 * It supports various selection, crossover, and mutation operators with configurable parameters.
 */
public class EA {
    // Crossover types
    public static final int CROSSOVER_OX = 0;  // Order Crossover
    public static final int CROSSOVER_PMX = 1; // Partially Mapped Crossover
    public static final int CROSSOVER_ERX = 2; // Edge Recombination Crossover

    // Mutation types
    public static final int MUTATION_SWAP = 0;   // Swap Mutation
    public static final int MUTATION_INSERT = 1; // Insert Mutation
    public static final int MUTATION_INVERT = 2; // Inversion Mutation

    // Selection types
    public static final int SELECTION_TOURNAMENT = 0; // Tournament Selection
    public static final int SELECTION_ROULETTE = 1;   // Roulette Wheel Selection

    private static final Random random = new Random();
    private static TSPInstance currentTSPInstance; // Store the TSP instance for fitness calculation

    /**
     * Runs the evolutionary algorithm on the given TSP instance.
     *
     * @param tsp The TSP instance
     * @param mu The parent population size (μ)
     * @param lambda The offspring population size (λ)
     * @param generations The number of generations to run
     * @param mutationRate The probability of an individual undergoing mutation (0.0-1.0)
     * @param crossoverType The type of crossover operator to use
     * @param mutationType The type of mutation operator to use
     * @param parentSelectionType The type of parent selection to use
     * @param tournamentSize The tournament size for tournament selection
     * @param useGreedyInit Whether to initialize part of the population with a greedy algorithm
     * @param verbose Whether to print detailed progress information
     * @return The best individual found during the evolutionary process
     */
    public static Individual run(TSPInstance tsp, int mu, int lambda, int generations, double mutationRate,
                                 int crossoverType, int mutationType, int parentSelectionType,
                                 int tournamentSize, boolean useGreedyInit, boolean verbose) {

        currentTSPInstance = tsp; // Set the current TSP instance for fitness calculations

        // 1. Initialization
        List<Individual> population = initializePopulation(mu, tsp.getNumCities(), useGreedyInit, tsp);
        population.forEach(ind -> ind.calculateFitness(tsp)); // Calculate initial fitness
        Collections.sort(population); // Sort to find best initial individual

        Individual overallBest = population.get(0); // Best individual across all generations

        if (verbose) {
            System.out.println("--- EA Run Started for " + tsp.getName() + " ---");
            System.out.printf("μ: %d, λ: %d, Generations: %d, Mutation Rate: %.2f%n", mu, lambda, generations, mutationRate);
            System.out.println("Crossover: " + getCrossoverName(crossoverType) + ", Mutation: " + getMutationName(mutationType) + ", Selection: " + getSelectionName(parentSelectionType));
            if (parentSelectionType == SELECTION_TOURNAMENT) {
                System.out.println("Tournament Size: " + tournamentSize);
            }
            System.out.println("Initial Population Average Fitness: " + String.format("%.2f", averageFitness(population)));
            System.out.println("Initial Best Fitness: " + String.format("%.2f", overallBest.fitness));
        }


        // Main Evolutionary Loop
        for (int gen = 0; gen < generations; gen++) {
            // 2. Parent Selection
            List<Individual> parents = selectParents(population, lambda, parentSelectionType, tournamentSize);

            // 3. Recombination (Crossover)
            List<Individual> offspring = new ArrayList<>(lambda);
            for (int i = 0; i < lambda / 2; i++) { // Generate lambda offspring pairs
                Individual parent1 = parents.get(random.nextInt(parents.size()));
                Individual parent2 = parents.get(random.nextInt(parents.size()));

                Individual[] children = crossover(parent1, parent2, crossoverType);
                offspring.add(children[0]);
                if (offspring.size() < lambda) { // Ensure we don't exceed lambda
                    offspring.add(children[1]);
                }
            }

            // 4. Mutation
            for (Individual ind : offspring) {
                if (random.nextDouble() < mutationRate) {
                    mutate(ind, mutationType);
                }
            }

            // Calculate fitness for offspring
            offspring.forEach(ind -> ind.calculateFitness(tsp));

            // 5. Survival Selection ((μ + λ) selection)
            List<Individual> nextPopulation = new ArrayList<>(mu + lambda);
            nextPopulation.addAll(population); // Add parents
            nextPopulation.addAll(offspring);  // Add offspring

            // Sort by fitness (lower is better) and select the best mu individuals
            Collections.sort(nextPopulation);
            population = nextPopulation.subList(0, mu); // Keep only the best mu individuals

            // Update overall best individual
            if (population.get(0).fitness < overallBest.fitness) {
                overallBest = new Individual(population.get(0).tour); // Deep copy
                overallBest.fitness = population.get(0).fitness;
            }

            if (verbose && (gen % (generations / 10) == 0 || gen == generations - 1)) { // Print progress
                System.out.printf("Generation %d: Best Fitness = %.2f, Average Fitness = %.2f%n",
                        gen, population.get(0).fitness, averageFitness(population));
            }
        }

        if (verbose) {
            System.out.println("--- EA Run Finished ---");
            System.out.println("Overall Best Tour Found: " + overallBest.toString());
        }

        return overallBest;
    }

    /**
     * Initializes the population.
     */
    private static List<Individual> initializePopulation(int popSize, int numCities, boolean useGreedyInit, TSPInstance tsp) {
        List<Individual> population = new ArrayList<>(popSize);
        int greedyCount = useGreedyInit ? Math.min(popSize, 1) : 0; // Use at least 1 greedy if enabled

        // Add greedy individuals
        for (int i = 0; i < greedyCount; i++) {
            population.add(Individual.greedy(tsp, -1)); // Random start city for greedy
        }

        // Fill the rest with random individuals
        while (population.size() < popSize) {
            population.add(Individual.random(numCities));
        }
        return population;
    }

    /**
     * Selects parents for crossover based on the specified selection type.
     */
    private static List<Individual> selectParents(List<Individual> population, int numParents, int selectionType, int tournamentSize) {
        List<Individual> parents = new ArrayList<>(numParents);
        switch (selectionType) {
            case SELECTION_TOURNAMENT:
                for (int i = 0; i < numParents; i++) {
                    parents.add(tournamentSelection(population, tournamentSize));
                }
                break;
            case SELECTION_ROULETTE:
                for (int i = 0; i < numParents; i++) {
                    parents.add(rouletteWheelSelection(population));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown selection type: " + selectionType);
        }
        return parents;
    }

    /**
     * Tournament Selection.
     * Selects `tournamentSize` individuals randomly and returns the best one.
     */
    private static Individual tournamentSelection(List<Individual> population, int tournamentSize) {
        Individual best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Individual candidate = population.get(random.nextInt(population.size()));
            if (best == null || candidate.fitness < best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Roulette Wheel Selection.
     * Individuals with lower fitness (better) have a higher probability of being selected.
     * This requires converting fitness to a 'goodness' score for selection probability.
     */
    private static Individual rouletteWheelSelection(List<Individual> population) {
        // Find the maximum fitness to normalize and invert for selection probability
        double maxFitness = population.stream().mapToDouble(ind -> ind.fitness).max().orElse(1.0); // Avoid division by zero

        double[] inverseFitnesses = new double[population.size()];
        double totalInverseFitness = 0;

        for (int i = 0; i < population.size(); i++) {
            // For minimization problem, better fitness (lower value) should have higher probability.
            // A common way is maxFitness - currentFitness + small_constant (to avoid zero for best)
            inverseFitnesses[i] = maxFitness - population.get(i).fitness + 1.0; // Adding 1.0 to avoid zero probability
            totalInverseFitness += inverseFitnesses[i];
        }

        if (totalInverseFitness == 0) { // All individuals have the same (worst) fitness
            return population.get(random.nextInt(population.size()));
        }

        double r = random.nextDouble() * totalInverseFitness; // Random point on the wheel
        double sum = 0;
        for (int i = 0; i < population.size(); i++) {
            sum += inverseFitnesses[i];
            if (r <= sum) {
                return population.get(i);
            }
        }

        // Fallback (should not happen with valid input and non-zero totalInverseFitness)
        return population.get(random.nextInt(population.size()));
    }


    /**
     * Applies a crossover operator to two parents to produce two offspring.
     *
     * @param parent1 The first parent
     * @param parent2 The second parent
     * @param crossoverType The type of crossover to perform
     * @return An array containing two offspring individuals
     */
    private static Individual[] crossover(Individual parent1, Individual parent2, int crossoverType) {
        switch (crossoverType) {
            case CROSSOVER_OX:
                return orderCrossover(parent1, parent2);
            case CROSSOVER_PMX:
                return partiallyMappedCrossover(parent1, parent2);
            case CROSSOVER_ERX:
                return edgeRecombinationCrossover(parent1, parent2);
            default:
                throw new IllegalArgumentException("Unknown crossover type: " + crossoverType);
        }
    }

    /**
     * Order Crossover (OX) operator.
     * Selects a random subsegment from parent1 and preserves the relative order of the remaining cities from parent2.
     *
     * @param parent1 The first parent
     * @param parent2 The second parent
     * @return An array containing two offspring individuals
     */
    private static Individual[] orderCrossover(Individual parent1, Individual parent2) {
        int n = parent1.tour.length;
        int[] child1Tour = new int[n];
        int[] child2Tour = new int[n];
        Arrays.fill(child1Tour, -1); // Mark as unassigned
        Arrays.fill(child2Tour, -1);

        // Choose two random cut points
        int cutPoint1 = random.nextInt(n);
        int cutPoint2 = random.nextInt(n);

        if (cutPoint1 > cutPoint2) {
            int temp = cutPoint1;
            cutPoint1 = cutPoint2;
            cutPoint2 = temp;
        }
        if (cutPoint1 == cutPoint2) { // Ensure at least one city in segment
            if (cutPoint1 == n - 1) cutPoint1--; else cutPoint2++;
            if (cutPoint1 > cutPoint2) { // Re-swap if necessary
                int temp = cutPoint1;
                cutPoint1 = cutPoint2;
                cutPoint2 = temp;
            }
        }


        // Copy segment from parent1 to child1
        System.arraycopy(parent1.tour, cutPoint1, child1Tour, cutPoint1, cutPoint2 - cutPoint1 + 1);
        // Copy segment from parent2 to child2
        System.arraycopy(parent2.tour, cutPoint1, child2Tour, cutPoint1, cutPoint2 - cutPoint1 + 1);


        // Fill remaining positions for child1 from parent2
        int currentChild1Idx = (cutPoint2 + 1) % n;
        for (int i = 0; i < n; i++) {
            int city = parent2.tour[(cutPoint2 + 1 + i) % n]; // Start after cutPoint2 in parent2, wrap around
            if (!contains(child1Tour, city)) {
                child1Tour[currentChild1Idx] = city;
                currentChild1Idx = (currentChild1Idx + 1) % n;
            }
        }

        // Fill remaining positions for child2 from parent1
        int currentChild2Idx = (cutPoint2 + 1) % n;
        for (int i = 0; i < n; i++) {
            int city = parent1.tour[(cutPoint2 + 1 + i) % n]; // Start after cutPoint2 in parent1, wrap around
            if (!contains(child2Tour, city)) {
                child2Tour[currentChild2Idx] = city;
                currentChild2Idx = (currentChild2Idx + 1) % n;
            }
        }

        return new Individual[]{new Individual(child1Tour), new Individual(child2Tour)};
    }


    /**
     * Partially Mapped Crossover (PMX) operator.
     * Ensures that offspring inherit absolute positions of a segment and relative order elsewhere.
     *
     * @param parent1 The first parent
     * @param parent2 The second parent
     * @return An array containing two offspring individuals
     */
    private static Individual[] partiallyMappedCrossover(Individual parent1, Individual parent2) {
        int n = parent1.tour.length;
        int[] child1Tour = new int[n];
        int[] child2Tour = new int[n];
        Arrays.fill(child1Tour, -1);
        Arrays.fill(child2Tour, -1);

        // Choose two random cut points
        int cutPoint1 = random.nextInt(n);
        int cutPoint2 = random.nextInt(n);

        if (cutPoint1 > cutPoint2) {
            int temp = cutPoint1;
            cutPoint1 = cutPoint2;
            cutPoint2 = temp;
        }
        if (cutPoint1 == cutPoint2) { // Ensure segment is at least 1 city
            if (cutPoint1 == n - 1) cutPoint1--; else cutPoint2++;
            if (cutPoint1 > cutPoint2) {
                int temp = cutPoint1;
                cutPoint1 = cutPoint2;
                cutPoint2 = temp;
            }
        }

        // 1. Copy segments
        for (int i = cutPoint1; i <= cutPoint2; i++) {
            child1Tour[i] = parent1.tour[i];
            child2Tour[i] = parent2.tour[i];
        }

        // 2. Map and fill remaining positions
        Map<Integer, Integer> mapping1 = new HashMap<>(); // From city in parent2 segment to city in parent1 segment
        Map<Integer, Integer> mapping2 = new HashMap<>(); // From city in parent1 segment to city in parent2 segment

        for (int i = cutPoint1; i <= cutPoint2; i++) {
            mapping1.put(parent2.tour[i], parent1.tour[i]);
            mapping2.put(parent1.tour[i], parent2.tour[i]);
        }

        // Fill child1
        for (int i = 0; i < n; i++) {
            if (i >= cutPoint1 && i <= cutPoint2) continue; // Segment already copied

            int city = parent2.tour[i];
            while (mapping1.containsKey(city)) {
                city = mapping1.get(city);
            }
            child1Tour[i] = city;
        }

        // Fill child2
        for (int i = 0; i < n; i++) {
            if (i >= cutPoint1 && i <= cutPoint2) continue; // Segment already copied

            int city = parent1.tour[i];
            while (mapping2.containsKey(city)) {
                city = mapping2.get(city);
            }
            child2Tour[i] = city;
        }

        return new Individual[]{new Individual(child1Tour), new Individual(child2Tour)};
    }


    /**
     * Edge Recombination Crossover (ERX) operator.
     * Attempts to preserve edges from parents. More complex to implement correctly.
     * This implementation will be a simplified version focusing on common edges.
     * For a full, robust ERX, neighbor lists are key.
     *
     * @param parent1 The first parent
     * @param parent2 The second parent
     * @return An array containing two offspring individuals
     */
    private static Individual[] edgeRecombinationCrossover(Individual parent1, Individual parent2) {
        int n = parent1.tour.length;
        int[] childTour = new int[n];
        boolean[] visited = new boolean[n];
        Arrays.fill(childTour, -1);

        // Build adjacency lists (neighbor lists) for both parents
        // Key: city index, Value: set of neighbor cities
        Map<Integer, Set<Integer>> adjacencyList = new HashMap<>();
        for (int i = 0; i < n; i++) {
            adjacencyList.put(i, new HashSet<>());
        }

        // Add edges from parent1
        for (int i = 0; i < n; i++) {
            int city1 = parent1.tour[i];
            int city2 = parent1.tour[(i + 1) % n];
            adjacencyList.get(city1).add(city2);
            adjacencyList.get(city2).add(city1); // TSP tours are undirected edges
        }

        // Add edges from parent2
        for (int i = 0; i < n; i++) {
            int city1 = parent2.tour[i];
            int city2 = parent2.tour[(i + 1) % n];
            adjacencyList.get(city1).add(city2);
            adjacencyList.get(city2).add(city1);
        }

        // Start with a random city from either parent's segment or any city
        int currentCity = parent1.tour[random.nextInt(n)]; // Start with a random city from P1
        childTour[0] = currentCity;
        visited[currentCity] = true;

        for (int i = 1; i < n; i++) {
            Set<Integer> neighbors = adjacencyList.get(currentCity);
            int nextCity = -1;

            // Remove visited cities from neighbors
            neighbors.removeIf(visitedCity -> visited[visitedCity]);

            if (!neighbors.isEmpty()) {
                // Find the neighbor with the fewest unvisited neighbors itself (heuristic)
                int minNeighborDegree = Integer.MAX_VALUE;
                List<Integer> candidates = new ArrayList<>();

                for (int neighbor : neighbors) {
                    Set<Integer> nbrsOfNeighbor = new HashSet<>(adjacencyList.get(neighbor));
                    nbrsOfNeighbor.removeIf(visitedCity -> visited[visitedCity]);
                    int degree = nbrsOfNeighbor.size();

                    if (degree < minNeighborDegree) {
                        minNeighborDegree = degree;
                        candidates.clear();
                        candidates.add(neighbor);
                    } else if (degree == minNeighborDegree) {
                        candidates.add(neighbor);
                    }
                }
                // Pick one randomly if multiple candidates have the same minimum degree
                nextCity = candidates.get(random.nextInt(candidates.size()));
            }

            if (nextCity == -1) {
                // If no unvisited neighbors or all paths lead to visited cities, pick a random unvisited city
                List<Integer> unvisitedCities = new ArrayList<>();
                for (int j = 0; j < n; j++) {
                    if (!visited[j]) {
                        unvisitedCities.add(j);
                    }
                }
                if (!unvisitedCities.isEmpty()) {
                    nextCity = unvisitedCities.get(random.nextInt(unvisitedCities.size()));
                } else {
                    // This should not happen if n > 0 and the loop terminates correctly
                    // Break the loop if no unvisited cities are left
                    break;
                }
            }
            childTour[i] = nextCity;
            visited[nextCity] = true;
            currentCity = nextCity;
        }

        // ERX typically produces a single offspring. We can generate a second by swapping parents or
        // starting from parent2, but for simplicity, we can also return two identical or one real and one random.
        // For a more standard (P1, P2) -> (C1, C2) EA, let's just make two identical copies or generate a second random one.
        // A better approach for the second child for ERX is to start with a different initial city.
        Individual child1 = new Individual(childTour);
        // For the second child, a common practice is to just return parent2 or generate another child.
        // For ERX, generating one good child and then perhaps another with a different starting point or random is common.
        // Let's create a second one by swapping parents for the ERX call.
        // This is a common way to get a second child from single-child operators like ERX or mutate and return
        // an already good individual.
        Individual child2;
        try {
            child2 = new Individual(edgeRecombinationCrossover(parent2, parent1)[0].getTour());
        } catch (Exception e) {
            // Fallback if ERX fails to produce a second valid tour (e.g., small N or edge cases)
            child2 = Individual.random(n);
        }

        return new Individual[]{child1, child2};
    }


    /**
     * Applies a mutation operator to an individual.
     *
     * @param individual The individual to mutate
     * @param mutationType The type of mutation to perform
     */
    private static void mutate(Individual individual, int mutationType) {
        switch (mutationType) {
            case MUTATION_SWAP:
                swapMutation(individual);
                break;
            case MUTATION_INSERT:
                insertMutation(individual);
                break;
            case MUTATION_INVERT:
                inversionMutation(individual);
                break;
            default:
                throw new IllegalArgumentException("Unknown mutation type: " + mutationType);
        }
    }

    /**
     * Swap Mutation operator.
     * Swaps two random cities in the tour.
     */
    private static void swapMutation(Individual individual) {
        int n = individual.tour.length;
        if (n < 2) return; // Cannot swap with less than 2 cities

        int index1 = random.nextInt(n);
        int index2 = random.nextInt(n);

        // Ensure indices are different
        while (index1 == index2) {
            index2 = random.nextInt(n);
        }

        int temp = individual.tour[index1];
        individual.tour[index1] = individual.tour[index2];
        individual.tour[index2] = temp;
    }

    /**
     * Insert Mutation operator.
     * Moves a randomly selected city to a new random position.
     */
    private static void insertMutation(Individual individual) {
        int n = individual.tour.length;
        if (n < 2) return; // Cannot insert with less than 2 cities

        int indexToRemove = random.nextInt(n);
        int indexToInsert = random.nextInt(n);

        // Ensure target position is different
        while (indexToRemove == indexToInsert) {
            indexToInsert = random.nextInt(n);
        }

        int cityToMove = individual.tour[indexToRemove];

        // Create a temporary list to easily remove and insert
        List<Integer> tempTourList = Arrays.stream(individual.tour).boxed().collect(Collectors.toList());
        tempTourList.remove(indexToRemove);
        tempTourList.add(indexToInsert, cityToMove);

        // Copy back to the original array
        for (int i = 0; i < n; i++) {
            individual.tour[i] = tempTourList.get(i);
        }
    }

    /**
     * Inversion (Reverse) Mutation operator.
     * Selects a subsegment and reverses its order.
     */
    private static void inversionMutation(Individual individual) {
        int n = individual.tour.length;
        if (n < 2) return; // Cannot invert with less than 2 cities

        int index1 = random.nextInt(n);
        int index2 = random.nextInt(n);

        if (index1 > index2) {
            int temp = index1;
            index1 = index2;
            index2 = temp;
        }

        // Ensure segment is at least 2 cities long for meaningful inversion
        if (index1 == index2) {
            if (index1 == n - 1) index1--; else index2++;
            if (index1 > index2) { // Re-swap if necessary
                int temp = index1;
                index1 = index2;
                index2 = temp;
            }
            if (index1 == index2) return; // Still single city, cannot invert
        }

        // Reverse the segment
        while (index1 < index2) {
            int temp = individual.tour[index1];
            individual.tour[index1] = individual.tour[index2];
            individual.tour[index2] = temp;
            index1++;
            index2--;
        }
    }

    /**
     * Calculate average fitness of a population.
     */
    private static double averageFitness(List<Individual> population) {
        if (population.isEmpty()) return 0;
        double sum = 0;
        for (Individual ind : population) {
            sum += ind.fitness;
        }
        return sum / population.size();
    }

    /**
     * Utility method to check if an array contains a value.
     * Used in OX crossover.
     */
    private static boolean contains(int[] arr, int val) {
        for (int a : arr) {
            if (a == val) return true;
        }
        return false;
    }

    /**
     * Returns the name of a crossover type.
     */
    private static String getCrossoverName(int crossoverType) {
        switch (crossoverType) {
            case CROSSOVER_OX: return "Order Crossover (OX)";
            case CROSSOVER_PMX: return "Partially Mapped Crossover (PMX)";
            case CROSSOVER_ERX: return "Edge Recombination Crossover (ERX)";
            default: return "Unknown Crossover";
        }
    }

    /**
     * Returns the name of a mutation type.
     */
    private static String getMutationName(int mutationType) {
        switch (mutationType) {
            case MUTATION_SWAP: return "Swap Mutation";
            case MUTATION_INSERT: return "Insert Mutation";
            case MUTATION_INVERT: return "Inversion Mutation";
            default: return "Unknown Mutation";
        }
    }

    /**
     * Returns the name of a selection type.
     */
    private static String getSelectionName(int selectionType) {
        switch (selectionType) {
            case SELECTION_TOURNAMENT: return "Tournament Selection";
            case SELECTION_ROULETTE: return "Roulette Wheel Selection";
            default: return "Unknown Selection";
        }
    }

    // Removed the simplified run method, as the main run method handles all parameters.
}