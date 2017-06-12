package com.luka.algorithm.stopCriterion;

import com.luka.algorithm.AlgorithmState;
import com.luka.qap.TimeStatistics;

/**
 * Created by lukas on 25.02.2017.
 */
public interface IStopCriterion {
    boolean isStopCriterionMet(AlgorithmState state, TimeStatistics statistics);
}
