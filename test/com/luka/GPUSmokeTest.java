package com.luka;

import com.jogamp.opencl.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Random;

import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import static java.lang.System.nanoTime;
import static java.lang.System.out;

/**
 * Created by lukas on 08.04.2017.
 */
public class GPUSmokeTest {

    @Test
    public void experiment() throws IOException {
        int elementCount = 11444777;
// Local work size dimensions
        int localWorkSize = 256;
// rounded up to the nearest multiple of the localWorkSize
        int globalWorkSize = roundUp(localWorkSize, elementCount);

// setup
        CLContext context = CLContext.create();

        CLProgram program = context.createProgram(
                Main.class.getResourceAsStream("VectorAdd.cl")
        ).build();

        CLBuffer<FloatBuffer> clBufferA =
                context.createFloatBuffer(globalWorkSize, READ_ONLY);
        CLBuffer<FloatBuffer> clBufferB =
                context.createFloatBuffer(globalWorkSize, READ_ONLY);
        CLBuffer<FloatBuffer> clBufferC =
                context.createFloatBuffer(globalWorkSize, WRITE_ONLY);

        out.println("used device memory: "
                + (clBufferA.getCLSize() + clBufferB.getCLSize() + clBufferC.getCLSize()) / 1000000 + "MB");

// fill read buffers with random numbers (just to have test data).
        fillBuffer(clBufferA.getBuffer(), 12345);
        fillBuffer(clBufferB.getBuffer(), 67890);

// get a reference to the kernel functon with the name 'VectorAdd'
// and map the buffers to its input parameters.
        CLKernel kernel = program.createCLKernel("VectorAdd");
        kernel.putArgs(clBufferA, clBufferB, clBufferC).putArg(elementCount);

// create command queue on fastest device.\
        CLDevice[] devices = context.getDevices();

        System.out.println("Available Devices " + devices.length + ":");
        for (CLDevice device : devices) {
            out.println("dev: " + device.getVendor() + ", " + device.toString());
        }

        CLDevice device = context.getMaxFlopsDevice(CLDevice.Type.GPU);
        out.println("Chosen device: " + device.getName());
        CLCommandQueue queue = device.createCommandQueue();

// asynchronous write to GPU device,
// blocking read later to get the computed results back.
        long time = nanoTime();
        queue.putWriteBuffer(clBufferA, false)
                .putWriteBuffer(clBufferB, false)
                .put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize)
                .putReadBuffer(clBufferC, true);
        time = nanoTime() - time;

// cleanup all resources associated with this context.
        context.release();

// print first few elements of the resulting buffer to the console.
        out.println("a+b=c results snapshot: ");
        for (int i = 0; i < 10; i++)
            out.print(clBufferC.getBuffer().get() + ", ");
        out.println("...; " + clBufferC.getBuffer().remaining() + " more");

        out.println("computation took: " + (time / 1000000) + "ms");

    }

    private static final void fillBuffer(FloatBuffer buffer, int seed) {
        Random rnd = new Random(seed);
        while (buffer.remaining() != 0)
            buffer.put(rnd.nextFloat() * 100);
        buffer.rewind();
    }

    private static final int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        if (r == 0) {
            return globalSize;
        } else {
            return globalSize + groupSize - r;
        }
    }
}