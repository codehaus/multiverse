package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.BetaTranlocal;

import static junit.framework.Assert.assertSame;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class FatArrayTreeBetaTransaction_openingManyItemsTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenReadonly() {
        whenManyItems(true);
    }

    @Test
    public void whenUpdate() {
        whenManyItems(false);
    }

    public void whenManyItems(boolean reading) {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        int refCount = 10000;
        BetaLongRef[] refs = new BetaLongRef[refCount];
        BetaLongRefTranlocal[] tranlocals = new BetaLongRefTranlocal[refCount];
        for (int k = 0; k < refCount; k++) {
            BetaLongRef ref = newLongRef(stm);
            refs[k] = ref;
            tranlocals[k] = reading ? tx.openForRead(ref, LOCKMODE_NONE) : tx.openForWrite(ref, LOCKMODE_NONE);
        }

        assertEquals(refCount, tx.size());

        System.out.println("everything inserted");
        System.out.println("usage percentage: " + (100 * tx.getUsage()));

        for (int k = 0; k < refCount; k++) {
            BetaLongRef ref = refs[k];
            BetaTranlocal found = reading ? tx.openForRead(ref, LOCKMODE_NONE) : tx.openForWrite(ref, LOCKMODE_NONE);
            assertSame(ref, found.owner);
            assertSame("tranlocal is incorrect at " + k, tranlocals[k], found);
        }

        assertIsActive(tx);
    }
}
