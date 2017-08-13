package com.luka.algorithm;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLMemory;
import com.luka.algorithm.parallel.FullGPUAbcAlgorithm;
import com.luka.algorithm.parallel.ParallelAbcAlgorithm;
import com.luka.algorithm.parallel.ParallelComputingInstance;
import com.luka.algorithm.selection.ISelectionStrategy;
import com.luka.algorithm.selection.RouletteWheelSelectionStrategyImpl;
import com.luka.algorithm.stopCriterion.IStopCriterion;
import com.luka.algorithm.stopCriterion.IterationStopCriterion;
import com.luka.qap.IAlgorithm;
import com.luka.qap.ProblemInstance;
import com.luka.qap.Solution;
import org.junit.Assert;
import org.junit.Test;

import java.nio.IntBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;

/**
 * Created by lukas on 24.04.2017.
 */
public class CasesRunner {

    private long seed = 20;
    Random random = new Random(seed);
    private int maxIterationNumber = 10000;
    private int onlookersNumber = 50;
    private int foodSourcesNumber = 100000;
    private int foodSourcesTrialsLimit = 50;
    private short useReductionToFindBest = 1;

    private int[][] weights = new int[][]{
            { 0, 2, 1, 5, 3},
            { 2, 0, 2, 0, 2},
            { 1, 2, 0, 4, 0},
            { 5, 0, 4, 0, 1},
            { 3, 2, 0, 1, 0}
    };
    private int[][] distances =  new int[][]{
            {0, 10, 5, 2, 15},
            {10, 0, 2, 100, 2},
            {5, 2, 0, 10, 5},
            {2, 100, 10, 0, 20},
            {15, 2, 5, 20, 0}
    };

    @Test
    public void simpleComparisonRun() throws Exception {

        ABCAlgorithmParameters params = getAbcAlgorithmParameters();

        ProblemInstance problem = ProblemInstance.ProblemFactory.createProblemFromArrays( weights,distances);

        ESelectionMethod onlookerMethods[] = {ESelectionMethod.ELITE_SELECTION, ESelectionMethod.EMPLOYEE_BEE_VERSION, ESelectionMethod.ORIGINAL, ESelectionMethod.NONE};//{ESelectionMethod.EMPLOYEE_BEE_VERSION, ESelectionMethod.NONE,ESelectionMethod.ELITE_SELECTION};

        int parallelTries = 0;
        int fullParallelTries = 10;
        int normalTries =10 ;
        for (int i = 0; i < fullParallelTries; i++) {
            IAlgorithm algorithm = new FullGPUAbcAlgorithm(params);
            algorithm.setProblem(problem);
            params.setUseReductionToFindBest((short)1);
            params.setOnlookerMethod(ESelectionMethod.ORIGINAL);

            Instant start = Instant.now();
            algorithm.init(321);
            Instant afterInit = Instant.now();
            Solution solution = algorithm.run();
            Instant afterAll = Instant.now();
            System.out.print("init: "+ Duration.between(start,afterInit).toMillis());
            System.out.print(" algorithm: "+ Duration.between(afterInit,afterAll).toMillis());
            System.out.print(" onlooker method: "+ params.getOnlookersMethod());
            System.out.print(" use reduction to find best: "+ params.getUseReductionToFindBest());
            System.out.print(" total: "+ Duration.between(start,afterAll).toMillis());
            System.out.println(solution.toString());
        }

        for (int i = 0; i < parallelTries; i++) {
            IAlgorithm algorithm = new ParallelAbcAlgorithm(params);
            params.setUseReductionToFindBest((short)(i<5?1:0));
            params.setOnlookerMethod(ESelectionMethod.ELITE_SELECTION);
            algorithm.setProblem(problem);
            Instant start = Instant.now();
            algorithm.init(321);
            Instant afterInit = Instant.now();
            Solution solution = algorithm.run();
            Instant afterAll = Instant.now();
            System.out.println("init: "+ Duration.between(start,afterInit).toMillis());
            System.out.println(" algorithm: "+ Duration.between(afterInit,afterAll).toMillis());
            System.out.println(" onlooker method: "+ params.getOnlookersMethod());
            System.out.println(" use reduction to find best: "+ params.getUseReductionToFindBest());
            System.out.println(" total: "+ Duration.between(start,afterAll).toMillis());
            System.out.println(solution.toString());
            algorithm.releaseMemory();
        }


        for (int i = 0; i < normalTries; i++) {
            ABCAlgorithm algorithm = new SequentionalAbcAlgorithm(params);
            algorithm.setProblem(problem);
            Instant start = Instant.now();
            algorithm.init(321);
            Instant afterInit = Instant.now();
            Solution solution = algorithm.run();
            Instant afterAll = Instant.now();
            System.out.print("init: "+ Duration.between(start,afterInit).toMillis());
            System.out.print(" algorithm: "+ Duration.between(afterInit,afterAll).toMillis());
            System.out.print(" total: "+ Duration.between(start,afterAll).toMillis());
            System.out.println(solution.toString());
        }

    }

    @Test
    public void fitnessEvaluationTest() {
        String programName = "qap_abc.cl";
        ParallelComputingInstance instance = new ParallelComputingInstance(programName);
        instance.init();
        CLContext context = instance.getContext();

        ABCAlgorithmParameters parameters = getAbcAlgorithmParameters();
        ProblemInstance problem = ProblemInstance.ProblemFactory.createProblemFromArrays( weights,distances);
        SequentionalAbcAlgorithm seqAlgorithm = new SequentionalAbcAlgorithm(parameters);
        seqAlgorithm.setProblem(problem);
        seqAlgorithm.init(141);

        seqAlgorithm.initFoodSources();

        List<FoodSource> foodSources = seqAlgorithm.getFoodSources();
        int[] facilitiesMappings1DArray = foodSources.stream().flatMapToInt(fs -> fs.getSolution().getFacilitiesMapping().stream().mapToInt(Integer::intValue))
                .toArray();
        CLBuffer<IntBuffer> weightsBuffer = createBufferFrom2DArray(weights, context,READ_ONLY);
        CLBuffer<IntBuffer> distancesBuffer = createBufferFrom2DArray(distances, context,READ_ONLY);
        CLBuffer<IntBuffer> facilitiesMappingBuffer = createBufferFrom1DArray(facilitiesMappings1DArray, context,READ_ONLY);
        CLBuffer<IntBuffer> outArrayBuffer = context.createIntBuffer(foodSourcesNumber, WRITE_ONLY );

        instance.writeBuffer(weightsBuffer);
        instance.writeBuffer(distancesBuffer);
        instance.writeBuffer(facilitiesMappingBuffer);

        CLBuffer<IntBuffer> resultBuffer = instance.evaluateCostFunctionValues(weightsBuffer, distancesBuffer, facilitiesMappingBuffer, outArrayBuffer, foodSourcesNumber, problem.getProblemSize(), true);
        int[] fitnesses = new int[resultBuffer.getBuffer().limit()];
        resultBuffer.getBuffer().get(fitnesses);

        int[] expected = foodSources.stream().map(fs -> fs.getSolution().getEvaluatedResult()).mapToInt(Integer::intValue).toArray();

        for (int i = 0; i < expected.length; i++) {
            System.out.println("expected " + expected[i]+", and was "+fitnesses[i]);
            Assert.assertEquals(expected[i],fitnesses[i]);
        }
    }

    private CLBuffer<IntBuffer> createBufferFrom1DArray(int[] array, CLContext context, CLMemory.Mem mem) {
        CLBuffer<IntBuffer> clBuffer = context.createIntBuffer(array.length , mem );
        for (int anInt : array) {
            clBuffer.getBuffer().put(anInt);
        }
        clBuffer.getBuffer().rewind();
        return clBuffer;
    }

    private CLBuffer<IntBuffer> createBufferFrom2DArray(int[][] array, CLContext context, CLMemory.Mem mem) {
        CLBuffer<IntBuffer> clBuffer = context.createIntBuffer(array.length * array.length, mem );
        for (int[] ints : array) {
            for (int anInt : ints) {
                clBuffer.getBuffer().put(anInt);
            }

        }
        clBuffer.getBuffer().rewind();
        return clBuffer;
    }

    private ABCAlgorithmParameters getAbcAlgorithmParameters() {
        ISelectionStrategy selectionStrategy = new RouletteWheelSelectionStrategyImpl(random);
        IStopCriterion stopCriterion = new IterationStopCriterion(maxIterationNumber);

        ABCAlgorithmParameters params = new ABCAlgorithmParameters();
        params.setSelectionStrategy(selectionStrategy);
        params.setMaxIterations(maxIterationNumber);
        params.setOnlookersNumber(onlookersNumber);
        params.setFoodSourcesNumber(foodSourcesNumber);
        params.setFoodSourceTrialsLimit(foodSourcesTrialsLimit);
        params.setUseReductionToFindBest(useReductionToFindBest);
        return params;
    }
}