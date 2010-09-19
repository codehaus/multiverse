package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_loadTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenEnsuredByOther_thenReadAllowed() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        Tranlocal read = ref.___load(1, null, LOCKMODE_NONE);

        assertSame(committed, read);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenPrivatizedByOther_thenReturnLocked() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        Tranlocal read = ref.___load(1, null, LOCKMODE_NONE);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenFirstTimeReadOnUpdateBiased() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongRefTranlocal tranlocal = ref.___load(1, null, LOCKMODE_NONE);

        assertSame(committed, tranlocal);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
    }

    @Test
    public void whenSecondTimeReadOnUpdateBiased() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        ref.___load(1, null, LOCKMODE_NONE);
        LongRefTranlocal tranlocal = ref.___load(1, null, LOCKMODE_NONE);

        assertSame(committed, tranlocal);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(2, ref);
    }

    @Test
    @Ignore
    public void whenReadBiased() {

    }

    @Test
    @Ignore
    public void whenJustBecomeReadBiased(){

    }

    @Test
    public void whenEnsuredByOtherAndEnsure_thenReturnLocked() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = ref.___load(1, tx, LOCKMODE_UPDATE);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenEnsuredByOtherAndPrivatized_thenReturnLocked() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = ref.___load(1, tx, LOCKMODE_COMMIT);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenPrivatizedByOtherAndEnsure_thenReturnLocked() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = ref.___load(1, tx, LOCKMODE_UPDATE);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenPrivatizedByOtherAndPrivatize_thenReturnLocked() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = ref.___load(1, tx, LOCKMODE_COMMIT);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    @Ignore
    public void whenReadByOther(){

    }

    @Test
    @Ignore
    public void whenPendingUpdateByOther(){

    }
}
