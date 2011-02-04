package org.multiverse.stms.gamma.transactionalobjects.orec;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_tryLockAndCheckConflictTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    //different locks by other
    //conflicts

    @Test
    public void updateBiased_whenOtherHasLocked() {
        updateBiased_whenOtherHasLocked(LockMode.Read, LockMode.None, true);
        updateBiased_whenOtherHasLocked(LockMode.Read, LockMode.Read, true);
        updateBiased_whenOtherHasLocked(LockMode.Read, LockMode.Write, false);
        updateBiased_whenOtherHasLocked(LockMode.Read, LockMode.Exclusive, false);

        updateBiased_whenOtherHasLocked(LockMode.Write, LockMode.None, true);
        updateBiased_whenOtherHasLocked(LockMode.Write, LockMode.Read, false);
        updateBiased_whenOtherHasLocked(LockMode.Write, LockMode.Write, false);
        updateBiased_whenOtherHasLocked(LockMode.Write, LockMode.Exclusive, false);

        //updateBiased_whenOtherHasLocked(LockMode.Exclusive, LockMode.None, false);
        updateBiased_whenOtherHasLocked(LockMode.Exclusive, LockMode.Read, false);
        updateBiased_whenOtherHasLocked(LockMode.Exclusive, LockMode.Write, false);
        updateBiased_whenOtherHasLocked(LockMode.Exclusive, LockMode.Exclusive, false);
    }

    public void updateBiased_whenOtherHasLocked(LockMode otherLockMode, LockMode thisLockMode, boolean success) {
        GammaLongRef ref = new GammaLongRef(stm);
        //tx.arriveEnabled = arriveNeeded;
        GammaRefTranlocal tranlocal = ref.openForRead(stm.newDefaultTransaction(), LOCKMODE_NONE);

        ref.openForRead(stm.newDefaultTransaction(), otherLockMode.asInt());


        //todo: null transaction
        boolean result = ref.tryLockAndCheckConflict(null, tranlocal, 1, thisLockMode.asInt());

        assertEquals(success, result);
        //assertEquals(expectedLockMode.asInt(), tranlocal.getLockMode());
        //assertLockMode(ref, expectedLockMode);
        //assertSurplus(ref, expectedSurplus);
    }

    @Test
    public void updateBiased_whenLockFreeAndArriveNeeded() {
        updateBiased(true, LockMode.None, LockMode.None, LockMode.None, 1);
        updateBiased(true, LockMode.None, LockMode.Read, LockMode.Read, 1);
        updateBiased(true, LockMode.None, LockMode.Write, LockMode.Write, 1);
        updateBiased(true, LockMode.None, LockMode.Exclusive, LockMode.Exclusive, 1);

        updateBiased(true, LockMode.Read, LockMode.None, LockMode.Read, 1);
        updateBiased(true, LockMode.Read, LockMode.Read, LockMode.Read, 1);
        updateBiased(true, LockMode.Read, LockMode.Write, LockMode.Write, 1);
        updateBiased(true, LockMode.Read, LockMode.Exclusive, LockMode.Exclusive, 1);

        updateBiased(true, LockMode.Write, LockMode.None, LockMode.Write, 1);
        updateBiased(true, LockMode.Write, LockMode.Read, LockMode.Write, 1);
        updateBiased(true, LockMode.Write, LockMode.Write, LockMode.Write, 1);
        updateBiased(true, LockMode.Write, LockMode.Exclusive, LockMode.Exclusive, 1);

        updateBiased(true, LockMode.Exclusive, LockMode.None, LockMode.Exclusive, 1);
        updateBiased(true, LockMode.Exclusive, LockMode.Read, LockMode.Exclusive, 1);
        updateBiased(true, LockMode.Exclusive, LockMode.Write, LockMode.Exclusive, 1);
        updateBiased(true, LockMode.Exclusive, LockMode.Exclusive, LockMode.Exclusive, 1);
    }

    @Test
    public void updateBiased_whenLockFreeAndNoArriveNeeded() {
        updateBiased(false, LockMode.None, LockMode.None, LockMode.None, 0);
        updateBiased(false, LockMode.None, LockMode.Read, LockMode.Read, 1);
        updateBiased(false, LockMode.None, LockMode.Write, LockMode.Write, 1);
        updateBiased(false, LockMode.None, LockMode.Exclusive, LockMode.Exclusive, 1);

        updateBiased(false, LockMode.Read, LockMode.None, LockMode.Read, 1);
        updateBiased(false, LockMode.Read, LockMode.Read, LockMode.Read, 1);
        updateBiased(false, LockMode.Read, LockMode.Write, LockMode.Write, 1);
        updateBiased(false, LockMode.Read, LockMode.Exclusive, LockMode.Exclusive, 1);

        updateBiased(false, LockMode.Write, LockMode.None, LockMode.Write, 1);
        updateBiased(false, LockMode.Write, LockMode.Read, LockMode.Write, 1);
        updateBiased(false, LockMode.Write, LockMode.Write, LockMode.Write, 1);
        updateBiased(false, LockMode.Write, LockMode.Exclusive, LockMode.Exclusive, 1);

        updateBiased(false, LockMode.Exclusive, LockMode.None, LockMode.Exclusive, 1);
        updateBiased(false, LockMode.Exclusive, LockMode.Read, LockMode.Exclusive, 1);
        updateBiased(false, LockMode.Exclusive, LockMode.Write, LockMode.Exclusive, 1);
        updateBiased(false, LockMode.Exclusive, LockMode.Exclusive, LockMode.Exclusive, 1);
    }

    public void updateBiased(boolean arriveNeeded, LockMode firstLockMode, LockMode secondLockMode,
                             LockMode expectedLockMode, int expectedSurplus) {
        GammaLongRef ref = new GammaLongRef(stm);
        GammaTransaction tx = stm.newDefaultTransaction();
        tx.richmansMansConflictScan = arriveNeeded;
        GammaRefTranlocal tranlocal = ref.openForRead(tx, firstLockMode.asInt());

        boolean result = ref.tryLockAndCheckConflict(tx, tranlocal, 1, secondLockMode.asInt());

        assertTrue(result);
        assertEquals(expectedLockMode.asInt(), tranlocal.getLockMode());
        assertLockMode(ref, expectedLockMode);
        assertSurplus(ref, expectedSurplus);
    }

    @Test
    public void readBiased_whenLockFree() {
        readBiased(LockMode.None, LockMode.None, LockMode.None);
        readBiased(LockMode.None, LockMode.Read, LockMode.Read);
        readBiased(LockMode.None, LockMode.Write, LockMode.Write);
        readBiased(LockMode.None, LockMode.Exclusive, LockMode.Exclusive);

        readBiased(LockMode.Read, LockMode.None, LockMode.Read);
        readBiased(LockMode.Read, LockMode.Read, LockMode.Read);
        readBiased(LockMode.Read, LockMode.Write, LockMode.Write);
        readBiased(LockMode.Read, LockMode.Exclusive, LockMode.Exclusive);

        readBiased(LockMode.Write, LockMode.None, LockMode.Write);
        readBiased(LockMode.Write, LockMode.Read, LockMode.Write);
        readBiased(LockMode.Write, LockMode.Write, LockMode.Write);
        readBiased(LockMode.Write, LockMode.Exclusive, LockMode.Exclusive);

        readBiased(LockMode.Exclusive, LockMode.None, LockMode.Exclusive);
        readBiased(LockMode.Exclusive, LockMode.Read, LockMode.Exclusive);
        readBiased(LockMode.Exclusive, LockMode.Write, LockMode.Exclusive);
        readBiased(LockMode.Exclusive, LockMode.Exclusive, LockMode.Exclusive);
    }

    public void readBiased(LockMode firstLockMode, LockMode secondLockMode, LockMode expectedLockMode) {
        GammaLongRef ref = makeReadBiased(new GammaLongRef(stm));
        GammaTransaction tx = stm.newDefaultTransaction();
        tx.richmansMansConflictScan = true;
        GammaRefTranlocal tranlocal = ref.openForRead(tx, firstLockMode.asInt());

        boolean result = ref.tryLockAndCheckConflict(tx, tranlocal, 1, secondLockMode.asInt());

        assertTrue(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(expectedLockMode.asInt(), tranlocal.getLockMode());
        assertLockMode(ref, expectedLockMode);
        assertSurplus(ref, 1);
    }

    @Test
    @Ignore
    public void lockNotFree() {

    }
}
