package com.luka.algorithm.selection;

import java.util.Random;

/**
 * Created by lukas on 17.06.2017.
 */
public class SelectionStrategyFactory {

    private  Random random;

    public SelectionStrategyFactory(Random random) {
        this.random = random;
    }

    public  RouletteWheelSelectionStrategyImpl getByName(String selectionStrategyName) {
        return new RouletteWheelSelectionStrategyImpl(random);
    }
}
