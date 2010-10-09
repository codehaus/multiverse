package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class DirtyCheckTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNoDirtyCheckAndNonDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setDirtyCheckEnabled(false)
                .build()
                .newTransaction();

        ref.set(tx, 0);
        tx.commit();

        assertEquals(0, ref.atomicGet());
        assertVersionAndValue(ref, initialVersion + 1, 0);
    }

    @Test
    public void whenNoDirtyCheckAndDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setDirtyCheckEnabled(false)
                .build()
                .newTransaction();

        ref.set(tx, 1);
        tx.commit();

        assertEquals(1, ref.atomicGet());
        assertVersionAndValue(ref, initialVersion + 1, 1);
    }

    @Test
    public void whenDirtyCheckAndNonDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setDirtyCheckEnabled(true)
                .build()
                .newTransaction();

        ref.set(tx, 0);
        tx.commit();

        assertEquals(0, ref.atomicGet());
        assertVersionAndValue(ref, initialVersion, 0);
    }

    @Test
    public void whenDirtyCheckAndDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setDirtyCheckEnabled(true)
                .build()
                .newTransaction();

        ref.set(tx, 1);
        tx.commit();

        assertEquals(1, ref.atomicGet());
        assertVersionAndValue(ref, initialVersion + 1, 1);
    }
}
