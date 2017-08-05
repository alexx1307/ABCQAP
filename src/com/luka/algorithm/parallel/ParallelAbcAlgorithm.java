package com.luka.algorithm.parallel;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLSubBuffer;
import com.luka.algorithm.ABCAlgorithmParameters;
import com.luka.algorithm.FoodSource;
import com.luka.qap.IAlgorithm;
import com.luka.qap.ProblemInstance;
import com.luka.qap.Solution;
import com.luka.qap.TimeStatistics;

import java.nio.IntBuffer;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by lukas on 25.03.2017.
 */
public class

ParallelAbcAlgorithm implements IAlgorithm {
    private ProblemInstance problemInstance;
    private ABCAlgorithmParameters parameters;
    private ParallelComputingInstance parallelComputingInstance;
    private Random random;
    private CLBuffer<IntBuffer> weightsBuffer;
    private CLBuffer<IntBuffer> distancesBuffer;
    private CLBuffer<IntBuffer> foodSourcesBuffer;
    private CLBuffer<IntBuffer> costFunctionValuesBuffer;
    private CLBuffer<IntBuffer> triesBuffer;

    private Solution bestSolution;
    private Instant algorithmSetup, beforeCriterionCheck, beforeEmployeePhase, beforeOnlookerPhase, beforeScoutPhase, beforeUpdatingPhase, afterUpdatingState;
    private TimeStatistics statistics;
    private  boolean debugModeEnabled = true;

    private final String programName = "qap_abc.cl";

    public ParallelAbcAlgorithm(ABCAlgorithmParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public Solution run() {
        algorithmSetup = Instant.now();
        debug("Initializing food sources");
        initFoodSources();
        beforeCriterionCheck = Instant.now();
        statistics.updateInitStatistics(algorithmSetup, beforeCriterionCheck);
        for (int i = 0; i < parameters.getMaxIterations(); i++) {
            beforeEmployeePhase = Instant.now();
            if(debugModeEnabled) {
                System.out.println("fitness from gpu = " + Arrays.toString(parallelComputingInstance.readBufferToArray(costFunctionValuesBuffer)));
                System.out.println("employedBeesPhase");
            }
            employedBeesPhase();
            if(debugModeEnabled) {
                System.out.println("fitness from gpu = " + Arrays.toString(parallelComputingInstance.readBufferToArray(costFunctionValuesBuffer)));
                System.out.println("food sources = " + Arrays.toString(parallelComputingInstance.readBufferToArray(foodSourcesBuffer)));
            }
            beforeOnlookerPhase = Instant.now();
            debug("onlookerBeesPhase");
            //onlookerBeesPhase();
            onlookerBeePhase();
            debug("beforeUpdateBest");
            beforeScoutPhase = Instant.now();
            updateBest();
            debug("beforeScoutPhase");
            scoutBeesPhase();
            beforeUpdatingPhase = Instant.now();
            debug("afterScoutPhase");

            if(debugModeEnabled) {
                String costFunctionValuesAsString = Arrays.toString(parallelComputingInstance.readBufferToArray(costFunctionValuesBuffer));
                System.out.println("employedBeesPhase");
                System.out.println("fitness from gpu = " + costFunctionValuesAsString);
            }
            /*
            updateAlgorithmState();
            afterUpdatingState = Instant.now();
            statistics.updateEveryTurnStatistics(beforeCriterionCheck, beforeEmployeePhase, beforeOnlookerPhase, beforeScoutPhase, beforeUpdatingPhase, afterUpdatingState);
            beforeCriterionCheck = afterUpdatingState;*/
        }
        Solution result = updateBest();

        releaseMemory();
        return result;
        //return algorithmState.getBestResultFound();
    }

    @Override
    public void releaseMemory() {
        costFunctionValuesBuffer.release();
        distancesBuffer.release();
        foodSourcesBuffer.release();
        triesBuffer.release();
        weightsBuffer.release();

    }

    private void debug(String s) {
        if(debugModeEnabled){
            System.out.println(s);
        }

    }


    /*private Solution evaluateBest() {
        int[] foodSources = parallelComputingInstance.readBufferToArray(foodSourcesBuffer);
        int[] fitnesses = parallelComputingInstance.readBufferToArray(costFunctionValuesBuffer);
        int best = Integer.MAX_VALUE;
        int iBest = -1;
        for (int i =0; i< fitnesses.length; i++) {
            int fit = fitnesses[i];
            System.out.print(fit+" ");
            if(fit<best) {
                best = fit;
                iBest = i;
            }
        }
        System.out.println();
        System.out.println("best = " + best);
        ArrayList<Integer> facilitiesMapping = new ArrayList<>(problemInstance.getProblemSize());
        for(int i = iBest*problemInstance.getProblemSize();i<(iBest+1)*problemInstance.getProblemSize();i++){
            facilitiesMapping.add(foodSources[i]);
        }
        Solution sol = new Solution(facilitiesMapping, problemInstance);
        return sol;
    }*/

    @Override
    public void init(long seed) {
        parallelComputingInstance = new ParallelComputingInstance(programName);
        parallelComputingInstance.init();
        random = new Random(seed);
        initBuffers();
        statistics= new TimeStatistics(5);
    }

    private void initBuffers() {
        weightsBuffer = parallelComputingInstance.createBufferFrom2DArray(problemInstance.getWeights(), CLMemory.Mem.READ_ONLY);
        distancesBuffer = parallelComputingInstance.createBufferFrom2DArray(problemInstance.getDistances(), CLMemory.Mem.READ_ONLY);
        foodSourcesBuffer = parallelComputingInstance.createIntBuffer(parameters.getFoodSourcesNumber() * problemInstance.getProblemSize(), CLMemory.Mem.READ_WRITE);
        costFunctionValuesBuffer = parallelComputingInstance.createIntBuffer(parameters.getFoodSourcesNumber(), CLMemory.Mem.READ_WRITE);

        int[] triesArray = new int[parameters.getFoodSourcesNumber()];
        for (int i = 0; i < triesArray.length; i++) {
            triesArray[i] = 0;
        }
        triesBuffer = parallelComputingInstance.createBufferFrom1DArray(triesArray, CLMemory.Mem.READ_WRITE);

        parallelComputingInstance.writeBuffer(weightsBuffer);
        parallelComputingInstance.writeBuffer(distancesBuffer);
        parallelComputingInstance.writeBuffer(triesBuffer);
    }


    private void initFoodSources() {
        int foodSourcesNumber = parameters.getFoodSourcesNumber();

        parallelComputingInstance.runDurstenfeldAlgorithmKernel(foodSourcesBuffer, foodSourcesNumber, problemInstance.getProblemSize(), 555);
        //parallelComputingInstance.finish();
        parallelComputingInstance.evaluateCostFunctionValues(weightsBuffer, distancesBuffer, foodSourcesBuffer, costFunctionValuesBuffer, foodSourcesNumber, problemInstance.getProblemSize(), false);
    }

    private void employedBeesPhase() {
        //yyyy? chyba do usuniecia
/*        if (foodSourcesBuffer.getBuffer().remaining() == 0) {
            addFoodSourcesIntoBuffer(foodSources, foodSourcesBuffer);
        }*/
        foodSourcesBuffer = parallelComputingInstance.runEmployeeBeeParallel(weightsBuffer, distancesBuffer, foodSourcesBuffer, costFunctionValuesBuffer, triesBuffer, parameters.getFoodSourcesNumber(), problemInstance.getProblemSize(), random.nextInt());
        parallelComputingInstance.finish();
        /*for (FoodSource foodSource : foodSources) {
            Solution mutatedSolution = searchInNeighbourhood(foodSource.getSolution());
            if (mutatedSolution.isBetterThan(foodSource.getSolution())) {
                foodSource.setSolution(mutatedSolution);
            }*//* else {
                foodSource.incrementTrials();
            }*//*
        }*/

        //foodSources = convertBufferIntoFoodSources( foodSourcesAsBuffer);
    }

    private void addFoodSourcesIntoBuffer(List<FoodSource> foodSources, CLBuffer<IntBuffer> foodSourcesAsBuffer) {
        IntBuffer buffer = foodSourcesAsBuffer.getBuffer();
        buffer.rewind();
        for (FoodSource foodSource : foodSources) {
            ArrayList<Integer> facilitiesMapping = foodSource.getSolution().getFacilitiesMapping();
            for (int integer : facilitiesMapping) {
                buffer.put(integer);
            }
        }
    }



    private void onlookerBeesPhaseWholeOnGPU() {
        parallelComputingInstance.runOnlookerBeesPhaseWholeOnGPU(weightsBuffer, distancesBuffer, foodSourcesBuffer, costFunctionValuesBuffer, triesBuffer, parameters.getFoodSourcesNumber(), problemInstance.getProblemSize(), parameters.getOnlookersNumber(), random.nextInt());

    }
    private void onlookerBeePhase() {
        switch (parameters.getOnlookersMethod()){
            case ELITE_SELECTION:
                parallelComputingInstance.runOnlookerBeesPhaseWithEliteSelection(weightsBuffer, distancesBuffer, foodSourcesBuffer, costFunctionValuesBuffer, triesBuffer, parameters.getFoodSourcesNumber(), problemInstance.getProblemSize(), parameters.getOnlookersNumber(), random.nextInt());

                break;
            case ORIGINAL:
                parallelComputingInstance.runOnlookerBeesPhaseWholeOnGPU(weightsBuffer, distancesBuffer, foodSourcesBuffer, costFunctionValuesBuffer, triesBuffer, parameters.getFoodSourcesNumber(), problemInstance.getProblemSize(), parameters.getOnlookersNumber(), random.nextInt());

                break;
            case EMPLOYEE_BEE_VERSION:
                parallelComputingInstance.runEmployeeBeeParallel(weightsBuffer,distancesBuffer,foodSourcesBuffer,costFunctionValuesBuffer,triesBuffer,parameters.getFoodSourcesNumber(),problemInstance.getProblemSize(),random.nextInt());
                break;
            case NONE:
                break;
        }
        parallelComputingInstance.finish();
    }



    private Solution updateBest() {
        int foodSourcesNumber = parameters.getFoodSourcesNumber();
        int maxWorkGroupSize = parallelComputingInstance.getDevice().getMaxWorkGroupSize();
        int workGroupSize;//= parallelComputingInstance.roundUp(parallelComputingInstance.getMaxWorkGroupSize(),foodSourcesNumber);
        int workGroups;// = workGroupSize parallelComputingInstance.getMaxWorkGroupSize();
        if (maxWorkGroupSize >= foodSourcesNumber) {
            workGroupSize = maxWorkGroupSize;
            while (workGroupSize >= foodSourcesNumber) {
                workGroupSize /= 2;
            }
            workGroups = 1;
        } else {
            workGroupSize = maxWorkGroupSize;
            workGroups = ParallelComputingInstance.roundUp(workGroupSize * 2, foodSourcesNumber) / (workGroupSize * 2);
        }


        int[] foodSourcesArr = parallelComputingInstance.readBufferToArray(foodSourcesBuffer);
        int[] costFunctionValuesFromCpu = new int[foodSourcesNumber];
        for (int i = 0; i < foodSourcesNumber; i++) {
            ArrayList<Integer> subArray = new ArrayList<>(IntStream.of(Arrays.copyOfRange(foodSourcesArr, i * problemInstance.getProblemSize(), (i + 1) * problemInstance.getProblemSize())).boxed().collect(Collectors.toList()));
            costFunctionValuesFromCpu[i] = problemInstance.evaluateResult(subArray);
        }
        //System.out.println("cos function from cpu = " + Arrays.toString(costFunctionValuesFromCpu));


        CLBuffer<IntBuffer> bestCostFunctionValuesInGroups = parallelComputingInstance.createIntBuffer(workGroups, CLMemory.Mem.WRITE_ONLY);
        CLBuffer<IntBuffer> bestCostFunctionValuesGlobalIndexes = parallelComputingInstance.createIntBuffer(workGroups, CLMemory.Mem.WRITE_ONLY);

        parallelComputingInstance.runFindBestReduction(costFunctionValuesBuffer, bestCostFunctionValuesInGroups, bestCostFunctionValuesGlobalIndexes, foodSourcesNumber, workGroupSize, workGroups);
        parallelComputingInstance.finish();
        int[] fitArray = new int[workGroups];
        int[] fitIndexArray = new int[workGroups];
        bestCostFunctionValuesInGroups.getBuffer().get(fitArray);
        bestCostFunctionValuesGlobalIndexes.getBuffer().get(fitIndexArray);
        boolean shouldLoadBest = false;
        int bestIndex = -1;
        int bestFit = bestSolution != null ? bestSolution.getEvaluatedResult() : Integer.MAX_VALUE;
        for (int i = 0; i < workGroups; i++) {
            if(debugModeEnabled){
                System.out.println("i = " + i + " fit = " + fitArray[i] + " index = " + fitIndexArray[i]);
            }
            if (fitArray[i] < bestFit) {
                System.out.println("i = " + i + " fit = " + fitArray[i] + " index = " + fitIndexArray[i]);
                bestFit = fitArray[i];
                bestIndex = fitIndexArray[i];
                shouldLoadBest = true;
            }
        }

        if (shouldLoadBest) {
            int[] bestSolutionArray = readOneSolution(bestIndex, problemInstance.getProblemSize());

            bestSolution = new Solution(new ArrayList(IntStream.of(bestSolutionArray).boxed().collect(Collectors.toList())), problemInstance);
            System.out.println("bestSolution = " + bestSolution.toString());

            System.out.println("fitness from gpu = " + Arrays.toString(parallelComputingInstance.readBufferToArray(costFunctionValuesBuffer)));

        }
        return bestSolution;
    }

    private int[] readOneSolution(int index, int problemSize) {
        int startIndex = index * problemInstance.getProblemSize();
        int offset = 0;
        long addrAlignInBytes = parallelComputingInstance.getDevice().getMemBaseAddrAlign();
        long addrAlign = addrAlignInBytes / foodSourcesBuffer.getElementSize();
        while (offset <= startIndex) {
            offset += addrAlign;
        }
        offset -= addrAlign;
        int size = startIndex + problemSize - offset;

        CLSubBuffer<IntBuffer> readData = foodSourcesBuffer.createSubBuffer(offset, size, CLMemory.Mem.READ_ONLY);
        parallelComputingInstance.readBuffer(readData);

        int[] bestSolutionArray = new int[problemInstance.getProblemSize()];

        ((IntBuffer) readData.getBuffer().position(startIndex - offset)).slice().get(bestSolutionArray);

        return bestSolutionArray;
    }


    private void scoutBeesPhase() {
        parallelComputingInstance.runScoutsPhaseParallel(foodSourcesBuffer, triesBuffer, parameters.getFoodSourceTrialsLimit(), parameters.getFoodSourcesNumber(), problemInstance.getProblemSize(), random.nextInt());
        parallelComputingInstance.evaluateCostFunctionValues(weightsBuffer, distancesBuffer, foodSourcesBuffer, costFunctionValuesBuffer, parameters.getFoodSourcesNumber(), problemInstance.getProblemSize(), false);
    }


    private Solution searchInNeighbourhood(Solution oldSolution) {
        Solution solution = new Solution((ArrayList) oldSolution.getFacilitiesMapping().clone(), oldSolution.getProblem());
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
    public TimeStatistics getStatistics() {
        return statistics;
    }

    @Override
    public void setProblem(ProblemInstance problem) {
        this.problemInstance = problem;
    }


    public List<FoodSource> convertBufferIntoFoodSources(CLBuffer<IntBuffer> foodSourcesAsBuffer) {
        IntBuffer buffer = foodSourcesAsBuffer.getBuffer();
        //System.out.println(buffer.remaining());
        buffer.rewind();
        int foodSourcesNumber = parameters.getFoodSourcesNumber();
        List<FoodSource> foodSources = new ArrayList<>(foodSourcesNumber);
        for (int i = 0; i < foodSourcesNumber; i++) {
            FoodSource foodSource = new FoodSource();
            ArrayList<Integer> mapping = new ArrayList<>(problemInstance.getProblemSize());
            for (int j = 0; j < problemInstance.getProblemSize(); j++) {
                int elem = buffer.get();
                //System.out.print(elem+" ");
                mapping.add(elem);
            }
            //System.out.println();
            foodSource.setSolution(new Solution(mapping, problemInstance));
            foodSources.add(foodSource);
        }
        return foodSources;
    }


    private Solution createRandomSolution() {
        ArrayList<Integer> mapping = new ArrayList<>(problemInstance.getProblemSize());
        for (int i = 0; i < problemInstance.getProblemSize(); i++) {
            mapping.add(i);
        }
        Collections.shuffle(mapping, random);
        return new Solution(mapping, problemInstance);

    }


}
