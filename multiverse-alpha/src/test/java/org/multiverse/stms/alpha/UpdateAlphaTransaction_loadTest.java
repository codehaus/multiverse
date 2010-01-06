package org.multiverse.stms.alpha;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.DummyTransaction;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.LoadLockedException;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.stms.alpha.manualinstrumentation.IntRefTranlocal;
import org.multiverse.utils.clock.Clock;

/**
 * @author Peter Veentjer
 */
public class UpdateAlphaTransaction_loadTest {

    private AlphaStm stm;
    private Clock clock;

    @Before
    public void setUp() {
        AlphaStmConfig config = new AlphaStmConfig();
        clock = config.clock;
        stm = new AlphaStm(config);
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    public AlphaTransaction startUpdateTransaction() {
        AlphaTransaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    /**
     * In the previous version multiverse, it was allowed to do a load of an atomicobject that was locked even the
     * version of the current content matches the version of the transaction. If the atomicobject didn't have any
     * references to other objects, this should be alright. But if an object does have dependencies, these dependencies
     * could escape before they are committed. For now this has been disallowed.
     */
    @Test
    public void loadFailsIfLockedButVersionIsStillExactMatch() {
        IntRef ref = new IntRef(0);
        Transaction owner = new DummyTransaction();
        ref.___tryLock(owner);

        AlphaTransaction t = startUpdateTransaction();
        try {
            t.load(ref);
            fail();
        } catch (LoadLockedException expected) {
        }

        assertSame(owner, ref.___getLockOwner());
        assertIsActive(t);
    }

    @Test
    public void loadFailsIfVersionIsTooNew() {
        IntRef ref = new IntRef(0);

        AlphaTransaction t1 = stm.startUpdateTransaction("");

        AlphaTransaction t2 = stm.startUpdateTransaction("");
        IntRefTranlocal tranlocal = (IntRefTranlocal) t2.load(ref);
        tranlocal.value++;
        t2.commit();

        try {
            t1.load(ref);
            fail();
        } catch (LoadTooOldVersionException ex) {
        }

        assertIsActive(t1);
    }

    @Test
    public void loadFailsIfLocked() {
        IntRef ref = new IntRef(0);

        clock.tick();

        Transaction owner = new DummyTransaction();
        ref.___tryLock(owner);

        AlphaTransaction t = stm.startUpdateTransaction("");
        try {
            t.load(ref);
            fail();
        } catch (LoadLockedException ex) {
        }

        assertIsActive(t);
    }

    @Test
    public void loadWithNullArgumentReturnsNull() {
        AlphaTransaction t = startUpdateTransaction();

        AlphaTranlocal result = t.load(null);

        assertNull(result);
        assertIsActive(t);
    }

    @Test
    public void loadOnCommittedValue() {
        AlphaTransaction t1 = startUpdateTransaction();
        IntRef intValue = new IntRef(0);
        t1.commit();

        IntRefTranlocal committed = (IntRefTranlocal) intValue.___load(stm.getTime());

        AlphaTransaction t2 = startUpdateTransaction();
        IntRefTranlocal read = (IntRefTranlocal) t2.load(intValue);
        assertTrue(committed != read);
        assertEquals(intValue, read.getAtomicObject());
        assertEquals(committed.value, read.value);
        assertFalse(read.___writeVersion > 0);
        //version doesn't need to be checked since it is not defined for a non committed value
        assertIsActive(t2);
    }

    @Test
    public void loadOnAlreadyLoadedValue() {
        AlphaTransaction t1 = startUpdateTransaction();
        IntRef intValue = new IntRef(0);
        t1.commit();

        AlphaTransaction t2 = startUpdateTransaction();
        IntRefTranlocal read1 = (IntRefTranlocal) t2.load(intValue);
        IntRefTranlocal read2 = (IntRefTranlocal) t2.load(intValue);
        assertSame(read1, read2);
        assertIsActive(t2);
    }

    @Test
    public void loadOnAlreadyAttachedValue() {
        AlphaTransaction t1 = startUpdateTransaction();
        IntRef intValue = new IntRef(20);
        IntRefTranlocal read1 = (IntRefTranlocal) t1.load(intValue);

        assertEquals(intValue, read1.getAtomicObject());
        assertEquals(0, read1.___writeVersion);
        assertIsActive(t1);
        t1.commit();

        IntRefTranlocal read2 = (IntRefTranlocal) intValue.___load(stm.getTime());
        assertSame(read1, read2);
    }

    @Test
    public void loadOnDifferentTransactionsReturnDifferentInstances() {
        IntRef value = new IntRef(1);

        AlphaTransaction t1 = stm.startUpdateTransaction(null);
        IntRefTranlocal found1 = (IntRefTranlocal) t1.load(value);

        AlphaTransaction t2 = stm.startUpdateTransaction(null);
        IntRefTranlocal found2 = (IntRefTranlocal) t2.load(value);

        assertNotSame(found1, found2);
        assertEquals(found1.value, found2.value);
        assertEquals(found1.___writeVersion, found2.___writeVersion);
        assertEquals(found1.getAtomicObject(), found2.getAtomicObject());
        //version doesn't need to be checked since it is undefined while not committed.
    }

    @Test
    public void loadReturnsNewTranlocalOnUncommittedObject() {
        IntRef value = IntRef.createUncommitted();

        AlphaTransaction t = stm.startUpdateTransaction(null);

        IntRefTranlocal tranlocal = (IntRefTranlocal) t.load(value);
        assertFalse(tranlocal.___writeVersion > 0);
        assertEquals(0, tranlocal.___writeVersion);

        assertIsActive(t);
    }

    @Test
    public void loadFailsIfTransactionAlreadyCommitted() {
        AlphaTransaction t1 = startUpdateTransaction();
        IntRef value = new IntRef(0);
        t1.commit();

        AlphaTransaction t2 = startUpdateTransaction();
        t2.commit();

        try {
            t2.load(value);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(t2);
    }

    @Test
    public void loadFailsIfTransactionAlreadyAborted() {
        AlphaTransaction t1 = startUpdateTransaction();
        IntRef value = new IntRef(0);
        t1.commit();

        AlphaTransaction t2 = startUpdateTransaction();
        t2.abort();

        try {
            t2.load(value);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(t2);
    }
}
