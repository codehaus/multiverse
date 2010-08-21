package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FatArrayTreeBetaTransaction_commuteTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenNotOpenedBefore() {
        LongRef ref = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

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
        LongRef ref = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

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
        LongRef ref = createLongRef(stm, 100);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
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
    @Ignore
    public void testingOfConflictCounter() {

    }

    @Test
    public void whenLocked_thenNoProblem() {
        LongRef ref = createLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, true, pool);

        LongFunction function = mock(LongFunction.class);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
    public void whenReadonlyTransaction() {
        LongRef ref = createLongRef(stm);

        LongFunction function = mock(LongFunction.class);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadonly(true);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

        try {
            tx.commute(ref, pool, function);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertAborted(tx);
        verifyZeroInteractions(function);
    }

    @Test
    public void whenPrepared() {
        LongRef ref = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        LongRef ref = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        LongRef ref = createLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
