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
public class AlphaRef_isNullTest {
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
        AlphaRef<String> ref = new AlphaRef<String>();

        long version = stm.getVersion();
        boolean result = ref.isNull();

        assertTrue(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenValueNotNull() {
        AlphaRef<String> ref = new AlphaRef<String>("foo");

        long version = stm.getVersion();
        boolean result = ref.isNull();

        assertFalse(result);
        assertEquals(version, stm.getVersion());
    }
}
