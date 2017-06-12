package com.luka.algorithm;

import com.luka.qap.IAlgorithm;
import com.luka.qap.ProblemInstance;
import com.luka.qap.Solution;
import com.luka.qap.TimeStatistics;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by lukas on 22.06.2016.
 */
public class DummyRandomAlgorithm implements IAlgorithm {
    private ProblemInstance problem;

    @Override
    public Solution run() {

        ArrayList<Integer> mapping = new ArrayList<>(problem.getProblemSize());
        for (int i = 0; i < problem.getProblemSize(); i++) {
             mapping.add(i);
        }
        Collections.shuffle(mapping);
        return  new Solution(mapping, problem);
       /* %% initialization
        for m=1:N
        Food source initializati
        on by scout: Equation (5);
        Fit=fitness (foods);
        end
        for cycle=1:MCN
                % employed bee phase
        for m=1:N
                v=New food sources based on equation (6);
        New Fit Evaluation equation (7);
        if fit v<fit(m)
        foods(m)=v;
        else
        next trial;
        end
                end
        % onlooker bee phase
        Calculate probability of
        selection e
        quation (8);
        while m<N
        if rand<P(m)
                x=foods(m);
        k=rand(1 N);
        while k==m
        k=rand(1 N);
        end
                v=New food sources in rand (-a, a) based on equation (6);
        New Fit Evaluation equation (7);
        if fit v<fit(m,1)
        foods =v;
        else
        next trial;
        end
        if m==N;
        m=0;
        end
                end
        % scout bee phase
                q= trial>Limit;
        for j=1:q
        New food by Equation (6);
        Evaluate food by Equation (7);
        end
                [fit food]=min (fit);
        if fit<best-fit
        best-fit=fit;
        end
                end;*/
    }

    @Override
    public TimeStatistics getStatistics() {
        return new TimeStatistics(0);
    }

    @Override
    public void setProblem(ProblemInstance problem) {
        this.problem = problem;
    }

    @Override
    public void init(long seed) {

    }
}
