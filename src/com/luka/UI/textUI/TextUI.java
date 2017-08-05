package com.luka.UI.textUI;

import com.luka.UI.AbstractUI;
import com.luka.UI.EUserAction;
import com.luka.algorithm.ABCAlgorithmParameters;
import com.luka.algorithm.parallel.ParallelAbcAlgorithm;
import com.luka.algorithm.stopCriterion.IterationStopCriterion;
import com.luka.algorithm.selection.RouletteWheelSelectionStrategyImpl;
import com.luka.qap.IAlgorithm;
import com.luka.qap.ProblemInstance;
import com.luka.utils.ProblemMarshaller;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by lukas on 11.03.2017.
 */
public class TextUI extends AbstractUI {
    private static final Logger LOGGER = Logger.getLogger("myLogger");
    Scanner reader = new Scanner(System.in);  // Reading from System.in

    @Override
    protected ProblemInstance acquireProblemData() {
        System.out.println("Would you like to define problem by yourself (1) or use predefined one (2)?");
        String userInput = reader.next();
        while (true) {
            switch (userInput) {
                case "1":
                    System.out.println("Not available yet");
                case "2":
                    File file = chooseProblemFile();
                    ProblemInstance problemFromFile = ProblemMarshaller.createProblemFromFile(file);
                    if(problemFromFile != null){
                        userAction = EUserAction.SETUP_ALGORITHM;
                    }
                    return problemFromFile;
                default:
                    System.out.println("Please enter \"1\" or \"2\".");
            }
            userInput = reader.next();
        }
    }

    private File chooseProblemFile() {
        //Path currPath = Paths.get("./testCases");
        Path currPath = Paths.get("./testCases");
        /*URI uri =;
        URI.create("../testCases");
        */

        List<File> directories = new ArrayList<>();
        List<File> possibleTests = new ArrayList<>();

        System.out.println("Using predefined problem");

        while (true) {

            System.out.println("You are in: " + currPath.toAbsolutePath().normalize().toString() + ". \"cd\" to some directory or choose number to indicate data file");
            File folder = currPath.toFile();


            directories.clear();
            possibleTests.clear();

            Arrays.stream(folder.listFiles()).forEach(f -> {
                if (f.isDirectory()) {
                    directories.add(f);
                } else if (f.isFile() && f.getName().endsWith(".dat")) {
                    possibleTests.add(f);
                }
            });

            for (int i = 0; i < directories.size(); i++) {
                System.out.println("(dir) " + directories.get(i).getName());
            }
            for (int i = 0; i < possibleTests.size(); i++) {
                System.out.println("(" + i + ") " + possibleTests.get(i).getName());
            }


            while (true) {
                if (possibleTests.size() > 0) {
                    System.out.println("Please enter number between \"0\" to \"" + (possibleTests.size() - 1) + "\" or cd to some directory.");
                } else {
                    System.out.println("Please cd to some directory.");
                }
                String userInput = null;
                while (StringUtils.isBlank(userInput)) {
                    userInput = reader.nextLine();
                }
                try {
                    int n = Integer.parseInt(userInput);
                    if (n >= 0 && n < possibleTests.size()) {
                        return possibleTests.get(n);
                    } else {
                        continue;
                    }
                } catch (NumberFormatException e) {
                    //do nothing
                }


                if (userInput.toLowerCase().startsWith("cd")) {
                    currPath = currPath.resolve(userInput.substring(3));
                    break;
                }
            }

        }
    }

    @Override
    protected IAlgorithm setupAlgorithm() {
        ABCAlgorithmParameters params = new ABCAlgorithmParameters();
        params.setFoodSourcesNumber(100);
        params.setFoodSourceTrialsLimit(20);
        params.setOnlookersNumber(30);
        params.setMaxIterations(1000);
        params.setSelectionStrategy(new RouletteWheelSelectionStrategyImpl(new Random()));

        userAction=EUserAction.START_ALGORITHM;

        return new ParallelAbcAlgorithm(params);
    }

    @Override
    protected void showWelcomeMessage() {
        userAction=EUserAction.ACQUIRE_PROBLEM;
    }

    @Override
    protected void askWhatToDoNext() {
        System.out.println("Would you like to do another test (1) or exit (2)?");
        String userInput = reader.next();
        while (true) {
            switch (userInput) {
                case "1":
                    userAction=EUserAction.START;
                    return;
                case "2":
                    userAction=EUserAction.QUIT;
                    return;
                default:
                    System.out.println("Please enter \"1\" or \"2\".");
            }
            userInput = reader.next();
        }
    }

    @Override
    protected void showStatistics() {
        System.out.println("QUALITY STATS:\n");
        System.out.println("Total evaluations: "+solution.newSolutionEvaluations);
        System.out.println("Result is = "+solution.getEvaluatedResult());
        System.out.println("Permutation: "+
                Arrays.toString(solution.getFacilitiesMapping().toArray()));

        System.out.println("\nTIME STATS:\n");
        System.out.println("Total time in ms: "+algorithm.getStatistics().getTotalTimeInMs());

        System.out.println("init phase time in ms: "+algorithm.getStatistics().getInitPhaseTime());

        System.out.println("avg criterion phase time in ms: "+algorithm.getStatistics().getAvgCriterionPhaseTimeInMs());
        System.out.println("avg employee phase time in ms: "+algorithm.getStatistics().getAvgEmployeePhaseTimeInMs());
        System.out.println("avg onlookers phase time in ms: "+algorithm.getStatistics().getAvgOnlookerPhaseTimeInMs());
        System.out.println("avg scouts phase time in ms: "+algorithm.getStatistics().getAvgScoutPhaseTimeInMs());
        System.out.println("avg updating phase time in ms: "+algorithm.getStatistics().getAvgUpdatingPhaseTimeInMs());


    }
}
