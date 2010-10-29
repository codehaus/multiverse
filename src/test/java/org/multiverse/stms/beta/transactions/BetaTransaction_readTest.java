package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.StmMismatchException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

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
    public void readCommittedIsolationLevel(){

    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForWrite() {

    }

    @Test
    @Ignore
    public void whenAlreadyCommuting(){

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
    @Ignore
    public void whenEnsuredBySelf() {

    }

    @Test
    @Ignore
    public void whenPrivatizedBySelf() {

    }

    @Test
    public void whenStmMismatch() {
        BetaLongRef ref = new BetaLongRef(new BetaStm());

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
