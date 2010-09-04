package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_readBiasedTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void test() {
        BetaTransactionalObject ref = createReadBiasedLongRef(stm);

        Tranlocal active = ref.___unsafeLoad();
        assertNotNull(active);
        assertTrue(active.isCommitted());
        assertFalse(active.isPermanent());
        assertUnlocked(ref.___getOrec());
        assertSurplus(0, ref.___getOrec());
    }
    
    @Test
    @Ignore
    public void whenArrivingOnReadBiasedOrec(){

    }
}
