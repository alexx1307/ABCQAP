package com.luka.algorithm.parallel;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import com.luka.algorithm.ABCAlgorithmParameters;
import com.luka.qap.IAlgorithm;
import com.luka.qap.ProblemInstance;
import com.luka.qap.Solution;
import com.luka.qap.TimeStatistics;

import java.nio.IntBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by lukas on 25.03.2017.
 */
public class

FullGPUAbcAlgorithm implements IAlgorithm {
    private ProblemInstance problemInstance;
    private ABCAlgorithmParameters parameters;
    private ParallelComputingInstance parallelComputingInstance;
    private Random random;
    private CLBuffer<IntBuffer> weightsBuffer;
    private CLBuffer<IntBuffer> distancesBuffer;
    private CLBuffer<IntBuffer> solutionBuffer;

    private Solution bestSolution;
    private Instant algorithmSetup, beforeCriterionCheck, beforeEmployeePhase, beforeOnlookerPhase, beforeScoutPhase, beforeUpdatingPhase, afterUpdatingState;
    private TimeStatistics statistics;
    private  boolean debugModeEnabled = true;
    private final String programName = "qap_abc2.cl";
    private long seed;
    private int workGrupSize;
    private int workGroupsNumber;


    public FullGPUAbcAlgorithm(ABCAlgorithmParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public Solution run() {

        short onlookerMethod = parameters.getOnlookersMethod().getCode();
        short useReductionToFindBest = parameters.getUseReductionToFindBest();
        parallelComputingInstance.runWholeAlgorithmOnGPU(weightsBuffer,distancesBuffer,parameters.getFoodSourcesNumber(),parameters.getOnlookersNumber(), problemInstance.getProblemSize(),parameters.getMaxIterations(), parameters.getFoodSourceTrialsLimit(),seed,workGrupSize, solutionBuffer, workGroupsNumber, useReductionToFindBest, onlookerMethod );
        parallelComputingInstance.finish();
        Solution best = null;
        int[] ints = parallelComputingInstance.readBufferToArray(solutionBuffer);
        for(int i = 0; i< workGroupsNumber;i++) {
            int problemSize = problemInstance.getProblemSize();
            ArrayList<Integer> list = new ArrayList<>(IntStream.of(ints).boxed().collect(Collectors.toList()).subList(i*problemSize, (i+1)*problemSize));
            Solution sol = new Solution(list,problemInstance);
            //System.out.println("i = " + i +", sol = "+sol.toString());
            if(best == null || best.getEvaluatedResult() > sol.getEvaluatedResult()){
                best = sol;
            }
        }

        return best;
    }

    @Override
    public void releaseMemory() {
        distancesBuffer.release();
        solutionBuffer.release();
        weightsBuffer.release();
        parallelComputingInstance.release();
    }

    @Override
    public void init(long seed) {
        parallelComputingInstance = new ParallelComputingInstance(programName);
        parallelComputingInstance.init();
        this.seed = seed;
        workGrupSize = 256;
        // dividing with ceiling up
        workGroupsNumber =  (parameters.getFoodSourcesNumber() + workGrupSize - 1)/workGrupSize;
        initBuffers(workGroupsNumber);
        statistics= new TimeStatistics(5);
    }

    private void initBuffers(int workGroupsNumber) {
        weightsBuffer = parallelComputingInstance.createBufferFrom2DArray(problemInstance.getWeights(), CLMemory.Mem.READ_ONLY);
        distancesBuffer = parallelComputingInstance.createBufferFrom2DArray(problemInstance.getDistances(), CLMemory.Mem.READ_ONLY);
        solutionBuffer = parallelComputingInstance.createIntBuffer(problemInstance.getProblemSize()*workGroupsNumber, CLMemory.Mem.WRITE_ONLY);
        //foodSourcesBuffer = parallelComputingInstance.createIntBuffer(parameters.getFoodSourcesNumber() * problemInstance.getProblemSize(), CLMemory.Mem.READ_WRITE);
        //costFunctionValuesBuffer = parallelComputingInstance.createIntBuffer(parameters.getFoodSourcesNumber(), CLMemory.Mem.READ_WRITE);

        /*int[] triesArray = new int[parameters.getFoodSourcesNumber()];
        for (int i = 0; i < triesArray.length; i++) {
            triesArray[i] = 0;
        }*/
        //triesBuffer = parallelComputingInstance.createBufferFrom1DArray(triesArray, CLMemory.Mem.READ_WRITE);

        parallelComputingInstance.writeBuffer(weightsBuffer);
        parallelComputingInstance.writeBuffer(distancesBuffer);
        //parallelComputingInstance.writeBuffer(triesBuffer);
    }





    @Override
    public TimeStatistics getStatistics() {
        return statistics;
    }

    @Override
    public void setProblem(ProblemInstance problem) {
        this.problemInstance = problem;
    }

}
