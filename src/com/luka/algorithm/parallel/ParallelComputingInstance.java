package com.luka.algorithm.parallel;

import com.jogamp.opencl.*;
import com.luka.Main;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import static java.lang.Math.min;

/**
 * Created by lukas on 24.04.2017.
 */
public class ParallelComputingInstance {
    private static final Logger LOGGER = Logger.getLogger("myLogger");
    private static final String DURSTENFELD_KERNEL_NAME = "durstenfeldAlgorithm";
    private static final String GENERATE_PSUDORANDOM_KERNEL_NAME = "generatePseudorandom";
    //private static final String PROGRAM_PATH = "qap_abc2.cl";
    private static final String EMPLOYEE_BEE_PHASE_KERNEL = "employeeBeePhase";
    private static final String SCOUT_BEE_PHASE_KERNEL = "scoutBeePhase";
    private static final String TEST_INCREMENT_KERNEL = "testIncrementkernel";
    private static final String EVALUATE_COST_FUNCTION_VALUE_KERNEL = "evaluateCostFunctionValues";
    private static final String FIND_BEST_REDUCTION_KERNEL = "findBestReduction";
    private static final String ONLOOKER_BEE_PHASE_WHOLE_ON_GPU_KERNEL = "onlookerBeePhaseOnGPU";
    private static final String ONLOOKER_BEE_PHASE_WITH_ELITE_SELECTION = "onlookerBeePhaseWithEliteSelection";


    private static final String WHOLE_ALGORITHM_KERNEL = "wholeAlgorithmKernel";

    private CLContext context;
    private CLCommandQueue queue;
    private CLProgram program;
    private CLDevice device;
    private String programName;

    public ParallelComputingInstance(String programName) {

        this.programName = programName;
    }

    public void init() {
        this.context = CLContext.create();
        CLDevice[] devices = context.getDevices();
        LOGGER.info("Available Devices " + devices.length + ":");
        for (CLDevice device : devices) {
            LOGGER.info("dev: " + device.getVendor() + ", " + device.toString());
        }

        device = context.getMaxFlopsDevice(CLDevice.Type.GPU);
        LOGGER.info("Chosen device: " + device.getName());

        CLCommandQueue.Mode mode = CLCommandQueue.Mode.PROFILING_MODE;
        this.queue = device.createCommandQueue(mode);
        long localMemSize = device.getLocalMemSize();
        long globalMemSize = device.getGlobalMemSize();
        long maxWorkGroupSize = device.getMaxWorkGroupSize();
        long maxComputeUnits = device.getMaxComputeUnits();
        LOGGER.info("localMemSize:" + localMemSize);
        LOGGER.info("globalMemSize:" + globalMemSize);
        LOGGER.info("maxWorkGroupSize:" + maxWorkGroupSize);
        LOGGER.info("maxComputeUnits:" + maxComputeUnits);
    }

    public CLBuffer<IntBuffer> getNPseudoRandomInts(int n, long seed) {
        int localWorkSize = min(device.getMaxWorkGroupSize(), 256);  // Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, n);   // rounded up to the nearest multiple of the localWorkSize


        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(GENERATE_PSUDORANDOM_KERNEL_NAME);

        CLBuffer<IntBuffer> clBuffer = context.createIntBuffer(n, CLMemory.Mem.WRITE_ONLY);
        kernel.putArgs(clBuffer);
        kernel.putArg(n);
        kernel.putArg(seed);

        queue.put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize);
        queue.putReadBuffer(clBuffer, true);
        return clBuffer;
    }

    public static int roundUp(int groupSize, int size) {
        int r = size % groupSize;
        if (r == 0) {
            return size;
        } else {
            return size + groupSize - r;
        }
    }

    public void runDurstenfeldAlgorithmKernel(CLBuffer<IntBuffer> facilitiesMappingOut, int permutationsNumber, int permutationLength, long seed) {
        int localMemSize = (int)device.getLocalMemSize();

        int localWorkSize =  determineWorkGroupSizeForDurstenfeld(device.getMaxWorkGroupSize(), permutationLength, localMemSize);  // Local work size dimensions
        localWorkSize/=2;
        int globalWorkSize = roundUp(localWorkSize, permutationsNumber);   // rounded up to the nearest multiple of the localWorkSize

        //System.out.println("localWorkSize:"+localWorkSize);
        //System.out.println("globalWorkSize:"+globalWorkSize);
        //System.out.println("localMemSize:"+localMemSize);
        //System.out.println("permutationLength = " + permutationLength);
        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(DURSTENFELD_KERNEL_NAME);
        //CLEventList list = new CLEventList(1);
        kernel.putArgs(facilitiesMappingOut);
        kernel.putArg(permutationsNumber);
        kernel.putArg(permutationLength);
        kernel.putArg(seed);
        kernel.putNullArg(localWorkSize * permutationLength * 4);
        queue.put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize);//,list);

/*        long start = list.getEvent(0).getProfilingInfo(CLEvent.ProfilingCommand.START);
        long end = list.getEvent(0).getProfilingInfo(CLEvent.ProfilingCommand.END);
        long duration = end - start; // time in nanoseconds
        System.out.println("Kernel time in ns:"+duration);*/
    }

    private int determineWorkGroupSizeForDurstenfeld(int maxWorkGroupSize, int permLength, int localMemorySize) {
        //dla 33
        //dla 32 : 32*256 + 32*4


        //perItem (7+permLength)*4*256
        //int localMemoryPerItem = 7*4;
        for(int i = maxWorkGroupSize; i>0;i/=2){
            int localMemoryPerGroup = i*permLength*4;

            int sum = localMemoryPerGroup;// + i*localMemoryPerItem;
            if(sum<=localMemorySize){
                return i/2;
            }
        }
        return 0;
    }

    public CLBuffer<IntBuffer> runEmployeeBeeParallel(CLBuffer<IntBuffer> weightsBuffer,CLBuffer<IntBuffer> distancesBuffer,CLBuffer<IntBuffer> foodSourcesAsBuffer, CLBuffer<IntBuffer> costFunctionValuesBuffer,CLBuffer<IntBuffer> triesBuffer,int foodSourcesNumber, int permutationLength,long seed) {
        int localWorkSize = min(device.getMaxWorkGroupSize(), 128);  // Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, foodSourcesNumber);   // rounded up to the nearest multiple of the localWorkSize
        long localMemSize = device.getLocalMemSize();
        /*System.out.println("localWorkSize:"+localWorkSize);
        System.out.println("globalWorkSize:"+globalWorkSize);
        System.out.println("localMemSize:"+localMemSize);*/
        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(EMPLOYEE_BEE_PHASE_KERNEL);
        kernel.putArgs(weightsBuffer,distancesBuffer,foodSourcesAsBuffer,costFunctionValuesBuffer,triesBuffer);
        kernel.putArg(foodSourcesNumber);
        kernel.putArg(permutationLength);
        kernel.putArg(seed);

        queue.put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize);

        return foodSourcesAsBuffer;
    }

    private CLProgram getProgram() {
        if (program == null) {
            try {
                program = context.createProgram(Main.class.getResourceAsStream(programName)).build();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return program;
    }

    public CLBuffer<IntBuffer> runTestIncrementKernel(CLBuffer<IntBuffer> buffer, int permNumber, int permLength, boolean readBuffer, boolean writeBuffer) {
        int localWorkSize = device.getMaxWorkGroupSize();  // Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, permNumber*permLength);   // rounded up to the nearest multiple of the localWorkSize

        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(TEST_INCREMENT_KERNEL);
        kernel.putArgs(buffer);
        kernel.putArg(permNumber*permLength);
        if(writeBuffer) {
            queue.putWriteBuffer(buffer, false);
        }
        queue.put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize);
        if(readBuffer) {
            queue.putReadBuffer(buffer, true);
        }
        return buffer;
    }
    public CLBuffer<IntBuffer> evaluateCostFunctionValues(CLBuffer<IntBuffer> weights, CLBuffer<IntBuffer> distances, CLBuffer<IntBuffer> facilitiesMapping, CLBuffer<IntBuffer> fitnessArrayOut, int permNumber, int permLength, boolean readBuffer) {
        int localWorkSize = device.getMaxWorkGroupSize();  // Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, permNumber);   // rounded up to the nearest multiple of the localWorkSize

        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(EVALUATE_COST_FUNCTION_VALUE_KERNEL);
        kernel.putArgs(weights,distances,facilitiesMapping,fitnessArrayOut);
        kernel.putArg(permNumber);
        kernel.putArg(permLength);
/*        if(writeBuffers) {
            queue.putWriteBuffer(weights, false);
            queue.putWriteBuffer(distances, false);
            queue.putWriteBuffer(facilitiesMapping, false);
        }*/
        queue.put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize);
        if(readBuffer) {
            queue.putReadBuffer(fitnessArrayOut, true);
        }
        return fitnessArrayOut;
    }

    public CLBuffer<IntBuffer> createBufferFrom1DArray(int[] array, CLMemory.Mem mem) {
        CLBuffer<IntBuffer> clBuffer = context.createIntBuffer(array.length , mem );
        for (int anInt : array) {
            clBuffer.getBuffer().put(anInt);
        }
        clBuffer.getBuffer().rewind();
        return clBuffer;
    }

    public CLBuffer<IntBuffer> createBufferFrom2DArray(int[][] array, CLMemory.Mem mem) {
        CLBuffer<IntBuffer> clBuffer = context.createIntBuffer(array.length * array.length, mem );
        for (int[] ints : array) {
            for (int anInt : ints) {
                clBuffer.getBuffer().put(anInt);
            }

        }
        clBuffer.getBuffer().rewind();
        return clBuffer;
    }

    public CLBuffer<IntBuffer> createIntBuffer(int size, CLMemory.Mem mem) {
        return context.createIntBuffer(size, mem );

    }

    public CLContext getContext() {
        return context;
    }

    public int[] readBufferToArray(CLBuffer<IntBuffer> buffer) {
        CLEventList eventList = new CLEventList(1);
        System.out.println("before read");
        queue.putReadBuffer(buffer,true, eventList);
        System.out.println("read status: "+eventList.getEvent(0).getStatus());
        int[] result = new int[buffer.getBuffer().limit()];
        buffer.getBuffer().get(result);
        buffer.getBuffer().rewind();
        return result;
    }
    public void writeBuffer(CLBuffer<IntBuffer> buffer) {
        queue.putWriteBuffer(buffer,false);
    }

    public void finish() {
        queue.finish();
    }

    public void runScoutsPhaseParallel(CLBuffer<IntBuffer> foodSourcesBuffer, CLBuffer<IntBuffer> triesBuffer, int foodSourceTrialsLimit, int foodSourcesNumber, int permutationLength, long seed) {
        int localMemSize = (int)device.getLocalMemSize();

        int localWorkSize =  determineWorkGroupSizeForDurstenfeld(device.getMaxWorkGroupSize(), permutationLength, localMemSize);  // Local work size dimensions
        localWorkSize/=2;
        int globalWorkSize = roundUp(localWorkSize, foodSourcesNumber);   // rounded up to the nearest multiple of the localWorkSize

       /* System.out.println("localWorkSize:"+localWorkSize);
        System.out.println("globalWorkSize:"+globalWorkSize);
        System.out.println("localMemSize:"+localMemSize);
        System.out.println("permutationLength = " + permutationLength);
*/        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(SCOUT_BEE_PHASE_KERNEL);
        //CLEventList list = new CLEventList(1);
        kernel.putArg(foodSourcesBuffer);
        kernel.putArg(triesBuffer);
        kernel.putArg(foodSourceTrialsLimit);
        kernel.putArg(foodSourcesNumber);
        kernel.putArg(permutationLength);
        kernel.putArg(seed);


        kernel.putNullArg(localWorkSize * permutationLength * 4);
        queue.put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize);//,list);
    }

    public void readBuffer(CLBuffer<IntBuffer> permBuffer) {
        queue.putReadBuffer(permBuffer,true);
    }

    public int getMaxWorkGroupSize() {
        return device.getMaxWorkGroupSize();
    }

    public void runFindBestReduction(CLBuffer<IntBuffer> fitnessArrayBuffer, CLBuffer<IntBuffer> bestFitnesses, CLBuffer<IntBuffer> bestGlobalIndex, int foodSourcesNumber, int workGroupSize, int workGroups) {
        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(FIND_BEST_REDUCTION_KERNEL);
        kernel.putArgs(fitnessArrayBuffer,bestFitnesses,bestGlobalIndex);
        kernel.putArg(foodSourcesNumber);
        kernel.putNullArg(workGroupSize*4);  //localWorkSize/2 * 4
        kernel.putNullArg(workGroupSize*4);  //localWorkSize/2 * 4
        queue.put1DRangeKernel(kernel, 0, workGroupSize*workGroups, workGroupSize);//,list);

        queue.putReadBuffer(bestFitnesses,true);
        queue.putReadBuffer(bestGlobalIndex,true);
    }

    public CLDevice getDevice() {
        return device;
    }

    public void runOnlookerBeesPhaseWholeOnGPU(CLBuffer<IntBuffer> weightsBuffer, CLBuffer<IntBuffer> distancesBuffer, CLBuffer<IntBuffer> foodSourcesBuffer, CLBuffer<IntBuffer> costFunctionValuesBuffer, CLBuffer<IntBuffer> triesBuffer, int foodSourcesNumber, int problemSize, int onlookersNumber, long seed) {
        int localWorkSize = min(device.getMaxWorkGroupSize(), 128);  // Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, foodSourcesNumber);   // rounded up to the nearest multiple of the localWorkSize
        long localMemSize = device.getLocalMemSize();
        /*System.out.println("localWorkSize:"+localWorkSize);
        System.out.println("globalWorkSize:"+globalWorkSize);
        System.out.println("localMemSize:"+localMemSize);*/
        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(ONLOOKER_BEE_PHASE_WHOLE_ON_GPU_KERNEL);
        kernel.putArgs(weightsBuffer,distancesBuffer,foodSourcesBuffer,costFunctionValuesBuffer,triesBuffer);
        kernel.putArg(foodSourcesNumber);
        kernel.putArg(problemSize);
        kernel.putArg(onlookersNumber);
        kernel.putArg(seed);

        queue.put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize);

        return;
    }

    public void runOnlookerBeesPhaseWithEliteSelection(CLBuffer<IntBuffer> weightsBuffer, CLBuffer<IntBuffer> distancesBuffer, CLBuffer<IntBuffer> foodSourcesBuffer, CLBuffer<IntBuffer> costFunctionValuesBuffer, CLBuffer<IntBuffer> triesBuffer, int foodSourcesNumber, int problemSize, int onlookersNumber, long seed) {
        int localWorkSize = min(device.getMaxWorkGroupSize(), 128);  // Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, onlookersNumber);   // rounded up to the nearest multiple of the localWorkSize
        long localMemSize = device.getLocalMemSize();
        /*System.out.println("localWorkSize:"+localWorkSize);
        System.out.println("globalWorkSize:"+globalWorkSize);
        System.out.println("localMemSize:"+localMemSize);*/
        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(ONLOOKER_BEE_PHASE_WITH_ELITE_SELECTION);
        kernel.putArgs(weightsBuffer,distancesBuffer,foodSourcesBuffer,costFunctionValuesBuffer,triesBuffer);
        kernel.putArg(foodSourcesNumber);
        kernel.putArg(problemSize);
        kernel.putArg(onlookersNumber);
        kernel.putArg(seed);

        queue.put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize);

        return;
    }

    public void runWholeAlgorithmOnGPU(CLBuffer<IntBuffer> weightsBuffer, CLBuffer<IntBuffer> distancesBuffer, int foodSourcesNumber, int onlookersNumber, int problemSize, int maxIterations, int maxTries, long seed, int localWorkSize, CLBuffer<IntBuffer> solutionBuffer, int workGroupsNumber, short useReductionToFindBest, short onlookerMethod) {
        /*System.out.println("localWorkSize:"+localWorkSize);
        System.out.println("globalWorkSize:"+globalWorkSize);
        System.out.println("localMemSize:"+localMemSize);*/
        CLProgram program = getProgram();
        CLKernel kernel = program.createCLKernel(WHOLE_ALGORITHM_KERNEL);
        kernel.putArgs(weightsBuffer,distancesBuffer);
        kernel.putArg(onlookersNumber);
        kernel.putArg(problemSize);
        kernel.putArg(maxIterations);
        kernel.putArg(maxTries);
        kernel.putArg(seed);
        kernel.putArg(solutionBuffer);
        kernel.putNullArg(localWorkSize * problemSize * 4); //foodSources
        kernel.putNullArg(localWorkSize * 4); //cost function values
        kernel.putNullArg(localWorkSize * 4); //tries
        kernel.putNullArg(localWorkSize * 4); //localTab
        kernel.putArg(useReductionToFindBest);
        kernel.putArg(onlookerMethod);
        queue.put1DRangeKernel(kernel, 0, localWorkSize * workGroupsNumber, localWorkSize);
        ;
    }


    public void release() {
        queue.release();
        context.release();
        program.release();
    }
}
