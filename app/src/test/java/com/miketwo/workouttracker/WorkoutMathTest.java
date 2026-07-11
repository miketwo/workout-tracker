package com.miketwo.workouttracker;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorkoutMathTest {
    @Test public void calculatesVolume() { assertEquals(720.0, WorkoutMath.volume(8, 90), 0.001); }
    @Test public void formatsDecimalWeight() {
        assertEquals("90", WorkoutMath.formatWeight(90));
        assertEquals("92.5", WorkoutMath.formatWeight(92.5));
    }
    @Test public void calculatesPace() { assertEquals("13:00", WorkoutMath.pace(26, 2)); }
    @Test public void handlesMissingPace() { assertEquals("—", WorkoutMath.pace(20, 0)); }
}
