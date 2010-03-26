package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class SpeculativeNonAutomaticReadTrackingTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void whenSpeculativeNonAutomaticReadTrackingAndNoRetry_thenSpeculationSuccess() {

    }

    @Test
    @Ignore
    public void whenSpeculativeNonAutomaticReadTrackingAndRetry_thenSpeculationFailure() {

    }
}
