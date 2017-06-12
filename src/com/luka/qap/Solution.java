package com.luka.qap;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Created by lukas on 22.06.2016.
 */

public class Solution {
    private static final Logger LOGGER = Logger.getLogger("myLogger");
    private ArrayList<Integer> facilitiesMapping;
    private ProblemInstance problem;
    private Integer evaluatedResult = null;
    public static long newSolutionEvaluations = 0;

    public Solution(ArrayList<Integer> facilitiesMapping, ProblemInstance problem) {
        this.facilitiesMapping = facilitiesMapping;
        this.problem = problem;
    }

    @Override
    public String toString() {
        return "Solution{" +
                "facilitiesMapping=" + facilitiesMapping +
                ", evaluatedResult=" + getEvaluatedResult() +
                '}';
    }

    public Integer getEvaluatedResult() {
        if (evaluatedResult == null) {
            newSolutionEvaluations++;
            evaluatedResult = problem.evaluateResult(facilitiesMapping);
        }
        return evaluatedResult;
    }



    public ArrayList<Integer> getFacilitiesMapping() {
        return facilitiesMapping;
    }

    public ProblemInstance getProblem() {
        return problem;
    }

    public boolean isBetterThan(Solution other) {
        boolean result;
        if (problem.isMinimalizationProblem()) {
            result = getEvaluatedResult() < other.getEvaluatedResult();
        } else {
            result = getEvaluatedResult() > other.getEvaluatedResult();
        }
        if(result) {
            LOGGER.info("Progress! " + getEvaluatedResult() + " is better than " + other.getEvaluatedResult());
        }
        return result;
    }

}
