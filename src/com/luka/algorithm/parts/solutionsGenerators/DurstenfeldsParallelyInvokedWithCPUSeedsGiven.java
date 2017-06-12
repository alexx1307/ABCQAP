package com.luka.algorithm.parts.solutionsGenerators;

import com.jogamp.opencl.*;
import com.luka.qap.ProblemInstance;
import com.luka.qap.Solution;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Created by lukas on 08.04.2017.
 */
public class DurstenfeldsParallelyInvokedWithCPUSeedsGiven implements ISolutionsGenerator {
    private static final Logger LOGGER = Logger.getLogger("myLogger");
    private static final String DURSTENFELD_KERNEL_NAME = "DurstenfeldAlgorithm.cl";
    private static final String CL_EXTENSION = ".cl";

    private static Random rnd = new Random();
    private ProblemInstance problemInstance;

    private CLContext context;
    private CLCommandQueue queue;
    private CLProgram program;

    public DurstenfeldsParallelyInvokedWithCPUSeedsGiven(ProblemInstance problemInstance, CLContext context, CLCommandQueue queue) {
        this.problemInstance = problemInstance;
        this.context = context;

        this.queue = queue;
    }


    @Override
    public List<Solution> generateSolutions(int n) {

        ArrayList<Integer> seeds = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            seeds.add(rnd.nextInt());
        }
        List<ArrayList<Integer>> permutations = getPermutationsViaDurstenfeldsAlgorithmParallely(n, seeds);
        return createSolutions(permutations);
    }

    private List<Solution> createSolutions(List<ArrayList<Integer>> permutations) {
        ArrayList<Solution> solutions = new ArrayList<>(permutations.size());

        for (ArrayList<Integer> permutation : permutations) {
            solutions.add(new Solution(permutation, problemInstance));
        }
        return solutions;
    }

    private List<ArrayList<Integer>> getPermutationsViaDurstenfeldsAlgorithmParallely(int n, ArrayList<Integer> seeds) {
        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(DURSTENFELD_KERNEL_NAME);

        IntBuffer buffer = IntBuffer.allocate(n*problemInstance.getProblemSize());
        fillBufferWithUnshuffledData(buffer,n);
        CLBuffer clBuffer = context.createBuffer(buffer, CLMemory.Mem.READ_WRITE);
        kernel.putArgs(clBuffer);
        kernel.putArg(n);
        kernel.putArg(problemInstance.getProblemSize());

        //queue.put1DRangeKernel(kernel,)
        return new ArrayList<>();
    }

    private void fillBufferWithUnshuffledData(IntBuffer buffer, int n) {
        for(int i = 0; i< n; i++){
            for(int j = 0; j<problemInstance.getProblemSize(); j++){
                buffer.put(j);
            }
        }
    }

    private CLProgram getProgram() {
        if (program == null) {
            LOGGER.info("Initializing " + DURSTENFELD_KERNEL_NAME);
            program = context.createProgram(DURSTENFELD_KERNEL_NAME + CL_EXTENSION).build();
        }
        return program;
    }

}
