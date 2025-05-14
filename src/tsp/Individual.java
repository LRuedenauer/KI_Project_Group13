package tsp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class Individual {
    public int[] tour;
    public double fitness;

    public Individual(int[] tour) {
        this.tour = tour.clone();
    }

    public static Individual random(int n) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(i);
        Collections.shuffle(list);
        int[] t = list.stream().mapToInt(i -> i).toArray();
        return new Individual(t);
    }

    public void evaluate(TSPInstance tsp) {
        this.fitness = tsp.totalDistance(this.tour);
    }
}
