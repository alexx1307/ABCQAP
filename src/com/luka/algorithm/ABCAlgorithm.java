package com.luka.algorithm;

import com.luka.algorithm.stopCriterion.IterationStopCriterion;
import com.luka.qap.IAlgorithm;
import com.luka.qap.ProblemInstance;
import com.luka.qap.Solution;
import com.luka.qap.TimeStatistics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by lukas on 12.11.2016.
 */
public abstract class ABCAlgorithm implements IAlgorithm {
    private static final Logger LOGGER = Logger.getLogger("myLogger");
    protected ABCAlgorithmParameters parameters;
    protected ProblemInstance problemInstance;
    private AlgorithmState algorithmState;
    protected TimeStatistics statistics;
    private Timer timer;
    protected Instant algorithmSetup, beforeCriterionCheck, beforeEmployeePhase, beforeOnlookerPhase, beforeScoutPhase, beforeUpdatingPhase, afterUpdatingState;

    public ABCAlgorithm(ABCAlgorithmParameters parameters) {
        LOGGER.setLevel(Level.WARNING);
        this.parameters = parameters;

        algorithmState = new AlgorithmState();

        algorithmState.setBestResultFound(null);
        algorithmState.resetIterations();

        int initStatisticsPhases = parameters.getMaxIterations();
        statistics = new TimeStatistics(initStatisticsPhases);
    }


    @Override
    public Solution run() {
        algorithmSetup = Instant.now();
        System.out.println("Initializing food sources");
        initFoodSources();
        beforeCriterionCheck = Instant.now();
        statistics.updateInitStatistics(algorithmSetup, beforeCriterionCheck);
        while (algorithmState.getIteration() < parameters.getMaxIterations()) {
            beforeEmployeePhase = Instant.now();
            //System.out.println("employedBeesPhase");
            employedBeesPhase();
            beforeOnlookerPhase = Instant.now();
            //System.out.println("onlookerBeesPhase");
            onlookerBeesPhase();
            beforeScoutPhase = Instant.now();
            scoutBeesPhase();
            beforeUpdatingPhase = Instant.now();
            updateAlgorithmState();
            afterUpdatingState = Instant.now();
            statistics.updateEveryTurnStatistics(beforeCriterionCheck, beforeEmployeePhase, beforeOnlookerPhase, beforeScoutPhase, beforeUpdatingPhase, afterUpdatingState);
            beforeCriterionCheck = afterUpdatingState;
        }
        return algorithmState.getBestResultFound();
    }

    protected abstract void scoutBeesPhase();

    protected abstract void onlookerBeesPhase();

    protected abstract void employedBeesPhase();

    protected abstract void initFoodSources();

    private void updateAlgorithmState() {
        incrementTrials();
        Solution best = getBestSolution();
        if (algorithmState.getBestResultFound() == null || algorithmState.getBestResultFound().getEvaluatedResult() > best.getEvaluatedResult()) {
            algorithmState.setBestResultFound(best);
        }
        /*LOGGER.warning(algorithmState.getIteration() + ": " + algorithmState.getBestResultFound().getEvaluatedResult());

        for (FoodSource foodSource : foodSources) {
            LOGGER.info(foodSource.getSolution().getEvaluatedResult() +" trials: "+foodSource.getTrialsNumber());
        }*/


        algorithmState.incrementIteration();
    }

    abstract protected Solution getBestSolution();

    abstract protected void incrementTrials();

    @Override
    public TimeStatistics getStatistics() {
        return statistics;
    }

    @Override
    public void setProblem(ProblemInstance problem) {
        this.problemInstance = problem;
    }


}
