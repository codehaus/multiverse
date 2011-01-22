package org.multiverse.stms.gamma.transactions;

import org.junit.Test;

import static org.junit.Assert.*;

public class SpeculativeGammaConfigurationTest {

    @Test
    public void test() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration(false);
        assertFalse(config.isNonRefTypeRequired);
        assertFalse(config.isFat);
        assertFalse(config.isCommuteRequired);
        assertFalse(config.isOrelseRequired);
        assertFalse(config.areListenersRequired);
        assertEquals(1, config.minimalLength);
    }

    @Test
    public void createWithNonRefType() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration(false).createWithNonRefType();

        assertTrue(config.isNonRefTypeRequired);
        assertTrue(config.isFat);
        assertFalse(config.isCommuteRequired);
        assertFalse(config.isOrelseRequired);
        assertFalse(config.areListenersRequired);
        assertEquals(1, config.minimalLength);
    }

    @Test
    public void createWithCommuteRequired() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration(false).createWithCommuteRequired();

        assertFalse(config.isNonRefTypeRequired);
        assertTrue(config.isFat);
        assertTrue(config.isCommuteRequired);
        assertFalse(config.isOrelseRequired);
        assertFalse(config.areListenersRequired);
        assertEquals(1, config.minimalLength);
    }

    @Test
    public void createWithListenersRequired() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration(false).createWithListenersRequired();

        assertFalse(config.isNonRefTypeRequired);
        assertTrue(config.isFat);
        assertFalse(config.isCommuteRequired);
        assertFalse(config.isOrelseRequired);
        assertTrue(config.areListenersRequired);
        assertEquals(1, config.minimalLength);
    }

    @Test
    public void createWithOrElseRequired() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration(false).createWithOrElseRequired();

        assertFalse(config.isNonRefTypeRequired);
        assertTrue(config.isFat);
        assertFalse(config.isCommuteRequired);
        assertTrue(config.isOrelseRequired);
        assertFalse(config.areListenersRequired);
        assertEquals(1, config.minimalLength);
    }

    @Test
    public void createWithMinimalLength() {
        SpeculativeGammaConfiguration config = new SpeculativeGammaConfiguration(false).createWithMinimalLength(10);

        assertFalse(config.isNonRefTypeRequired);
        assertFalse(config.isFat);
        assertFalse(config.isCommuteRequired);
        assertFalse(config.isOrelseRequired);
        assertFalse(config.areListenersRequired);
        assertEquals(10, config.minimalLength);
    }
}
