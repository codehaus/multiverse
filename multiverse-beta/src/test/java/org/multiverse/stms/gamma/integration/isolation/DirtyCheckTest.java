package org.multiverse.stms.gamma.integration.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.assertVersionAndValue;

public class DirtyCheckTest {
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = (GammaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNoDirtyCheckAndNonDirtyWrite() {
        GammaLongRef ref = new GammaLongRef(stm);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
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
        GammaLongRef ref = new GammaLongRef(stm);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
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
        GammaLongRef ref = new GammaLongRef(stm);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
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
        GammaLongRef ref = new GammaLongRef(stm);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .setDirtyCheckEnabled(true)
                .build()
                .newTransaction();

        ref.set(tx, 1);
        tx.commit();

        assertEquals(1, ref.atomicGet());
        assertVersionAndValue(ref, initialVersion + 1, 1);
    }
}
