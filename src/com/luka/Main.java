package com.luka;

import com.jogamp.opencl.*;
import com.luka.UI.IUI;
import com.luka.UI.textUI.TextUI;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Random;

import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import static java.lang.System.nanoTime;
import static java.lang.System.out;


public class Main {

    public static void main(String[] args) {
        IUI ui = new TextUI();
        ui.handleUserInteractions();
    }



}
