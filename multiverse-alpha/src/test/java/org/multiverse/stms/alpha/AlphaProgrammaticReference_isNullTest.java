package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticReference_isNullTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenValueNull() {
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>();

        long version = stm.getVersion();
        boolean result = ref.isNull();

        assertTrue(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenValueNotNull() {
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>("foo");

        long version = stm.getVersion();
        boolean result = ref.isNull();

        assertFalse(result);
        assertEquals(version, stm.getVersion());
    }
}
