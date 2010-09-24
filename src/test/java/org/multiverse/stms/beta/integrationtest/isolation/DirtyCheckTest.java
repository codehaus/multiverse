package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

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
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setDirtyCheckEnabled(false)
                .build()
                .start();

        ref.set(tx, 0);
        tx.commit();

        assertEquals(0, ref.atomicGet());
        assertNotSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNoDirtyCheckAndDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setDirtyCheckEnabled(false)
                .build()
                .start();

        ref.set(tx, 1);
        tx.commit();

        assertEquals(1, ref.atomicGet());
        assertNotSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenDirtyCheckAndNonDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setDirtyCheckEnabled(true)
                .build()
                .start();

        ref.set(tx, 0);
        tx.commit();

        assertEquals(0, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenDirtyCheckAndDirtyWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setDirtyCheckEnabled(true)
                .build()
                .start();

        ref.set(tx, 1);
        tx.commit();

        assertEquals(1, ref.atomicGet());
        assertNotSame(committed, ref.___unsafeLoad());
    }
}
