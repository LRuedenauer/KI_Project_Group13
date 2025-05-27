package tsp;

import java.io.*;
import java.util.*;

/**
 * Main class to run the TSP evolutionary algorithm.
 * Supports reading TSP instances from files and configuring EA parameters from a properties file or command line.
 */
public class Main {

    private static final String CONFIG_FILE = "config.properties"; // Default configuration file name

    public static void main(String[] args) {
        try {
            // Load configuration from file first
            Properties config = loadConfiguration(CONFIG_FILE);

            // Apply command-line arguments, overriding file settings
            applyCommandLineArgs(config, args);

            // Extract parameters from config
            String filename = config.getProperty("tsp.file");
            int mu = Integer.parseInt(config.getProperty("ea.mu", "50"));
            int lambda = Integer.parseInt(config.getProperty("ea.lambda", "100"));
            int generations = Integer.parseInt(config.getProperty("ea.generations", "1000"));
            double mutationRate = Double.parseDouble(config.getProperty("ea.mutationRate", "0.2"));
            int crossoverType = parseCrossoverType(config.getProperty("ea.crossoverType", "OX"));
            int mutationType = parseMutationType(config.getProperty("ea.mutationType", "SWAP"));
            int selectionType = parseSelectionType(config.getProperty("ea.selectionType", "TOURNAMENT"));
            int tournamentSize = Integer.parseInt(config.getProperty("ea.tournamentSize", "3"));
            boolean useGreedyInit = Boolean.parseBoolean(config.getProperty("ea.greedyInitialization", "false"));
            boolean verbose = Boolean.parseBoolean(config.getProperty("ea.verbose", "true"));
            String outputFileName = config.getProperty("output.bestTourFile", "best_tour_results.txt");


            TSPInstance tspInstance;
            if (filename != null && !filename.isEmpty()) {
                System.out.println("Reading TSP instance from file: " + filename);
                tspInstance = TSPLibReader.readTSPInstance(filename);
            } else {
                int numCities = Integer.parseInt(config.getProperty("tsp.numCities", "50"));
                System.out.println("Generating random TSP instance with " + numCities + " cities.");
                tspInstance = TSPLibReader.createRandomInstance(numCities);
            }

            System.out.println("\n--- Evolutionary Algorithm Parameters ---");
            System.out.println("TSP Instance: " + tspInstance.getName() + " (" + tspInstance.getNumCities() + " cities)");
            System.out.println("Parent Population Size (μ): " + mu);
            System.out.println("Offspring Population Size (λ): " + lambda);
            System.out.println("Generations: " + generations);
            System.out.println("Mutation Rate: " + String.format("%.2f", mutationRate));
            System.out.println("Crossover Type: " + getCrossoverName(crossoverType));
            System.out.println("Mutation Type: " + getMutationName(mutationType));
            System.out.println("Parent Selection Type: " + getSelectionName(selectionType));
            if (selectionType == EA.SELECTION_TOURNAMENT) {
                System.out.println("Tournament Size: " + tournamentSize);
            }
            System.out.println("Greedy Initialization: " + useGreedyInit);
            System.out.println("Verbose Output: " + verbose);
            System.out.println("Results will be saved to: " + outputFileName);
            System.out.println("-----------------------------------------");

            // Run the EA
            Individual bestIndividual = EA.run(tspInstance, mu, lambda, generations, mutationRate,
                    crossoverType, mutationType, selectionType, tournamentSize, useGreedyInit, verbose);

            System.out.println("\n--- EA Simulation Complete ---");
            System.out.println("Best Tour Found: " + bestIndividual.toString());
            System.out.println("Overall Best Fitness: " + String.format("%.2f", bestIndividual.fitness));

            // Save the best tour to a file
            saveBestTour(bestIndividual, tspInstance, outputFileName);

        } catch (FileNotFoundException e) {
            System.err.println("Error: Configuration file '" + CONFIG_FILE + "' not found. Please create it or use command-line arguments.");
            printHelp();
            System.exit(1);
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Loads configuration properties from a file.
     *
     * @param filename The name of the properties file
     * @return A Properties object containing the configuration
     * @throws IOException If an error occurs while reading the file
     */
    private static Properties loadConfiguration(String filename) throws IOException {
        Properties config = new Properties();
        File configFile = new File(filename);
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                config.load(input);
                System.out.println("Configuration loaded from " + filename);
            }
        } else {
            System.out.println("No configuration file '" + filename + "' found. Using default parameters and command-line arguments.");
            // Set some sensible defaults if no file exists
            setDefaultConfig(config);
        }
        return config;
    }

    /**
     * Sets default values in the Properties object if the config file is not found.
     */
    private static void setDefaultConfig(Properties config) {
        config.setProperty("tsp.file", ""); // No default file
        config.setProperty("tsp.numCities", "50"); // Default for random instance
        config.setProperty("ea.mu", "50");
        config.setProperty("ea.lambda", "100");
        config.setProperty("ea.generations", "1000");
        config.setProperty("ea.mutationRate", "0.2");
        config.setProperty("ea.crossoverType", "OX");
        config.setProperty("ea.mutationType", "SWAP");
        config.setProperty("ea.selectionType", "TOURNAMENT");
        config.setProperty("ea.tournamentSize", "3");
        config.setProperty("ea.greedyInitialization", "false");
        config.setProperty("ea.verbose", "true");
        config.setProperty("output.bestTourFile", "best_tour_results.txt");
    }


    /**
     * Applies command-line arguments, overriding properties loaded from the file.
     */
    private static void applyCommandLineArgs(Properties config, String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-f":
                case "--file":
                    config.setProperty("tsp.file", args[++i]);
                    break;
                case "-n":
                case "--numCities":
                    config.setProperty("tsp.numCities", args[++i]);
                    break;
                case "-m":
                case "--mu":
                    config.setProperty("ea.mu", args[++i]);
                    break;
                case "-l":
                case "--lambda":
                    config.setProperty("ea.lambda", args[++i]);
                    break;
                case "-g":
                case "--generations":
                    config.setProperty("ea.generations", args[++i]);
                    break;
                case "-mr":
                case "--mutationRate":
                    config.setProperty("ea.mutationRate", args[++i]);
                    break;
                case "-cx":
                case "--crossover":
                    config.setProperty("ea.crossoverType", args[++i].toUpperCase());
                    break;
                case "-mt":
                case "--mutation":
                    config.setProperty("ea.mutationType", args[++i].toUpperCase());
                    break;
                case "-s":
                case "--selection":
                    config.setProperty("ea.selectionType", args[++i].toUpperCase());
                    break;
                case "-ts":
                case "--tournamentSize":
                    config.setProperty("ea.tournamentSize", args[++i]);
                    break;
                case "--greedy":
                    config.setProperty("ea.greedyInitialization", "true");
                    break;
                case "-q":
                case "--quiet":
                    config.setProperty("ea.verbose", "false");
                    break;
                case "-o":
                case "--output":
                    config.setProperty("output.bestTourFile", args[++i]);
                    break;
                case "-h":
                case "--help":
                    printHelp();
                    System.exit(0);
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    printHelp();
                    System.exit(1);
            }
        }
    }

    /**
     * Parses the crossover type string to its corresponding integer constant.
     */
    private static int parseCrossoverType(String type) {
        switch (type.toUpperCase()) {
            case "OX": return EA.CROSSOVER_OX;
            case "PMX": return EA.CROSSOVER_PMX;
            case "ERX": return EA.CROSSOVER_ERX;
            default:
                System.err.println("Warning: Unknown crossover type '" + type + "'. Using OX (Order Crossover).");
                return EA.CROSSOVER_OX;
        }
    }

    /**
     * Parses the mutation type string to its corresponding integer constant.
     */
    private static int parseMutationType(String type) {
        switch (type.toUpperCase()) {
            case "SWAP": return EA.MUTATION_SWAP;
            case "INSERT": return EA.MUTATION_INSERT;
            case "INVERT": return EA.MUTATION_INVERT;
            default:
                System.err.println("Warning: Unknown mutation type '" + type + "'. Using SWAP (Swap Mutation).");
                return EA.MUTATION_SWAP;
        }
    }

    /**
     * Parses the selection type string to its corresponding integer constant.
     */
    private static int parseSelectionType(String type) {
        switch (type.toUpperCase()) {
            case "TOURNAMENT": return EA.SELECTION_TOURNAMENT;
            case "ROULETTE": return EA.SELECTION_ROULETTE;
            default:
                System.err.println("Warning: Unknown selection type '" + type + "'. Using TOURNAMENT (Tournament Selection).");
                return EA.SELECTION_TOURNAMENT;
        }
    }

    /**
     * Saves the best found tour to a text file.
     *
     * @param bestTour The Individual representing the best tour
     * @param tspInstance The TSP instance
     * @param filename The name of the file to save to
     */
    private static void saveBestTour(Individual bestTour, TSPInstance tspInstance, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("--- TSP EA Results ---");
            writer.println("TSP Instance: " + tspInstance.getName() + " (" + tspInstance.getNumCities() + " cities)");
            writer.println("Best Fitness (Total Distance): " + String.format("%.2f", bestTour.fitness));
            writer.println("Best Tour Sequence (0-indexed cities):");
            StringBuilder tourString = new StringBuilder();
            for (int i = 0; i < bestTour.tour.length; i++) {
                tourString.append(bestTour.tour[i]);
                if (i < bestTour.tour.length - 1) {
                    tourString.append(", ");
                }
            }
            writer.println(tourString.toString());
            writer.println("Complete Tour Path:");
            writer.println(bestTour.toString()); // Uses the individual's toString for detailed path
            writer.println("----------------------");
            System.out.println("Best tour saved to: " + filename);
        } catch (IOException e) {
            System.err.println("Error saving best tour to file: " + e.getMessage());
        }
    }

    /**
     * Prints the help message for command line arguments.
     */
    private static void printHelp() {
        System.out.println("Usage: java -jar TSP_EA.jar [options]");
        System.out.println("Options can be specified via 'config.properties' file or command line arguments.");
        System.out.println("Command line arguments override settings in 'config.properties'.");
        System.out.println("\nOptions:");
        System.out.println("  -f, --file <path>           Path to the TSPLIB instance file (e.g., data/berlin52.tsp)");
        System.out.println("  -n, --numCities <n>         Number of cities for a randomly generated TSP instance (if -f is not used). Default: 50");
        System.out.println("  -m, --mu <n>                Parent population size (μ). Default: 50");
        System.out.println("  -l, --lambda <n>            Offspring population size (λ). Default: 100");
        System.out.println("  -g, --generations <n>       Number of generations to run. Default: 1000");
        System.out.println("  -mr, --mutationRate <rate>  Probability of mutation (0.0-1.0). Default: 0.2");
        System.out.println("  -cx, --crossover <type>     Crossover type (OX, PMX, ERX). Default: OX");
        System.out.println("  -mt, --mutation <type>      Mutation type (SWAP, INSERT, INVERT). Default: SWAP");
        System.out.println("  -s, --selection <type>      Parent selection type (TOURNAMENT, ROULETTE). Default: TOURNAMENT");
        System.out.println("  -ts, --tournamentSize <n>   Tournament size for tournament selection. Default: 3");
        System.out.println("  --greedy                    Use greedy initialization for part of the initial population. Default: false");
        System.out.println("  -q, --quiet                 Suppress detailed generational output. Default: false (verbose)");
        System.out.println("  -o, --output <filename>     File to save the best tour results. Default: best_tour_results.txt");
        System.out.println("  -h, --help                  Show this help message");
    }

    /**
     * Get the name of a crossover type (moved from EA to Main for display purposes).
     */
    private static String getCrossoverName(int crossoverType) {
        switch (crossoverType) {
            case EA.CROSSOVER_OX: return "Order Crossover (OX)";
            case EA.CROSSOVER_PMX: return "Partially Mapped Crossover (PMX)";
            case EA.CROSSOVER_ERX: return "Edge Recombination Crossover (ERX)";
            default: return "Unknown";
        }
    }

    /**
     * Get the name of a mutation type (moved from EA to Main for display purposes).
     */
    private static String getMutationName(int mutationType) {
        switch (mutationType) {
            case EA.MUTATION_SWAP: return "Swap Mutation";
            case EA.MUTATION_INSERT: return "Insert Mutation";
            case EA.MUTATION_INVERT: return "Inversion Mutation";
            default: return "Unknown";
        }
    }

    /**
     * Get the name of a selection type (moved from EA to Main for display purposes).
     */
    private static String getSelectionName(int selectionType) {
        switch (selectionType) {
            case EA.SELECTION_TOURNAMENT: return "Tournament Selection";
            case EA.SELECTION_ROULETTE: return "Roulette Wheel Selection";
            default: return "Unknown";
        }
    }
}