package org.multiverse.stms.gamma;

import org.junit.Assert;
import org.multiverse.TestUtils;
import org.multiverse.api.LockMode;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.api.functions.Function;
import org.multiverse.stms.gamma.transactionalobjects.*;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import java.util.*;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import static org.multiverse.TestUtils.getField;

public class GammaTestUtils implements GammaConstants {

    public static void assertHasCommutingFunctions(GammaRefTranlocal tranlocal, Function... expected) {
        CallableNode current = tranlocal.headCallable;
        List<Function> functions = new LinkedList<Function>();
        while (current != null) {
            functions.add(current.function);
            current = current.next;
        }

        Assert.assertEquals(asList(expected), functions);
    }


    public static void assertHasListeners(AbstractGammaObject ref, RetryLatch... listeners) {
        Set<RetryLatch> expected = new HashSet<RetryLatch>(Arrays.asList(listeners));

        Set<RetryLatch> found = new HashSet<RetryLatch>();
        Listeners l = (Listeners) getField(ref, "listeners");
        while (l != null) {
            found.add(l.listener);
            l = l.next;
        }
        Assert.assertEquals(expected, found);
    }

    public static void assertHasNoListeners(AbstractGammaObject ref) {
        assertHasListeners(ref);
    }

    public static void assertRefHasNoLocks(AbstractGammaObject ref) {
        assertLockMode(ref, LOCKMODE_NONE);
        assertReadLockCount(ref, 0);
    }

    public static void assertRefHasReadLock(AbstractGammaRef ref, GammaTransaction tx) {
        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);
        if (tranlocal == null) {
            fail("A Tranlocal should have been available for a ref that has the read lock");
        }
        Assert.assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertLockMode(ref, LOCKMODE_READ);
    }

    public static void assertRefHasNoLocks(AbstractGammaRef ref, GammaTransaction tx) {
        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);
        if (tranlocal != null) {
            Assert.assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        }
        assertLockMode(ref, LOCKMODE_NONE);
        assertReadLockCount(ref, 0);
    }

    public static void assertRefHasWriteLock(AbstractGammaRef ref, GammaTransaction lockOwner) {
        GammaRefTranlocal tranlocal = lockOwner.getRefTranlocal(ref);
        if (tranlocal == null) {
            fail("A Tranlocal should have been available for a ref that has the write lock");
        }
        Assert.assertEquals(LOCKMODE_WRITE, tranlocal.getLockMode());
        assertLockMode(ref, LOCKMODE_WRITE);
        assertReadLockCount(ref, 0);
    }

    public static void assertRefHasCommitLock(AbstractGammaRef ref, GammaTransaction lockOwner) {
        GammaRefTranlocal tranlocal = lockOwner.getRefTranlocal(ref);
        if (tranlocal == null) {
            fail("A tranlocal should have been stored in the transaction for the ref");
        }
        Assert.assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
        assertLockMode(ref, LOCKMODE_COMMIT);
        assertReadLockCount(ref, 0);
    }

    public static void assertRefHasLockMode(AbstractGammaRef ref, GammaTransaction lockOwner, int lockMode) {
        switch (lockMode) {
            case LOCKMODE_NONE:
                assertRefHasNoLocks(ref, lockOwner);
                break;
            case LOCKMODE_READ:
                assertRefHasReadLock(ref, lockOwner);
                break;
            case LOCKMODE_WRITE:
                assertRefHasWriteLock(ref, lockOwner);
                break;
            case LOCKMODE_COMMIT:
                assertRefHasCommitLock(ref, lockOwner);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    // public static void assertVersionAndValue(GammaBooleanRef ref, long version, boolean value) {
    //    Assert.assertEquals("version doesn't match", version, ref.getVersion());
    //    Assert.assertEquals("value doesn't match", value, ref.___weakRead());
    //}

    //public static void assertVersionAndValue(GammaDoubleRef ref, long version, double value) {
    //    Assert.assertEquals("version doesn't match", version, ref.getVersion());
    //    assertEqualsDouble("value doesn't match", value, ref.___weakRead());
    //}


    //public static void assertVersionAndValue(GammaIntRef ref, long version, int value) {
    //    Assert.assertEquals("version doesn't match", version, ref.getVersion());
    //    Assert.assertEquals("value doesn't match", value, ref.___weakRead());
    //}

    public static void assertVersionAndValue(GammaLongRef ref, long version, long value) {
        Assert.assertEquals("version doesn't match", version, ref.getVersion());
        Assert.assertEquals("value doesn't match", value, ref.atomicWeakGet());
    }

    public static void assertVersionAndValue(GammaBooleanRef ref, long version, boolean value) {
        Assert.assertEquals("version doesn't match", version, ref.getVersion());
        Assert.assertEquals("value doesn't match", value, ref.atomicWeakGet());
    }

    public static void assertVersionAndValue(GammaIntRef ref, long version, int value) {
        Assert.assertEquals("version doesn't match", version, ref.getVersion());
        Assert.assertEquals("value doesn't match", value, ref.atomicWeakGet());
    }

      public static void assertVersionAndValue(GammaDoubleRef ref, long version, double value) {
        Assert.assertEquals("version doesn't match", version, ref.getVersion());
        TestUtils.assertEqualsDouble("value doesn't match", value, ref.atomicWeakGet());
    }

    public static <E> void assertVersionAndValue(GammaRef<E> ref, long version, E value) {
        Assert.assertEquals("version doesn't match", version, ref.getVersion());
        Assert.assertSame("value doesn't match", value, ref.atomicWeakGet());
    }


    //public static void assertVersionAndValue(GammaRef ref, long version, Object value) {
    //    Assert.assertEquals("version doesn't match", version, ref.getVersion());
    //    assertSame("value doesn't match", value, ref.___weakRead());
    //}

    public static void assertReadLockCount(AbstractGammaObject orec, int readLockCount) {
        if (readLockCount > 0) {
            assertEquals(LOCKMODE_READ, orec.atomicGetLockModeAsInt());
        }
        assertEquals(readLockCount, orec.getReadLockCount());
    }

    public static void assertLockMode(GammaObject orec, LockMode lockMode) {
        assertEquals(lockMode, orec.getLock().atomicGetLockMode());
    }

    public static void assertLockMode(AbstractGammaObject orec, int lockMode) {
        assertEquals(lockMode, orec.atomicGetLockModeAsInt());
    }

    public static void assertSurplus(AbstractGammaObject orec, int expectedSurplus) {
        assertEquals(expectedSurplus, orec.getSurplus());
    }

    public static void assertReadBiased(AbstractGammaObject orec, boolean readBiased) {
        if (readBiased) {
            assertReadBiased(orec);
        } else {
            assertUpdateBiased(orec);
        }
    }

    public static void assertReadBiased(AbstractGammaObject orec) {
        assertTrue(orec.isReadBiased());
    }

    public static void assertUpdateBiased(AbstractGammaObject orec) {
        assertFalse(orec.isReadBiased());
    }

    public static void assertReadonlyCount(int expectedReadonlyCount, AbstractGammaObject orec) {
        assertEquals(expectedReadonlyCount, orec.getReadonlyCount());
    }

    public static <O extends AbstractGammaObject> O makeReadBiased(O orec) {
        if (orec.isReadBiased()) {
            return orec;
        }

        int x = orec.getReadonlyCount();
        for (int k = x; k < orec.getReadBiasedThreshold(); k++) {
            orec.arrive(1);
            orec.departAfterReading();
        }

        assertReadBiased(orec);
        assertLockMode(orec, LOCKMODE_NONE);

        return orec;
    }
}
