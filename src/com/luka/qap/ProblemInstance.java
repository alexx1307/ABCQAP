package com.luka.qap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by lukas on 22.06.2016.
 */
public class ProblemInstance {
    private int problemSize;
    private int[][] weights;
    private int[][] distances;
    private boolean minimalizationProblem;

    private ProblemInstance(int problemSize, int[][] weights, int[][] distances, boolean minimalizationProblem) {
        this.problemSize = problemSize;
        this.weights = weights;
        this.distances = distances;
        this.minimalizationProblem = minimalizationProblem;
    }

    public int getProblemSize() {
        return problemSize;
    }

    public int getWeight(int i, int j) {
        return weights[i][j];
    }

    public int getDistance(int i, int j) {
        return distances[i][j];
    }

    public int evaluateResult(ArrayList<Integer> facilitiesMapping ) {
        int sum = 0;
        for (int i = 0; i < facilitiesMapping.size(); i++) {
            //można by liczyć od j = i+1 ale wtedy założenie o symetryczności
            for (int j = i+1; j < facilitiesMapping.size(); j++) {
                sum += getWeight(facilitiesMapping.get(i), facilitiesMapping.get(j)) * getDistance(i, j);
            }
        }
        return sum;
    }
    @Override
    public String toString() {
        return "ProblemInstance{" +
                "problemSize=" + problemSize +
                "\n, weights=" + twoDimArrayToString(weights) +
                "\n, distances=" + twoDimArrayToString(distances) +
                "\n}\n";
    }

    private String twoDimArrayToString(int[][] array){
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("[\n");
        for (int[] row: array) {
            strBuilder.append('\t');
            strBuilder.append(Arrays.toString(row));
            strBuilder.append('\n');
        }
        strBuilder.append("]\n");
        return strBuilder.toString();
    }

    public boolean isMinimalizationProblem() {
        return minimalizationProblem;
    }

    public void setMinimalizationProblem(boolean minimalizationProblem) {
        this.minimalizationProblem = minimalizationProblem;
    }

    public int[][] getWeights() {
        return weights;
    }

    public int[][] getDistances() {
        return distances;
    }

    public static class ProblemFactory {
        public static ProblemInstance createRandomProblem(int size) {
            int[][] weights = new int[size][size];
            int[][] distances = new int[size][size];
            Random random = new Random();
            for (int i = 0; i < size; i++) {
                for (int j = i; j < size; j++) {
                    weights[i][j] = random.nextInt();
                    weights[j][i] = weights[i][j];
                    distances[i][j] = random.nextInt();
                    distances[j][i] = distances[i][j];
                }
            }
            return new ProblemInstance(size, weights, distances, true);
        }

        public static ProblemInstance createProblemFromArrays(int[][] weights, int[][]distances) {
            int size = weights.length;
            return new ProblemInstance(size, weights, distances, true);
        }
    }
}
