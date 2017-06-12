package com.luka.algorithm.stopCriterion;

import com.luka.algorithm.AlgorithmState;
import com.luka.qap.TimeStatistics;

/**
 * Created by lukas on 25.02.2017.
 */
public class IterationStopCriterion implements IStopCriterion {
    private Integer maxIterationNumber;

    public IterationStopCriterion(Integer maxIterationNumber) {
        this.maxIterationNumber = maxIterationNumber;
    }

    @Override
    public boolean isStopCriterionMet(AlgorithmState state, TimeStatistics statistics) {
        return state.getIteration() >= maxIterationNumber;
    }

    public Integer getMaxIterationNumber() {
        return maxIterationNumber;
    }
}
