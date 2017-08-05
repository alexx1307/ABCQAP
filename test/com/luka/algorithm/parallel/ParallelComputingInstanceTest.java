package com.luka.algorithm.parallel;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import com.luka.algorithm.FoodSource;
import com.luka.qap.Solution;
import org.junit.Test;

import java.nio.IntBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by lukas on 29.04.2017.
 */
public class ParallelComputingInstanceTest {

    private String programName = "qap_abc.cl";

    @Test
    public void getNPseudoRandomInts() throws Exception {
        ParallelComputingInstance instance = new ParallelComputingInstance(programName);
        instance.init();

        CLBuffer<IntBuffer> pseudorandomIntsBuffer = instance.getNPseudoRandomInts(10, 9876541L);
        System.out.println("Pseudo random ints:");
        while (pseudorandomIntsBuffer.getBuffer().hasRemaining()) {
            System.out.println(pseudorandomIntsBuffer.getBuffer().get());
        }

    }

    @Test
    public void testDurstenfeldAlgorithm() throws Exception {
        ParallelComputingInstance instance = new ParallelComputingInstance(programName);
        instance.init();

        int permLength = 50;
        int permNumber = 200000;
        int z = 600;
        while (z > 0) {
            Instant before = Instant.now();
            CLBuffer<IntBuffer> permBuffer = instance.createIntBuffer(permLength * permNumber, CLMemory.Mem.READ_WRITE);
            instance.runDurstenfeldAlgorithmKernel(permBuffer, permNumber, permLength, 555);
            Instant after = Instant.now();
            System.out.println(Duration.between(before, after));
            z--;
        }
        /*System.out.println("perms:");
        for(int i = 0; i<permNumber;i++)
        {
            System.out.print("perm "+i+": ");
            for (int j = 0; j< permLength;j++){
                System.out.print(permBuffer.getBuffer().get()+" ");
            }
            System.out.println();
        }*/
    }

    @Test
    public void testInitAndEmployedBeesPhase() throws Exception {
        ParallelComputingInstance instance = new ParallelComputingInstance(programName);
        instance.init();

        int permLength = 5;
        int permNumber = 10;
        int repetitions = 1;
        while (repetitions > 0) {
            List<FoodSource> foodSources = null;

            //UWAGA: NIEPOPRAWNIE ZAINICJALIZOWANE BUFORY
            CLBuffer<IntBuffer> permBuffer = instance.createIntBuffer(permLength*permNumber, CLMemory.Mem.READ_WRITE);
            CLBuffer<IntBuffer> weightsBuffer = instance.createIntBuffer(permLength*permNumber, CLMemory.Mem.READ_WRITE);
            CLBuffer<IntBuffer> distancesBuffer = instance.createIntBuffer(permLength*permNumber, CLMemory.Mem.READ_WRITE);
            CLBuffer<IntBuffer> fitnessBuffer = instance.createIntBuffer(permLength*permNumber, CLMemory.Mem.READ_WRITE);
            CLBuffer<IntBuffer> triesBuffer = instance.createIntBuffer(permLength*permNumber, CLMemory.Mem.READ_WRITE);
            instance.runDurstenfeldAlgorithmKernel(permBuffer, permNumber, permLength, 555);
            System.out.println("After init");
            System.out.println("buffer remaining = " + permBuffer.getBuffer().remaining());
            System.out.println("buffer position = " + permBuffer.getBuffer().position());
            for (int i = 0; i < 5; i++) {
                System.out.println("Before adding");
                System.out.println("buffer remaining = " + permBuffer.getBuffer().remaining());
                System.out.println("buffer position = " + permBuffer.getBuffer().position());
                if (permBuffer.getBuffer().remaining() == 0) {
                    System.out.println("adding to buffer");
                    addFoodSourcesIntoBuffer(foodSources, permBuffer);
                }
                System.out.println("After adding");
                System.out.println("buffer remaining = " + permBuffer.getBuffer().remaining());
                System.out.println("buffer position = " + permBuffer.getBuffer().position());
                instance.writeBuffer(permBuffer);
                instance.runEmployeeBeeParallel(weightsBuffer, distancesBuffer,permBuffer, fitnessBuffer, triesBuffer,permNumber, permLength, 111 + i);
                instance.readBuffer(permBuffer);
                System.out.println("After employee phase");
                System.out.println("buffer remaining = " + permBuffer.getBuffer().remaining());
                System.out.println("buffer position = " + permBuffer.getBuffer().position());
                foodSources = convertBufferIntoFoodSources(permBuffer, permLength);
                printFoodSources(foodSources);
                System.out.println();
            }
            //printPermutationsFromBuffer(permLength, permNumber, permBuffer);

            repetitions--;
        }
    }
/*
    @Test
    public void testOmmitingBufferReading() throws Exception {
        ParallelComputingInstance instance = new ParallelComputingInstance();
        instance.init();

        int permLength = 5;
        int permNumber = 10;
        List<FoodSource> foodSources = null;
        CLBuffer<IntBuffer> permBuffer = instance.createIntBuffer(permNumber* permLength, CLMemory.Mem.READ_WRITE) ;
        instance.runDurstenfeldAlgorithmKernel(permBuffer,permNumber, permLength, 555, false);


        for (int i = 0; i < 5; i++) {
            if (permBuffer.getBuffer().remaining() == 0) {
                addFoodSourcesIntoBuffer(foodSources, permBuffer);
            }

            instance.runEmployeeBeeParallel(permBuffer, permNumber, permLength, 111 + i, false,false);
            instance.runEmployeeBeeParallel(permBuffer, permNumber, permLength, 111 + i, false,false);
            instance.runEmployeeBeeParallel(permBuffer, permNumber, permLength, 111 + i, false,false);
            instance.runEmployeeBeeParallel(permBuffer, permNumber, permLength, 111 + i, false, true);

            foodSources = convertBufferIntoFoodSources(permBuffer, permLength);
            printFoodSources(foodSources);
            System.out.println();

        }
    }*/


    private void addFoodSourcesIntoBuffer(List<FoodSource> foodSources, CLBuffer<IntBuffer> foodSourcesAsBuffer) {
        IntBuffer buffer = foodSourcesAsBuffer.getBuffer();
        buffer.rewind();
        for (FoodSource foodSource : foodSources) {
            ArrayList<Integer> facilitiesMapping = foodSource.getSolution().getFacilitiesMapping();
            for (int integer : facilitiesMapping) {
                buffer.put(integer);
            }
        }
        buffer.rewind();
    }

    private List<FoodSource> convertBufferIntoFoodSources(CLBuffer<IntBuffer> foodSourcesAsBuffer, int permLength) {
        IntBuffer buffer = foodSourcesAsBuffer.getBuffer();
        //System.out.println(buffer.remaining());
        int foodSourcesNumber = buffer.remaining() / permLength;
        List<FoodSource> foodSources = new ArrayList<>(foodSourcesNumber);
        for (int i = 0; i < foodSourcesNumber; i++) {
            FoodSource foodSource = new FoodSource();
            ArrayList<Integer> mapping = new ArrayList<>(permLength);
            for (int j = 0; j < permLength; j++) {
                int elem = buffer.get();
                //System.out.print(elem+" ");
                mapping.add(elem);
            }
            //System.out.println();
            foodSource.setSolution(new Solution(mapping, null));
            foodSources.add(foodSource);
        }
        return foodSources;
    }

    private void printPermutationsFromBuffer(int permLength, int permNumber, CLBuffer<IntBuffer> permBuffer) {
        System.out.println("perms:");
        for (int i = 0; i < permNumber; i++) {
            System.out.print("perm " + i + ": ");
            for (int j = 0; j < permLength; j++) {
                System.out.print(permBuffer.getBuffer().get() + " ");
            }
            System.out.println();
        }
    }

    private void printFoodSources(List<FoodSource> foodSources) {
        System.out.println("perms:");
        for (FoodSource foodSource : foodSources) {
            System.out.println(foodSource.getSolution().toString());
        }
    }

    @Test
    public void testIncerementKernel() throws Exception {

        ParallelComputingInstance instance = new ParallelComputingInstance(programName);
        instance.init();
        int permNumber = 900;
        int permLength = 120;
        runTestWithoutTransfer(instance, permNumber, permLength);
        runTestWithoutTransfer(instance, permNumber, permLength);
        runTestWithoutTransfer(instance, permNumber, permLength);
        runTestWithoutTransfer(instance, permNumber, permLength);
        runOnCPU(instance, permNumber, permLength);
        runOnCPU(instance, permNumber, permLength);
        runOnCPU(instance, permNumber, permLength);
        runOnCPU(instance, permNumber, permLength);
        runTestWithTransfer(instance, permNumber, permLength);
        runTestWithTransfer(instance, permNumber, permLength);
        runTestWithTransfer(instance, permNumber, permLength);
        runTestWithTransfer(instance, permNumber, permLength);
        //printPermutationsFromBuffer(permLength,permNumber,buffer);


    }

    private void runTestWithTransfer(ParallelComputingInstance instance, int permNumber, int permLength) {
        Instant before = Instant.now();
        CLBuffer<IntBuffer> buffer = instance.createIntBuffer(permNumber* permLength, CLMemory.Mem.READ_WRITE) ;

        instance.runDurstenfeldAlgorithmKernel(buffer, permNumber, permLength, 555);
        instance.runTestIncrementKernel(buffer, permNumber, permLength, true, true);
        for (int i = 0; i < 10000; i++) {
            instance.runTestIncrementKernel(buffer, permNumber, permLength, true, true);
        }
        instance.runTestIncrementKernel(buffer, permNumber, permLength, true, true);
        Instant after = Instant.now();
        System.out.println(Duration.between(before, after).toMillis());

    }

    private void runTestWithoutTransfer(ParallelComputingInstance instance, int permNumber, int permLength) {
        Instant before = Instant.now();
        CLBuffer<IntBuffer> buffer = instance.createIntBuffer(permNumber* permLength, CLMemory.Mem.READ_WRITE) ;
        instance.runDurstenfeldAlgorithmKernel(buffer, permNumber, permLength, 555);
        instance.runTestIncrementKernel(buffer, permNumber, permLength, false, true);
        for (int i = 0; i < 10000; i++) {
            instance.runTestIncrementKernel(buffer, permNumber, permLength, false, false);
        }
        instance.runTestIncrementKernel(buffer, permNumber, permLength, true, false);
        Instant after = Instant.now();
        System.out.println(Duration.between(before, after).toMillis());

    }

    private void runOnCPU(ParallelComputingInstance instance, int permNumber, int permLength) {
        Instant before = Instant.now();
        CLBuffer<IntBuffer> buffer = instance.createIntBuffer(permNumber* permLength, CLMemory.Mem.READ_WRITE) ;
        instance.runDurstenfeldAlgorithmKernel(buffer,permNumber, permLength, 555);
        instance.readBuffer(buffer);
        int[] tab = new int[permLength * permNumber];
        buffer.getBuffer().get(tab);
        for (int i = 0; i < 10002; i++) {
            for (int j = 0; j < permLength * permNumber; j++) {
                tab[j]++;
            }
        }
        Instant after = Instant.now();
        System.out.println(Duration.between(before, after).toMillis());
    }

    @Test
    public void durstenfeldGroupSizeTest() {
        ParallelComputingInstance instance = new ParallelComputingInstance(programName);
        instance.init();
        int permNumber = 53000;
        for (int permLength = 37; permLength < 39; permLength++) {
            CLBuffer<IntBuffer> buffer = instance.createIntBuffer(permNumber* permLength, CLMemory.Mem.READ_WRITE) ;
            instance.runDurstenfeldAlgorithmKernel(buffer,permNumber, permLength, 1);
        }
    }


    public static void main(String[] args) {
        ParallelComputingInstanceTest test = new ParallelComputingInstanceTest();
        test.durstenfeldGroupSizeTest();
        Scanner scanner = new Scanner(System.in);
        scanner.next();
        System.exit(0);
    }
}