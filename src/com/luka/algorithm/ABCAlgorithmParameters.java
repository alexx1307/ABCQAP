package com.luka.algorithm;

import com.luka.algorithm.selection.ISelectionStrategy;
import com.luka.algorithm.stopCriterion.IStopCriterion;

/**
 * Created by lukas on 12.11.2016.
 */
public class ABCAlgorithmParameters {
    private int foodSourcesNumber;
    private int onlookersNumber;
    private ISelectionStrategy selectionStrategy;
    private int foodSourceTrialsLimit;
    private IStopCriterion stopCriterion;

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

    public void setSelectionStrategy(ISelectionStrategy selectionStrategy) {
        this.selectionStrategy = selectionStrategy;
    }

    public int getFoodSourceTrialsLimit() {
        return foodSourceTrialsLimit;
    }

    public void setFoodSourceTrialsLimit(int foodSourceTrialsLimit) {
        this.foodSourceTrialsLimit = foodSourceTrialsLimit;
    }

    public IStopCriterion getStopCriterion() {
        return stopCriterion;
    }

    public void setStopCriterion(IStopCriterion stopCriterion) {
        this.stopCriterion = stopCriterion;
    }
}
