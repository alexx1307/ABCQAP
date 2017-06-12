package com.luka.algorithm;

import com.luka.algorithm.selection.ISelectionStrategy;
import com.luka.qap.Solution;

import java.util.*;

/**
 * Created by lukas on 25.03.2017.
 */
public class SequentionalAbcAlgorithm extends ABCAlgorithm {

    protected List<FoodSource> foodSources;
    public SequentionalAbcAlgorithm(ABCAlgorithmParameters parameters) {
        super(parameters);
    }

    @Override
    protected void employedBeesPhase() {
        for (FoodSource foodSource : foodSources) {
            Solution mutatedSolution = searchInNeighbourhood(foodSource.getSolution());
            if (mutatedSolution.isBetterThan(foodSource.getSolution())) {
                foodSource.setSolution(mutatedSolution);
            }/* else {
                foodSource.incrementTrials();
            }*/
        }
    }


    @Override
    protected void onlookerBeesPhase() {
        ISelectionStrategy selectionStrategy = parameters.getSelectionStrategy();
        List<FoodSource> selectedFoodSources = selectionStrategy.select(foodSources, parameters.getOnlookersNumber());

        for (FoodSource foodSource : selectedFoodSources) {
            Solution mutatedSolution = searchInNeighbourhood(foodSource.getSolution());
            if (mutatedSolution.isBetterThan(foodSource.getSolution())) {
                foodSource.setSolution(mutatedSolution);
            } /*else {
                foodSource.incrementTrials();
            }*/
        }
    }

    @Override
    protected void scoutBeesPhase() {
        for (FoodSource foodSource : foodSources) {
            if (foodSource.getTrialsNumber() > parameters.getFoodSourceTrialsLimit()) {
                foodSource.setSolution(createRandomSolution());
            }
        }
    }


    private Solution searchInNeighbourhood(Solution oldSolution) {
        Solution solution = new Solution((ArrayList)oldSolution.getFacilitiesMapping().clone(), oldSolution.getProblem());
        Random random = new Random();
        int i = random.nextInt(oldSolution.getProblem().getProblemSize());
        int j;
        do {
            j = random.nextInt(oldSolution.getProblem().getProblemSize());
        }
        while (i == j);
        Collections.swap(solution.getFacilitiesMapping(), i, j);
        return solution;
    }


    @Override
    protected void initFoodSources() {
        int foodSourcesNumber = parameters.getFoodSourcesNumber();
        foodSources = new ArrayList<>(foodSourcesNumber);
        for (int i = 0; i < foodSourcesNumber; i++) {
            FoodSource foodSource = new FoodSource();
            foodSource.setSolution(createRandomSolution());
            foodSources.add(foodSource);
        }
    }

    @Override
    protected void incrementTrials() {
        foodSources.stream().forEach(FoodSource::incrementTrials);
    }

    @Override
    protected Solution getBestSolution() {
        return foodSources.stream().max(Comparator.comparing(f -> f.getFitness())).get().getSolution();
    }

    private Solution createRandomSolution() {
        ArrayList<Integer> mapping = new ArrayList<>(problemInstance.getProblemSize());
        for (int i = 0; i < problemInstance.getProblemSize(); i++) {
            mapping.add(i);
        }
        Collections.shuffle(mapping);
        return new Solution(mapping, problemInstance);

    }


    @Override
    public void init(long seed) {

    }

    public List<FoodSource> getFoodSources() {
        return foodSources;
    }
}
