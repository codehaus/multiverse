package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_atomicCompareAndSetTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenValueMatches() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);

        long version = stm.getVersion();
        boolean result = ref.atomicCompareAndSet(1, 5);

        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(5, ref.get());
        assertNull(ref.___getLockOwner());

        AlphaProgrammaticLongTranlocal current = (AlphaProgrammaticLongTranlocal) ref.___load();
        assertNotNull(current);
        assertTrue(current.isCommitted());
        assertEquals(5, current.value);
        assertEquals(version + 1, current.___writeVersion);
    }

    @Test
    @Ignore
    public void whenChangeThenListenersNotified() {

    }

    @Test
    public void whenValueNotMatches() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);
        AlphaProgrammaticLongTranlocal readonly = (AlphaProgrammaticLongTranlocal) ref.___load();

        long version = stm.getVersion();
        boolean result = ref.atomicCompareAndSet(2, 5);

        assertFalse(result);
        assertEquals(version, stm.getVersion());
        assertEquals(1, ref.get());
        assertNull(ref.___getLockOwner());
        assertSame(readonly, ref.___load());
    }
}
