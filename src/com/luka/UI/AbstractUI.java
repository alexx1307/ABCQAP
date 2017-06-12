package com.luka.UI;

import com.luka.qap.IAlgorithm;
import com.luka.qap.ProblemInstance;
import com.luka.qap.Solution;

/**
 * Created by lukas on 11.03.2017.
 */
public abstract class AbstractUI implements IUI {
    protected EUserAction userAction = EUserAction.START;

    protected ProblemInstance problemInstance;
    protected IAlgorithm algorithm;
    protected Solution solution;


    @Override
    public void handleUserInteractions() {
        while(true){
            switch(userAction){
                case START:
                    showWelcomeMessage();
                    break;
                case ACQUIRE_PROBLEM:
                    problemInstance = acquireProblemData();
                    break;
                case SETUP_ALGORITHM:
                    algorithm = setupAlgorithm();
                    break;
                case START_ALGORITHM:
                    solution = startAlgorithm();
                    break;
                case SHOW_STATISTICS:
                    showStatistics();
                    askWhatToDoNext();
                    break;
                case QUIT:
                    return;
            }
        }
    }

    private Solution startAlgorithm() {
        userAction = EUserAction.SHOW_STATISTICS;
        algorithm.setProblem(problemInstance);
        algorithm.init(100);
        return algorithm.run();
    }

    protected abstract void showStatistics();

    protected abstract void askWhatToDoNext();


    protected abstract ProblemInstance acquireProblemData();

    protected abstract IAlgorithm setupAlgorithm();

    protected abstract void showWelcomeMessage();
/*
    protected void setUserAction(EUserAction userAction) {
        this.userAction = userAction;
    }*/

}
