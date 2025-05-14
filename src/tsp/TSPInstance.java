package tsp;

public class TSPInstance {
    private double[][] distanceMatrix;

    public TSPInstance(double[][] distanceMatrix) {
        this.distanceMatrix = distanceMatrix;
    }

    public int getNumCities() {
        return distanceMatrix.length;
    }

    public double getDistance(int i, int j) {
        return distanceMatrix[i][j];
    }

    public double totalDistance(int[] tour) {
        double dist = 0;
        for (int i = 0; i < tour.length; i++) {
            int from = tour[i];
            int to = tour[(i + 1) % tour.length];
            dist += getDistance(from, to);
        }
        return dist;
    }
}
