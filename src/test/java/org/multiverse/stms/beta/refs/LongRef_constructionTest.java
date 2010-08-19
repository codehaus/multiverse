package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class LongRef_constructionTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        BetaTransaction tx = mock(BetaTransaction.class);
        LongRef ref = new LongRef(tx);

        assertSurplus(1, ref);
        assertLocked(ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
        assertSame(tx, ref.___getLockOwner());
    }
}
