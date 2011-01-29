package org.multiverse.stms.gamma.transactions;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class SpeculativeGammaConfigurationTest {

    @Ignore
    @Test
    public void whenFat() {
        /*
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration(true);
        assertTrue(config.isNonRefTypeRequired);
        assertTrue(config.isFat);
        assertTrue(config.isCommuteRequired);
        assertTrue(config.isOrelseRequired);
        assertTrue(config.areListenersRequired);
        assertTrue(config.areLocksRequired);
        assertEquals(Integer.MAX_VALUE, config.minimalLength);
        */
    }

    @Test
    public void whenLean() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration();
        assertFalse(config.isNonRefTypeDetected);
        assertFalse(config.isFat);
        assertFalse(config.isCommuteDetected);
        assertFalse(config.isOrelseDetected);
        assertFalse(config.areListenersDetected);
        assertFalse(config.areLocksDetected);
        assertFalse(config.isAbortOnlyDetected);
        assertEquals(1, config.minimalLength);
    }

    @Test
    public void createWithNonRefType() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration()
                .newWithNonRefType();

        assertTrue(config.isNonRefTypeDetected);
        assertTrue(config.isFat);
        assertFalse(config.isCommuteDetected);
        assertFalse(config.isOrelseDetected);
        assertFalse(config.areListenersDetected);
        assertFalse(config.areLocksDetected);
        assertFalse(config.isAbortOnlyDetected);
        assertEquals(1, config.minimalLength);
    }

      @Test
    public void createWithAbortOnly() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration()
                .newWithAbortOnly();

        assertFalse(config.isNonRefTypeDetected);
        assertTrue(config.isFat);
        assertFalse(config.isCommuteDetected);
        assertFalse(config.isOrelseDetected);
        assertFalse(config.areListenersDetected);
        assertFalse(config.areLocksDetected);
        assertTrue(config.isAbortOnlyDetected);
        assertEquals(1, config.minimalLength);
    }

    @Test
    public void createWithCommuteRequired() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration()
                .newWithCommuteRequired();

        assertFalse(config.isNonRefTypeDetected);
        assertTrue(config.isFat);
        assertTrue(config.isCommuteDetected);
        assertFalse(config.isOrelseDetected);
        assertFalse(config.areListenersDetected);
        assertFalse(config.areLocksDetected);
        assertFalse(config.isAbortOnlyDetected);
        assertEquals(1, config.minimalLength);
    }

    @Test
    public void createWithListenersRequired() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration()
                .newWithListenersRequired();

        assertFalse(config.isNonRefTypeDetected);
        assertTrue(config.isFat);
        assertFalse(config.isCommuteDetected);
        assertFalse(config.isOrelseDetected);
        assertTrue(config.areListenersDetected);
        assertFalse(config.areLocksDetected);
        assertFalse(config.isAbortOnlyDetected);
        assertEquals(1, config.minimalLength);
    }

    @Test
    public void createWithOrElseRequired() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration()
                .newWithOrElseRequired();

        assertFalse(config.isNonRefTypeDetected);
        assertTrue(config.isFat);
        assertFalse(config.isCommuteDetected);
        assertTrue(config.isOrelseDetected);
        assertFalse(config.areListenersDetected);
        assertFalse(config.areLocksDetected);
        assertFalse(config.isAbortOnlyDetected);
        assertEquals(1, config.minimalLength);
    }

    @Test
    public void createWithMinimalLength() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration()
                .newWithMinimalLength(10);

        assertFalse(config.isNonRefTypeDetected);
        assertFalse(config.isFat);
        assertFalse(config.isCommuteDetected);
        assertFalse(config.isOrelseDetected);
        assertFalse(config.areListenersDetected);
        assertFalse(config.areLocksDetected);
        assertFalse(config.isAbortOnlyDetected);
        assertEquals(10, config.minimalLength);
    }

    @Test
    public void createWithLocksRequired() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration()
                .newWithLocksRequired();

        assertFalse(config.isNonRefTypeDetected);
        assertTrue(config.isFat);
        assertFalse(config.isCommuteDetected);
        assertFalse(config.isOrelseDetected);
        assertFalse(config.areListenersDetected);
        assertTrue(config.areLocksDetected);
        assertFalse(config.isAbortOnlyDetected);
        assertEquals(1, config.minimalLength);
    }
}
