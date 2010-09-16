package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.createReadBiasedLongRef;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasNoCommitLock;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_readBiasedTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        BetaTransactionalObject ref = createReadBiasedLongRef(stm);

        Tranlocal active = ref.___unsafeLoad();
        assertNotNull(active);
        assertTrue(active.isCommitted());
        assertFalse(active.isPermanent());
        assertHasNoCommitLock(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
    }
    
    @Test
    @Ignore
    public void whenArrivingOnReadBiasedOrec(){

    }
}
