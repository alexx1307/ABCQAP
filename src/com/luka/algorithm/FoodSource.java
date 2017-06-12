package com.luka.algorithm;

import com.luka.qap.Solution;

/**
 * Created by lukas on 12.11.2016.
 */
public class FoodSource implements IFitnessEvaluable {
    private Solution solution;
    private Integer trialsNumber = 0;

    @Override
    public Double getFitness() {
        Integer result = solution.getEvaluatedResult();
        if(solution.getProblem().isMinimalizationProblem()){
            if(result < 0){
                return 1.0 - result;   //1+abs(res)
            }else{
                return 1.0/(1.0 + result);  //1/(1+res)
            }
        }else{
            return result.doubleValue();
        }
    }

    public Solution getSolution() {
        return solution;
    }

    public void incrementTrials() {
        trialsNumber++;
    }

    public void setSolution(Solution solution) {
        this.solution = solution;
        resetTrials();
    }

    private void resetTrials() {
        setTrialsNumber(0);
    }

    private void setTrialsNumber(int trialsNumber) {
        this.trialsNumber = trialsNumber;
    }

    public Integer getTrialsNumber() {
        return trialsNumber;
    }
}
