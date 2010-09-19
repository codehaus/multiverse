package org.multiverse.stms.beta.integrationtest.pessimistic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertSame;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasCommitLock;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasNoUpdateLock;

public class PessimisticTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void constructedObjectAutomaticallyIsLocked() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref);

        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }


}
