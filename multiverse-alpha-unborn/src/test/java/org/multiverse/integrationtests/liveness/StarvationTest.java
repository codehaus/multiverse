package org.multiverse.integrationtests.liveness;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class StarvationTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void whenNoContentionNoStarvation() {

    }

    @Test
    @Ignore
    public void whenLittleContention() {

    }

    @Test
    @Ignore
    public void whenMuchContention() {

    }

    @Test
    @Ignore
    public void whenTooMuchContention() {

    }
}
