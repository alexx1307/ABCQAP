package com.luka.algorithm.selection;

import com.luka.algorithm.IFitnessEvaluable;

import java.util.List;

/**
 * Created by lukas on 12.11.2016.
 */
public interface ISelectionStrategy<T extends IFitnessEvaluable> {
    List<T> select(List<T> sources, Integer selectingObjectsNumber);
}
