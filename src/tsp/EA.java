package tsp;

import java.util.*;

public class EA {
    public static Individual run(TSPInstance tsp, int mu, int lambda, int generations, double mutationRate) {
        int numCities = tsp.getNumCities();
        List<Individual> population = new ArrayList<>();

        // Initialisierung
        for (int i = 0; i < mu; i++) {
            Individual ind = Individual.random(numCities);
            ind.evaluate(tsp);
            population.add(ind);
        }

        for (int gen = 0; gen < generations; gen++) {
            List<Individual> offspring = new ArrayList<>();

            for (int i = 0; i < lambda; i++) {
                Individual p1 = population.get((int)(Math.random() * mu));
                Individual p2 = population.get((int)(Math.random() * mu));

                Individual child = crossover(p1, p2);
                mutate(child, mutationRate);
                child.evaluate(tsp);
                offspring.add(child);
            }

            // (μ + λ) Selektion
            population.addAll(offspring);
            population.sort(Comparator.comparingDouble(ind -> ind.fitness));
            population = population.subList(0, mu);

            System.out.printf("Generation %d – Beste Distanz: %.2f\n", gen, population.get(0).fitness);
        }

        return population.get(0);
    }

    private static Individual crossover(Individual p1, Individual p2) {
        int size = p1.tour.length;
        int[] child = new int[size];
        Arrays.fill(child, -1);

        int a = (int)(Math.random() * size);
        int b = (int)(Math.random() * size);
        int start = Math.min(a, b), end = Math.max(a, b);

        for (int i = start; i <= end; i++)
            child[i] = p2.tour[i];

        int cur = 0;
        for (int i = 0; i < size; i++) {
            int val = p1.tour[i];
            if (!contains(child, val)) {
                while (child[cur] != -1) cur++;
                child[cur] = val;
            }
        }

        return new Individual(child);
    }

    private static void mutate(Individual ind, double mutationRate) {
        if (Math.random() < mutationRate) {
            int i = (int)(Math.random() * ind.tour.length);
            int j = (int)(Math.random() * ind.tour.length);
            int tmp = ind.tour[i];
            ind.tour[i] = ind.tour[j];
            ind.tour[j] = tmp;
        }
    }

    private static boolean contains(int[] arr, int val) {
        for (int a : arr) if (a == val) return true;
        return false;
    }
}

