package com.luka.algorithm.stopCriterion;

import com.luka.algorithm.AlgorithmState;
import com.luka.qap.TimeStatistics;

import java.time.Duration;

/**
 * Created by lukas on 29.04.2017.
 */
public class TotalTimeStopCriterion implements IStopCriterion {
    private Duration maxTime;

    public TotalTimeStopCriterion(Duration maxTime) {
        this.maxTime = maxTime;
    }

    @Override
    public boolean isStopCriterionMet(AlgorithmState state, TimeStatistics statistics) {
        return isFirstDurationGreater(statistics.getTotalTime(),maxTime);
    }

    private boolean isFirstDurationGreater(Duration a, Duration b){
        return a.compareTo(b) >0;
    }
}
