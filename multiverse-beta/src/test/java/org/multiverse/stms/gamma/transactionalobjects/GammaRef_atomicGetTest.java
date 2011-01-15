package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.assertRefHasNoLocks;
import static org.multiverse.stms.gamma.GammaTestUtils.assertVersionAndValue;

public class GammaRef_atomicGetTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void locked() {

    }

    @Test
    public void whenNull() {
        GammaRef<String> ref = new GammaRef<String>(stm, null);
        long initialVersion = ref.getVersion();

        String result = ref.atomicGet();

        assertNull(result);
        assertVersionAndValue(ref, initialVersion, null);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenNotNull() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        String result = ref.atomicGet();

        assertSame(initialValue, result);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }
}
