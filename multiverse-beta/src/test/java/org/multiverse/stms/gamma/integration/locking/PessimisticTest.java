package org.multiverse.stms.gamma.integration.locking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertRefHasCommitLock;

public class PessimisticTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = (BetaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void constructedObjectAutomaticallyIsLocked() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref);

        assertRefHasCommitLock(ref, tx);
    }
}
