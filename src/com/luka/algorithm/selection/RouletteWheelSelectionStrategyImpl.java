package com.luka.algorithm.selection;

import com.luka.algorithm.FoodSource;
import com.luka.algorithm.IFitnessEvaluable;
import com.luka.qap.ProblemInstance;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by lukas on 25.02.2017.
 */
public class RouletteWheelSelectionStrategyImpl implements ISelectionStrategy<FoodSource>{
    private static final Logger LOGGER = Logger.getLogger("myLogger");
    
    private Random random;

    public RouletteWheelSelectionStrategyImpl(Random random) {
        this.random = random;
    }

    @Override
    public List<FoodSource> select(List<FoodSource> sources, Integer selectingObjectsNumber) {
        double[] probCompositionArray = new double[sources.size()];
        double lastVal = 0.0;
        for (int i = 0; i < sources.size(); i++) {
            probCompositionArray[i] = sources.get(i).getFitness() + lastVal;
            lastVal = probCompositionArray[i];
        }
        double sumOfFitness = probCompositionArray[sources.size() - 1];
        for (FoodSource source : sources) {
            LOGGER.info(source.getSolution().getEvaluatedResult()+" is "+ source.getFitness()/sumOfFitness);
        }


        List<FoodSource> selectionResult = new LinkedList<FoodSource>();


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

    public int[] selectIndexesForFitness(int[] costFunctionValues,  int selectingObjectsNumber) {
        double[] probCompositionArray = Arrays.stream(costFunctionValues)
                .mapToDouble(costValue -> evaluateFitness(costValue, null)).toArray();
        double lastVal = 0.0;
        for (int i = 0; i < probCompositionArray.length; i++) {
            probCompositionArray[i] += lastVal;
            lastVal = probCompositionArray[i];
        }
        double sumOfFitness = probCompositionArray[probCompositionArray.length - 1];

        List<FoodSource> selectionResult = new LinkedList<FoodSource>();

        int[] results = new int[selectingObjectsNumber];
        for (int i = 0; i < selectingObjectsNumber; i++) {
            double randValue = random.nextDouble() * sumOfFitness;
            int index = Arrays.binarySearch(probCompositionArray, randValue);
            if (index < 0) {
                //not equal value found (as expected
                //Below is method to revert negative index into insertion point
                index = Math.abs(index + 1);
            }
            results[i] = index;
        }
        return results;
    }

    private double evaluateFitness(int costValue, ProblemInstance problemInstance) {
        if (problemInstance == null || problemInstance.isMinimalizationProblem()) {
            if (costValue < 0) {
                return 1.0 - costValue;   //1+abs(res)
            } else {
                return 1.0 / (1.0 + costValue);  //1/(1+res)
            }
        } else {
            return (double) costValue;
        }
    }


}
