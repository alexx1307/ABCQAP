package com.luka.algorithm.stopCriterion;

import com.luka.algorithm.AlgorithmState;
import com.luka.qap.TimeStatistics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Created by lukas on 29.04.2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class TotalTimeStopCriterionTest {
    private static final Duration bigDuration = Duration.ofMinutes(10L);
    private static final Duration smallDuration = Duration.ofMinutes(5L);
    @Mock
    AlgorithmState algorithmState;
    @Mock
    TimeStatistics statistics;

    @Test
    public void shouldStopCriterionBeMet() throws Exception {
        TotalTimeStopCriterion stopCriterion = new TotalTimeStopCriterion(smallDuration);
        when(statistics.getTotalTime()).thenReturn(bigDuration);
        assertTrue(stopCriterion.isStopCriterionMet(algorithmState,statistics));
    }

    @Test
    public void shouldNotStopCriterionBeMet() throws Exception {
        TotalTimeStopCriterion stopCriterion = new TotalTimeStopCriterion(bigDuration);
        when(statistics.getTotalTime()).thenReturn(smallDuration);
        assertFalse(stopCriterion.isStopCriterionMet(algorithmState,statistics));
    }
    @Test
    public void shouldNotStopCriterionBeMetWhenTimesEqual() throws Exception {
        TotalTimeStopCriterion stopCriterion = new TotalTimeStopCriterion(bigDuration);
        when(statistics.getTotalTime()).thenReturn(bigDuration);
        assertFalse(stopCriterion.isStopCriterionMet(algorithmState,statistics));
    }
}