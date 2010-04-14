package org.multiverse.stms.alpha.transactions;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class SpeculativeConfiguration_readonlyTest {

    @Test
    public void whenSpeculativeReadonlyNotEnabled_thenIllegalStateException() {
        SpeculativeConfiguration config = new SpeculativeConfiguration(false, false, false, 100);

        try {
            config.signalSpeculativeReadonlyFailure();
            fail();
        } catch (IllegalStateException expected) {
        }

        assertTrue(config.isReadonly());
    }

    @Test
    public void test() {
        SpeculativeConfiguration config = new SpeculativeConfiguration(40);

        config.signalSpeculativeReadonlyFailure();

        assertFalse(config.isReadonly());
    }
}
