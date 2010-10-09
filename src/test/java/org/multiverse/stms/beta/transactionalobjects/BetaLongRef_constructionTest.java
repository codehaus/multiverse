package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_constructionTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void withStm() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef exampleRef = new BetaLongRef(tx);
        tx.openForConstruction(exampleRef);
        tx.commit();


        BetaLongRef ref = new BetaLongRef(stm);
        assertEquals(exampleRef.___toOrecString(), ref.___toOrecString());

        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, BetaTransactionalObject.VERSION_UNCOMMITTED + 1, 0);
    }

    @Test
    public void withStmAndInitialValue() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef exampleRef = new BetaLongRef(tx);
        tx.openForConstruction(exampleRef).value = 10;
        tx.commit();

        BetaLongRef ref = new BetaLongRef(stm, 10);
        assertEquals(exampleRef.___toOrecString(), ref.___toOrecString());

        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, BetaTransactionalObject.VERSION_UNCOMMITTED + 1, 10);
    }

    @Test
    public void withTransaction() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);

        assertSurplus(1, ref);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, 0, 0);
    }
}
