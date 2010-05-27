package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class ManualRefTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = AlphaStm.createFast();
        clearThreadLocalTransaction();
    }

    @Test
    public void testConstruction() {
        long version = stm.getVersion();

        ManualRef ref = new ManualRef(stm, 10);
        assertEquals(10, ref.get(stm));
        assertEquals(version, stm.getVersion());
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void testUpdate() {
        ManualRef ref = new ManualRef(stm, 10);

        long version = stm.getVersion();
        ref.inc(stm);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(11, ref.get(stm));
        assertNull(getThreadLocalTransaction());
    }
}
