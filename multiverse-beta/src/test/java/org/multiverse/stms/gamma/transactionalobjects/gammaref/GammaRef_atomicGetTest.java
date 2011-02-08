package org.multiverse.stms.gamma.transactionalobjects.gammaref;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertOrecValue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.assertGlobalConflictCount;
import static org.multiverse.stms.gamma.GammaTestUtils.assertVersionAndValue;
import static org.multiverse.stms.gamma.GammaTestUtils.makeReadBiased;

public class GammaRef_atomicGetTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    // ==================== write biased ====================

    @Test
    public void writeBiased_whenReadLocked() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();
        GammaTransaction tx = stm.newDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);
        long orecValue = ref.orec;
        long conflictCount = stm.globalConflictCounter.count();

        String result = ref.atomicGet();

        assertSame(result, initialValue);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertOrecValue(ref, orecValue);
        assertGlobalConflictCount(stm, conflictCount);
    }

    @Test
    public void writeBiased_whenWriteLocked() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();
        GammaTransaction tx = stm.newDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        long orecValue = ref.orec;
        long conflictCount = stm.globalConflictCounter.count();

        String result = ref.atomicGet();

        assertSame(result, initialValue);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertOrecValue(ref, orecValue);
        assertGlobalConflictCount(stm, conflictCount);
    }

    @Test
    public void writeBiased_whenExclusiveLocked_thenLockedException() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();
        GammaTransaction tx = stm.newDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);
        long orecValue = ref.orec;
        long conflictCount = stm.globalConflictCounter.count();

        try {
            ref.atomicGet();
            fail();
        } catch (LockedException expected) {
        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertOrecValue(ref, orecValue);
        assertGlobalConflictCount(stm, conflictCount);
    }

    @Test
    public void writeBiased_whenNull() {
        GammaRef<String> ref = new GammaRef<String>(stm, null);
        long initialVersion = ref.getVersion();

        long conflictCount = stm.globalConflictCounter.count();
        long orecValue = ref.orec;

        String result = ref.atomicGet();

        assertNull(result);
        assertVersionAndValue(ref, initialVersion, null);
        assertOrecValue(ref, orecValue);
        assertGlobalConflictCount(stm, conflictCount);
    }

    @Test
    public void writeBiased_whenNotNull() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();
        long conflictCount = stm.globalConflictCounter.count();
        long orecValue = ref.orec;

        String result = ref.atomicGet();

        assertSame(initialValue, result);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertOrecValue(ref, orecValue);
        assertGlobalConflictCount(stm, conflictCount);
    }

     // ==================== read biased ====================

    @Test
    public void readBiased_whenReadLocked() {
        String initialValue = "foo";
        GammaRef<String> ref = makeReadBiased(new GammaRef<String>(stm, initialValue));
        long initialVersion = ref.getVersion();
        GammaTransaction tx = stm.newDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);
        long orecValue = ref.orec;
        long conflictCount = stm.globalConflictCounter.count();

        String result = ref.atomicGet();

        assertSame(result, initialValue);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertOrecValue(ref, orecValue);
        assertGlobalConflictCount(stm, conflictCount);
    }

    @Test
    public void readBiased_whenWriteLocked() {
        String initialValue = "foo";
        GammaRef<String> ref = makeReadBiased(new GammaRef<String>(stm, initialValue));
        long initialVersion = ref.getVersion();
        GammaTransaction tx = stm.newDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        long orecValue = ref.orec;
        long conflictCount = stm.globalConflictCounter.count();

        String result = ref.atomicGet();

        assertSame(result, initialValue);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertOrecValue(ref, orecValue);
        assertGlobalConflictCount(stm, conflictCount);
    }

    @Test
    public void readBiased_whenExclusiveLocked_thenLockedException() {
        String initialValue = "foo";
        GammaRef<String> ref = makeReadBiased(new GammaRef<String>(stm, initialValue));
        long initialVersion = ref.getVersion();
        GammaTransaction tx = stm.newDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);
        long orecValue = ref.orec;
        long conflictCount = stm.globalConflictCounter.count();

        try {
            ref.atomicGet();
            fail();
        } catch (LockedException expected) {
        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertOrecValue(ref, orecValue);
        assertGlobalConflictCount(stm, conflictCount);
    }

    @Test
    public void readBiased_whenNull() {
        GammaRef<String> ref = makeReadBiased(new GammaRef<String>(stm, null));
        long initialVersion = ref.getVersion();

        long conflictCount = stm.globalConflictCounter.count();
        long orecValue = ref.orec;

        String result = ref.atomicGet();

        assertNull(result);
        assertVersionAndValue(ref, initialVersion, null);
        assertOrecValue(ref, orecValue);
        assertGlobalConflictCount(stm, conflictCount);
    }

    @Test
    public void readBiased_whenNotNull() {
        String initialValue = "foo";
        GammaRef<String> ref = makeReadBiased(new GammaRef<String>(stm, initialValue));
        long initialVersion = ref.getVersion();
        long conflictCount = stm.globalConflictCounter.count();
        long orecValue = ref.orec;

        String result = ref.atomicGet();

        assertSame(initialValue, result);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertOrecValue(ref, orecValue);
        assertGlobalConflictCount(stm, conflictCount);
    }
}
