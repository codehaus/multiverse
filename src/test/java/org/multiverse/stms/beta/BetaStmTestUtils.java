package org.multiverse.stms.beta;

import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.stms.beta.transactionalobjects.*;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;
import org.multiverse.stms.beta.transactions.LeanMonoBetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertEqualsDouble;
import static org.multiverse.TestUtils.getField;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class BetaStmTestUtils implements BetaStmConstants {

    public static RetryLatch getFirstListener(BetaLongRef ref) {
        Listeners listeners = (Listeners) getField(ref, "___listeners");
        assertNotNull(listeners);
        RetryLatch latch = listeners.listener;
        assertNotNull(latch);
        return latch;
    }

    public static void assertRefHasNoLocks(VeryAbstractBetaTransactionalObject ref) {
        assertNull(ref.___getLockOwner());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
    }

    public static void assertRefHasUpdateLock(VeryAbstractBetaTransactionalObject ref, BetaTransaction lockOwner) {
        assertSame(lockOwner, ref.___getLockOwner());
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
    }

    public static void assertRefLockMode(VeryAbstractBetaTransactionalObject ref, BetaTransaction lockOwner, int lockMode) {
        switch (lockMode) {
            case LOCKMODE_NONE:
                assertNull(ref.___getLockOwner());
                assertHasNoUpdateLock(ref);
                assertHasNoCommitLock(ref);
                break;
            case LOCKMODE_UPDATE:
                assertSame(lockOwner, ref.___getLockOwner());
                assertHasUpdateLock(ref);
                assertHasNoCommitLock(ref);
                break;
            case LOCKMODE_COMMIT:
                assertSame(lockOwner, ref.___getLockOwner());
                assertHasNoUpdateLock(ref);
                assertHasCommitLock(ref);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static void assertRefHasCommitLock(VeryAbstractBetaTransactionalObject ref, BetaTransaction lockOwner) {
        assertSame(lockOwner, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
    }

    public static <E> BetaRef<E> newRef(BetaStm stm, E value) {
        BetaTransaction tx = new LeanMonoBetaTransaction(stm);
        BetaRef<E> ref = new BetaRef<E>(tx);
        RefTranlocal<E> tranlocal = tx.openForConstruction(ref);
        tranlocal.value = value;
        tx.commit();
        return ref;
    }

    public static BetaDoubleRef newDoubleRef(BetaStm stm, double value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaDoubleRef ref = new BetaDoubleRef(tx);
        DoubleRefTranlocal tranlocal = tx.openForConstruction(ref);
        tranlocal.value = value;
        tx.commit();
        return ref;
    }

    public static void assertHasVersionAndValue(LongRefTranlocal tranlocal, long expectedVersion, long expectedValue) {
        assertEquals(expectedVersion, tranlocal.version);
        assertEquals(expectedValue, tranlocal.value);
    }

    public static void assertTranlocalHasNoLock(Tranlocal tranlocal) {
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
    }

    public static void assertTranlocalHasCommitLock(Tranlocal tranlocal) {
        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
    }

    public static void assertTranlocalHasUpdateLock(Tranlocal tranlocal) {
        assertEquals(LOCKMODE_UPDATE, tranlocal.getLockMode());
    }

    public static BetaRef newRef(BetaStm stm) {
        return newRef(stm, null);
    }

    public static BetaIntRef newIntRef(BetaStm stm) {
        return newIntRef(stm, 0);
    }

    public static BetaIntRef newIntRef(BetaStm stm, int value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaIntRef ref = new BetaIntRef(tx);
        tx.openForConstruction(ref).value = value;
        tx.commit();
        return ref;
    }

    public static BetaLongRef newLongRef(BetaStm stm) {
        return newLongRef(stm, 0);
    }


    public static BetaLongRef newLongRef(BetaStm stm, long value) {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal tranlocal = tx.openForConstruction(ref);
        tranlocal.value = value;
        tx.commit();
        return ref;
    }

    public static void assertVersionAndValue(BetaBooleanRef ref, long version, boolean value) {
        assertEquals("version doesn't match", version, ref.getVersion());
        assertEquals("value doesn't match", value, ref.___weakRead());
    }

    public static void assertVersionAndValue(BetaDoubleRef ref, long version, double value) {
        assertEquals("version doesn't match", version, ref.getVersion());
        assertEqualsDouble("value doesn't match", value, ref.___weakRead());
    }


    public static void assertVersionAndValue(BetaIntRef ref, long version, int value) {
        assertEquals("version doesn't match", version, ref.getVersion());
        assertEquals("value doesn't match", value, ref.___weakRead());
    }

    public static void assertVersionAndValue(BetaLongRef ref, long version, long value) {
        assertEquals("version doesn't match", version, ref.getVersion());
        assertEquals("value doesn't match", value, ref.___weakRead());
    }

    public static void assertVersionAndValue(BetaRef ref, long version, Object value) {
        assertEquals("version doesn't match", version, ref.getVersion());
        assertSame("value doesn't match", value, ref.___weakRead());
    }

    public static BetaLongRef newReadBiasedLongRef(BetaStm stm, long value) {
        BetaLongRef ref = newLongRef(stm, value);

        for (int k = 0; k < ref.___getOrec().___getReadBiasedThreshold(); k++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            ref.get(tx);
            tx.commit();
        }

        return ref;
    }

    public static BetaLongRef newReadBiasedLongRef(BetaStm stm) {
        return newReadBiasedLongRef(stm, 0);
    }

}
