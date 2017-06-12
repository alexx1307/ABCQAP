package com.luka.qap;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lukas on 25.03.2017.
 */
public class TimeStatistics {


    private ArrayList<Duration> criterionPhaseTimes;
    private ArrayList<Duration> employeePhaseTimes;
    private ArrayList<Duration> onlookerPhaseTimes;
    private ArrayList<Duration> scoutPhaseTimes;
    private ArrayList<Duration> updatingPhaseTimes;

    private Instant startTime;
    private Duration initPhaseTime;
    private Duration totalTimeIn;
    private Duration totalTime;

    public TimeStatistics(int phases) {
        this.employeePhaseTimes = new ArrayList<>(phases);
        this.onlookerPhaseTimes = new ArrayList<>(phases);
        this.criterionPhaseTimes = new ArrayList<>(phases);
        this.scoutPhaseTimes = new ArrayList<>(phases);
        this.updatingPhaseTimes = new ArrayList<>(phases);
    }

    public long getInitPhaseTime() {
        return initPhaseTime.toMillis();
    }

    public long getTotalTimeInMs() {
        return totalTimeIn.toMillis();
    }

    public Double getAvgOnlookerPhaseTimeInMs(){
        return getAvgInMs(onlookerPhaseTimes);
    }

    public Double getAvgEmployeePhaseTimeInMs(){
        return getAvgInMs(employeePhaseTimes);
    }

    public Double getAvgScoutPhaseTimeInMs(){
        return getAvgInMs(scoutPhaseTimes);
    }

    public Double getAvgUpdatingPhaseTimeInMs(){
        return getAvgInMs(updatingPhaseTimes);
    }

    public Double getAvgCriterionPhaseTimeInMs(){
        return getAvgInMs(criterionPhaseTimes);
    }

    public Double getAvgInMs(List<Duration> durations){
        return durations.stream()
                .mapToDouble(a -> a.toMillis())
                .average().getAsDouble();
    }


    public void updateInitStatistics(Instant algorithmSetup, Instant beforeCriterionCheck) {
        totalTimeIn = initPhaseTime = Duration.between(algorithmSetup, beforeCriterionCheck);
        startTime = algorithmSetup;
    }

    public void updateEveryTurnStatistics(Instant beforeCriterionCheck, Instant beforeEmployeePhase, Instant beforeOnlookerPhase, Instant beforeScoutPhase, Instant beforeUpdatingPhase, Instant afterUpdatingState) {
        criterionPhaseTimes.add(Duration.between(beforeCriterionCheck, beforeEmployeePhase));
        employeePhaseTimes.add(Duration.between(beforeEmployeePhase, beforeOnlookerPhase));
        onlookerPhaseTimes.add(Duration.between(beforeOnlookerPhase, beforeScoutPhase));
        scoutPhaseTimes.add(Duration.between(beforeScoutPhase, beforeUpdatingPhase));
        updatingPhaseTimes.add(Duration.between(beforeUpdatingPhase, afterUpdatingState));

        totalTimeIn = Duration.between(startTime,afterUpdatingState);
    }

    public Duration getTotalTime() {
        return totalTime;
    }
}
