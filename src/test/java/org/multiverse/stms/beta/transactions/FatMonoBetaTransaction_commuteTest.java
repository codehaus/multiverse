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
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FatMonoBetaTransaction_commuteTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenNotOpenedBefore() {
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        LongFunction function = mock(LongFunction.class);

        tx.commute(ref, pool, function);

        assertActive(tx);
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
    public void whenAlreadyOpenedForCommute() {
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);

        tx.commute(ref, pool, function1);
        tx.commute(ref, pool, function2);

        assertActive(tx);
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
        BetaLongRef ref = createLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);
        tx.commute(ref, pool, new IncLongFunction());

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        assertActive(tx);
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
        BetaLongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal tranlocal = tx.openForWrite(ref, false, pool);
        tx.commute(ref, pool, new IncLongFunction());

        assertActive(tx);
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
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref, pool);
        tx.commute(ref, pool, new IncLongFunction());

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);

        assertActive(tx);
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
    public void whenOverflow() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);
        tx.commute(ref1, pool, function1);

        try {
            tx.commute(ref2, pool, function2);
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertAborted(tx);
        assertEquals(2, tx.getConfiguration().getSpeculativeConfig().getMinimalLength());

        verifyZeroInteractions(function1);
        assertSurplus(0, ref1);
        assertUnlocked(ref1);

        verifyZeroInteractions(function2);
        assertSurplus(0, ref2);
        assertUnlocked(ref2);
    }

    @Test
    public void whenCommuteThenConflictCounterNotSet() {
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        long localConflictCount = tx.getLocalConflictCounter().get();

        LongFunction function = mock(LongFunction.class);

        stm.getGlobalConflictCounter().signalConflict(new BetaLongRef());

        tx.commute(ref, pool, function);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertActive(tx);
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
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);
        LongFunction function3 = mock(LongFunction.class);

        tx.commute(ref, pool, function1);
        tx.commute(ref, pool, function2);
        tx.commute(ref, pool, function3);

        assertActive(tx);
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
    public void whenLocked_thenNoProblem() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, true, pool);

        LongFunction function = mock(LongFunction.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commute(ref, pool, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        assertActive(tx);
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
    public void whenReadonlyTransaction_thenReadonlyException() {
        BetaLongRef ref = createLongRef(stm);

        LongFunction function = mock(LongFunction.class);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadonly(true);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);

        try {
            tx.commute(ref, pool, function);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertAborted(tx);
        verifyZeroInteractions(function);
    }

    @Test
    public void whenPrepared_thenPreparedException() {
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare(pool);

        LongFunction function = mock(LongFunction.class);

        try {
            tx.commute(ref, pool, function);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit(pool);

        LongFunction function = mock(LongFunction.class);

        try {
            tx.commute(ref, pool, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }

    @Test
    public void whenAlreadyAborted_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort();

        LongFunction function = mock(LongFunction.class);

        try {
            tx.commute(ref, pool, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }
}
