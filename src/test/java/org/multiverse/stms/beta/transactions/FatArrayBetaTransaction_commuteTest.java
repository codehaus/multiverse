package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FatArrayBetaTransaction_commuteTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        try {
            tx.commute(ref, null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsAborted(tx);
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNotOpenedBefore() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        LongFunction function = mock(LongFunction.class);

        tx.commute(ref, function);

        assertIsActive(tx);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
    }

    @Test
    public void whenCommuteThenConflictCounterNotSet() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        long localConflictCount = tx.getLocalConflictCounter().get();

        LongFunction function = mock(LongFunction.class);

        stm.getGlobalConflictCounter().signalConflict(new BetaLongRef(stm));

        tx.commute(ref, function);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
    }

    @Test
    public void whenMultipleCommutesOnSameReference() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);
        LongFunction function3 = mock(LongFunction.class);

        tx.commute(ref, function1);
        tx.commute(ref, function2);
        tx.commute(ref, function3);

        assertIsActive(tx);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function3, function2, function1);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
    }

    @Test
    public void whenMultipleCommutesInTransaction() {
        BetaLongRef ref1 = BetaStmUtils.newLongRef(stm);
        BetaLongRef ref2 = BetaStmUtils.newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);

        tx.commute(ref1, function1);
        tx.commute(ref2, function2);

        assertIsActive(tx);
        LongRefTranlocal tranlocal1 = (LongRefTranlocal) tx.get(ref1);
        assertHasCommutingFunctions(tranlocal1, function1);
        LongRefTranlocal tranlocal2 = (LongRefTranlocal) tx.get(ref2);
        assertHasCommutingFunctions(tranlocal2, function2);
    }


    @Test
    public void whenOverflow() {
        BetaLongRef ref1 = BetaStmUtils.newLongRef(stm);
        BetaLongRef ref2 = BetaStmUtils.newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 1);
        config.init();
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);
        tx.commute(ref1, function1);

        try {
            tx.commute(ref2, function2);
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertIsAborted(tx);
        assertEquals(2, tx.getConfiguration().getSpeculativeConfiguration().minimalLength);

        verifyZeroInteractions(function1);
        assertSurplus(0, ref1);
        assertUnlocked(ref1);

        verifyZeroInteractions(function2);
        assertSurplus(0, ref2);
        assertUnlocked(ref2);
    }


    @Test
    public void whenAlreadyOpenedForCommute() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);

        tx.commute(ref, function1);
        tx.commute(ref, function2);

        assertIsActive(tx);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);

        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function2, function1);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        verifyZeroInteractions(function1);
        verifyZeroInteractions(function2);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenOpenedForWritingAndApplyFunction() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);
        tx.commute(ref, new IncLongFunction());

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        assertIsActive(tx);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertSame(read, tranlocal.read);
        assertEquals(101, tranlocal.value);
        assertHasNoCommutingFunctions(tranlocal);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal tranlocal = tx.openForWrite(ref, false);
        tx.commute(ref, new IncLongFunction());

        assertIsActive(tx);
        assertSame(ref, tranlocal.owner);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertSame(committed, tranlocal.read);
        assertEquals(101, tranlocal.value);
        assertHasNoCommutingFunctions(tranlocal);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
    }

    @Test
    @Ignore
    public void whenPessimisticTransactionAndNotOpenedBefore_thenIgnorePessimisticSetting() {

    }

    @Test
    public void whenFreshObject() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref);
        tx.commute(ref, new IncLongFunction());

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);

        assertIsActive(tx);
        assertSame(ref, tranlocal.owner);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertEquals(1, tranlocal.value);
        assertHasNoCommutingFunctions(tranlocal);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
    }

    @Test
    public void whenLocked_thenNoProblem() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, true);

        LongFunction function = mock(LongFunction.class);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commute(ref, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        assertIsActive(tx);
        assertTrue(commuting.isCommuting);
        assertFalse(commuting.isCommitted);
        assertNull(commuting.read);
        assertHasCommutingFunctions(commuting, function);
        assertLocked(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenReadonlyTransaction() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        LongFunction function = mock(LongFunction.class);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadonly(true);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

        try {
            tx.commute(ref, function);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
        verifyZeroInteractions(function);
    }

    @Test
    public void whenPrepared() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare();

        LongFunction function = mock(LongFunction.class);

        try {
            tx.commute(ref, function);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit();

        LongFunction function = mock(LongFunction.class);

        try {
            tx.commute(ref, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenAlreadyAborted_thenDeadTransactionException() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        LongFunction function = mock(LongFunction.class);

        try {
            tx.commute(ref, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }
}
