package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class GammaTransaction_abortTest<T extends GammaTransaction> implements GammaConstants {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    protected abstract void assertCleaned(T tx);

    @Test
    public void whenUnused() {
        T tx = newTransaction();

        tx.abort();

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
    }

    @Test
    public void whenHasRead() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx,LOCKMODE_NONE);
        tx.abort();

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
        assertEquals(initialValue, ref.value);
        assertEquals(initialVersion, ref.getVersion());
        assertCleaned(tx);
    }

    @Test
    public void whenHasWrite() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx,LOCKMODE_NONE);
        tx.abort();

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
        assertEquals(initialValue, ref.value);
        assertEquals(initialVersion, ref.getVersion());
        assertCleaned(tx);
    }

    @Test
    public void whenAborted() {
        T tx = newTransaction();
        tx.abort();

        tx.abort();

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
        assertCleaned(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        T tx = newTransaction();
        tx.commit();

        try {
            tx.abort();
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertCleaned(tx);
    }
}
