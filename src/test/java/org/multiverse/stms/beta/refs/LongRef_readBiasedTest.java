package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.ObjectPool;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

/**
 * @author Peter Veentjer
 */
public class LongRef_readBiasedTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void test() {
        BetaTransactionalObject ref = createReadBiasedLongRef(stm);

        Tranlocal active = ref.unsafeLoad();
        assertNotNull(active);
        assertTrue(active.isCommitted());
        assertTrue(active.isPermanent());
        assertUnlocked(ref.getOrec());
        assertSurplus(0, ref.getOrec());
    }
}
