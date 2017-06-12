package com.luka.algorithm;

import com.luka.qap.Solution;

/**
 * Created by lukas on 25.02.2017.
 */
public class AlgorithmState {
    private int iteration;
    private Solution bestResultFound;

    public int getIteration() {
        return iteration;
    }

    public Solution getBestResultFound() {
        return bestResultFound;
    }

    public void setBestResultFound(Solution bestResultFound) {
        this.bestResultFound = bestResultFound;
    }

    public void incrementIteration() {
        iteration++;
    }

    public void resetIterations() {
        iteration = 0;
    }
}
