package com.luka.algorithm;

import com.luka.algorithm.selection.ISelectionStrategy;

/**
 * Created by lukas on 12.11.2016.
 */
public class ABCAlgorithmParameters {
    private int foodSourcesNumber;
    private int onlookersNumber;
    private ISelectionStrategy selectionStrategy;
    private int foodSourceTrialsLimit;
    private int maxIterations;
    private ESelectionMethod onlookerMethod = ESelectionMethod.ELITE_SELECTION;
    private short useReductionToFindBest = 1;

    public int getFoodSourcesNumber() {
        return foodSourcesNumber;
    }

    public void setFoodSourcesNumber(int foodSourcesNumber) {
        this.foodSourcesNumber = foodSourcesNumber;
    }

    public int getOnlookersNumber() {
        return onlookersNumber;
    }

    public void setOnlookersNumber(int onlookersNumber) {
        this.onlookersNumber = onlookersNumber;
    }

    public ISelectionStrategy getSelectionStrategy() {
        return selectionStrategy;
    }

    public void setSelectionStrategy(ISelectionStrategy selectionStrategyName) {
        this.selectionStrategy = selectionStrategyName;
    }

    public int getFoodSourceTrialsLimit() {
        return foodSourceTrialsLimit;
    }

    public void setFoodSourceTrialsLimit(int foodSourceTrialsLimit) {
        this.foodSourceTrialsLimit = foodSourceTrialsLimit;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public ESelectionMethod getOnlookersMethod() {
        return onlookerMethod;
    }

    public void setOnlookerMethod(ESelectionMethod onlookerMethod) {
        this.onlookerMethod = onlookerMethod;
    }

    public short getUseReductionToFindBest() {
        return useReductionToFindBest;
    }

    public void setUseReductionToFindBest(short useReductionToFindBest) {
        this.useReductionToFindBest = useReductionToFindBest;
    }
}
