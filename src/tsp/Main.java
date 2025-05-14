package tsp;

public class Main {
    public static void main(String[] args) {
        // Beispiel: Distanzmatrix (manuell eingegeben oder aus Datei)
        double[][] dist = {
                {0, 2, 9, 10},
                {1, 0, 6, 4},
                {15, 7, 0, 8},
                {6, 3, 12, 0}
        };

        TSPInstance tsp = new TSPInstance(dist);

        Individual best = EA.run(tsp, 50, 100, 200, 0.2);

        System.out.println("Beste Tour gefunden:");
        for (int city : best.tour)
            System.out.print(city + " ");
        System.out.printf("\nDistanz: %.2f\n", best.fitness);
    }
}
