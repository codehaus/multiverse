package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.StmMismatchException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;

public abstract class BetaTransaction_readTest {

    protected BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    public abstract BetaTransaction newTransaction();


    @Test
    public void whenCommittedBefore() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();

        LongRefTranlocal tranlocal = tx.open(ref);
    }

    @Test
    @Ignore
    public void serializedIsolationLevel() {

    }

    @Test
    @Ignore
    public void snapshotIsolationLevel() {

    }

    @Test
    @Ignore
    public void repeatableReadIsolationLevel() {

    }

    @Test
    @Ignore
    public void readCommittedIsolationLevel() {

    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.set(tx, initialValue+1);

        long value = tx.read(ref);

        assertEquals(initialValue+1, value);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasUpdateLock(ref, tx);
    }

    @Test
    @Ignore
    public void whenAlreadyCommuting() {

    }

    @Test
    @Ignore
    public void whenPrivatizedByOther() {

    }

    @Test
    @Ignore
    public void whenEnsuredByOther() {

    }

    @Test
    public void whenEnsuredBySelf() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);

        long value = tx.read(ref);

        assertEquals(initialValue, value);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasUpdateLock(ref, tx);
    }

    @Test
    public void whenPrivatizedBySelf() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);

        long value = tx.read(ref);

        assertEquals(initialValue, value);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenStmMismatch() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        try {
            tx.read(ref);
            fail();
        } catch (StmMismatchException expected) {

        }

        assertIsAborted(tx);
    }

    @Test
    public void whenNull_thenNullPointerException() {
        BetaTransaction tx = newTransaction();
        try {
            tx.read((BetaLongRef) null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsAborted(tx);
    }
}
