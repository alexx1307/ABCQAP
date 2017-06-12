package com.luka.qap;

/**
 * Created by lukas on 22.06.2016.
 */
public interface IAlgorithm {
    Solution run();
    TimeStatistics getStatistics();
    void setProblem(ProblemInstance problem);
    void init(long seed);
}
