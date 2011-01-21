package org.multiverse.stms.gamma.transactions.lean;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.*;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class LeanGammaTransaction_openForWriteTest<T extends GammaTransaction> implements GammaConstants {

    public GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    public abstract T newTransaction();

    @Test
    public void whenIntRef_thenSpeculativeConfigurationError() {
        int initialValue = 10;
        GammaIntRef ref = new GammaIntRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        try {
            ref.openForWrite(tx);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSpeculativeConfigurationNonRefTypeRequired(tx);
    }

    @Test
    public void whenBooleanRef_thenSpeculativeConfigurationError() {
        boolean initialValue = true;
        GammaBooleanRef ref = new GammaBooleanRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        try {
            ref.openForWrite(tx);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSpeculativeConfigurationNonRefTypeRequired(tx);
    }

    @Test
    public void whenDoubleRef_thenSpeculativeConfigurationError() {
        double initialValue = 10;
        GammaDoubleRef ref = new GammaDoubleRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        try {
            ref.openForWrite(tx);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSpeculativeConfigurationNonRefTypeRequired(tx);
    }

    @Test
    public void whenLongRef_thenSpeculativeConfigurationError() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        try {
            ref.openForWrite(tx);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSpeculativeConfigurationNonRefTypeRequired(tx);
    }

    @Test
    public void whenOverflowing() {

    }

    @Test
    public void whenNotOpenedBefore() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx);

        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertSame(initialValue, tranlocal.ref_value);
        assertSame(initialValue, tranlocal.ref_oldValue);
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertEquals(initialVersion, tranlocal.version);

        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal read = ref.openForRead(tx);
        GammaRefTranlocal tranlocal = ref.openForWrite(tx);

        assertNotNull(tranlocal);
        assertSame(read, tranlocal);
        assertSame(ref, tranlocal.owner);
        assertSame(initialValue, tranlocal.ref_value);
        assertSame(initialValue, tranlocal.ref_oldValue);
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertEquals(initialVersion, tranlocal.version);

        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal first = ref.openForWrite(tx);
        GammaRefTranlocal tranlocal = ref.openForWrite(tx);

        assertNotNull(tranlocal);
        assertSame(first, tranlocal);
        assertSame(ref, tranlocal.owner);
        assertSame(initialValue, tranlocal.ref_value);
        assertSame(initialValue, tranlocal.ref_oldValue);
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertEquals(initialVersion, tranlocal.version);

        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyPreparedAndunused() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        tx.prepare();

        try {
            ref.openForWrite(tx);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyCommitted() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        tx.commit();

        try {
            ref.openForWrite(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyAborted() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        tx.abort();

        try {
            ref.openForWrite(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }
}
