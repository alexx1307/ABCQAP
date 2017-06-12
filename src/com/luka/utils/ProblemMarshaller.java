package com.luka.utils;

import com.luka.qap.ProblemInstance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

/**
 * Created by lukas on 11.03.2017.
 */
public class ProblemMarshaller {
    public static ProblemInstance createProblemFromFile(File file) {
        try (Scanner scanner = new Scanner(file)) {
            int size = scanner.nextInt();
            int[][] distances = new int[size][size];
            int[][] weights = new int[size][size];

            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    weights[i][j] = scanner.nextInt();
                }
            }

            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    distances[i][j] = scanner.nextInt();
                }
            }

            return ProblemInstance.ProblemFactory.createProblemFromArrays(weights, distances);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}