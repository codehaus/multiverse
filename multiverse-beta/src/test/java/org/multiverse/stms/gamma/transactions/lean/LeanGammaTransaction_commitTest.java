package org.multiverse.stms.gamma.transactions.lean;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class LeanGammaTransaction_commitTest<T extends GammaTransaction> {

    public GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    public abstract T newTransaction();

    public abstract void assertClearedAfterCommit();

    public abstract void assertClearedAfterAbort();

    @Test
    public void whenUnused() {
        T tx = newTransaction();
        tx.commit();

        assertIsCommitted();
        assertClearedAfterCommit();
    }

    @Test
    public void whenNonDirtyUpdate() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx);
        tranlocal.ref_value = initialValue;
        tx.commit();

        assertNull(tranlocal.owner);
        assertNull(tranlocal.ref_value);
        assertNull(tranlocal.ref_oldValue);
        assertIsCommitted(tx);
        assertSurplus(ref, 0);
        assertReadonlyCount(ref, 0);
        assertUpdateBiased(ref);
        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue);
    }

    @Test
    public void whenDirtyUpdate() {
        String initialValue = "foo";
        String newValue = "bar";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx);
        tranlocal.ref_value = newValue;
        tx.commit();

        assertNull(tranlocal.owner);
        assertNull(tranlocal.ref_value);
        assertNull(tranlocal.ref_oldValue);
        assertIsCommitted(tx);
        assertSurplus(ref, 0);
        assertReadonlyCount(ref, 0);
        assertUpdateBiased(ref);
        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, newValue);
    }

    @Test
    public void whenAlreadyPreparedAndUnused() {
        T tx = newTransaction();
        tx.prepare();

        tx.commit();

        assertIsCommitted(tx);
    }

    @Test
    public void whenAlreadyCommitted() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        tx.commit();

        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyAborted() {
        T tx = newTransaction();
        tx.abort();

        try {
            tx.commit();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }
}
