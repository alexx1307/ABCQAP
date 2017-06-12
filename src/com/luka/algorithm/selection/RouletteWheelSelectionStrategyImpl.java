package com.luka.algorithm.selection;

import com.luka.algorithm.FoodSource;
import com.luka.algorithm.IFitnessEvaluable;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by lukas on 25.02.2017.
 */
public class RouletteWheelSelectionStrategyImpl<T extends IFitnessEvaluable> implements ISelectionStrategy<T> {
    private static final Logger LOGGER = Logger.getLogger("myLogger");
    
    private Random random;

    public RouletteWheelSelectionStrategyImpl(Random random) {
        this.random = random;
    }

    @Override
    public List<T> select(List<T> sources, Integer selectingObjectsNumber) {
        double[] probCompositionArray = new double[sources.size()];
        double lastVal = 0.0;
        for (int i = 0; i < sources.size(); i++) {
            probCompositionArray[i] = sources.get(i).getFitness() + lastVal;
            lastVal = probCompositionArray[i];
        }
        double sumOfFitness = probCompositionArray[sources.size() - 1];
        for (T source : sources) {
            LOGGER.info(((FoodSource)source).getSolution().getEvaluatedResult()+" is "+ source.getFitness()/sumOfFitness);
        }


        List<T> selectionResult = new LinkedList<T>();


        for (int i = 0; i < selectingObjectsNumber; i++) {
            double randValue = random.nextDouble() * sumOfFitness;
            int index = Arrays.binarySearch(probCompositionArray, randValue);
            if (index < 0) {
                //not equal value found (as expected
                //Below is method to revert negative index into insertion point
                index = Math.abs(index + 1);
            }
            selectionResult.add(sources.get(index));
        }
        return selectionResult;
    }


}
