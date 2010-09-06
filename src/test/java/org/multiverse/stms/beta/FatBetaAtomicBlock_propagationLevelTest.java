package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.PropagationLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicIntClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.NoTransactionAllowedException;
import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class FatBetaAtomicBlock_propagationLevelTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNeverAndTransactionAvailable_thenNoTransactionAllowedException() {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Never)
                .buildAtomicBlock();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        setThreadLocalTransaction(otherTx);

        AtomicVoidClosure closure = mock(AtomicVoidClosure.class);

        try {
            block.execute(closure);
            fail();
        } catch (NoTransactionAllowedException expected) {
        }

        verifyZeroInteractions(closure);
        assertIsActive(otherTx);
        assertSame(otherTx, getThreadLocalTransaction());
    }

    @Test
    public void whenNeverAndNoTransactionAvailable() {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Never)
                .buildAtomicBlock();

        AtomicIntClosure closure = new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                assertNull(tx);
                return 10;
            }
        };

        int result = block.execute(closure);

        assertEquals(10, result);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenMandatoryAndNoTransactionAvailable_thenNoTransactionFoundException() {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Mandatory)
                .buildAtomicBlock();

        AtomicVoidClosure closure = mock(AtomicVoidClosure.class);

        try {
            block.execute(closure);
            fail();
        } catch (NoTransactionFoundException expected) {
        }

        verifyZeroInteractions(closure);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenMandatoryAndTransactionAvailable_thenExistingTransactionUsed() {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Mandatory)
                .buildAtomicBlock();

        final BetaTransaction otherTx = stm.startDefaultTransaction();
        setThreadLocalTransaction(otherTx);

        AtomicIntClosure closure = new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                assertSame(otherTx, tx);
                return 10;
            }
        };

        int result = block.execute(closure);

        assertEquals(10, result);
        assertIsActive(otherTx);
        assertSame(otherTx, getThreadLocalTransaction());
    }

    @Test
    public void whenRequiresAndNoTransactionAvailable_thenNewTransactionUsed() {
        BetaTransactionFactory txFactory = stm.createTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Requires)
                .build();

        final BetaLongRef ref = createLongRef(stm);

        AtomicIntClosure closure = new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                assertNotNull(tx);
                BetaTransaction btx = (BetaTransaction) tx;
                btx.openForWrite(ref, false).value++;
                return 10;
            }
        };

        int result = new FatBetaAtomicBlock(txFactory).execute(closure);

        assertEquals(10, result);
        assertNull(getThreadLocalTransaction());
        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenRequiresAndTransactionAvailable_thenExistingTransactionUsed() {
        BetaTransactionFactory txFactory = stm.createTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Requires)
                .build();

        final BetaTransaction existingTx = stm.startDefaultTransaction();
        setThreadLocalTransaction(existingTx);

        final BetaLongRef ref = createLongRef(stm);

        AtomicIntClosure closure = new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                assertSame(existingTx, tx);
                BetaTransaction btx = (BetaTransaction) tx;
                btx.openForWrite(ref, false).value++;
                return 10;
            }
        };

        int result = new FatBetaAtomicBlock(txFactory).execute(closure);

        assertEquals(10, result);
        assertSame(existingTx, getThreadLocalTransaction());
        assertIsActive(existingTx);
        //since the value hasn't committed yet, it still is zero (the value before the transaction began).
        assertEquals(0, ref.___unsafeLoad().value);
    }

    @Test
    public void whenRequiresNewAndNoTransactionAvailable_thenNewTransactionCreated() {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.RequiresNew)
                .buildAtomicBlock();

        final BetaLongRef ref = createLongRef(stm, 0);

        AtomicIntClosure closure = new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                assertNotNull(tx);
                BetaTransaction btx = (BetaTransaction) tx;
                btx.openForWrite(ref, false).value++;
                return 10;
            }
        };

        int result = block.execute(closure);

        assertEquals(10, result);
        assertEquals(1, ref.___unsafeLoad().value);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenRequiresNewAndTransactionAvailable_thenExistingTransactionSuspended() {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.RequiresNew)
                .buildAtomicBlock();

        final BetaTransaction otherTx = stm.startDefaultTransaction();
        setThreadLocalTransaction(otherTx);

        final BetaLongRef ref = createLongRef(stm, 10);

        AtomicIntClosure closure = new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                assertNotNull(tx);
                assertNotSame(otherTx, tx);
                BetaTransaction btx = (BetaTransaction) tx;
                btx.openForWrite(ref, false).value++;
                return 1;
            }
        };

        int result = block.execute(closure);

        assertEquals(1, result);
        assertEquals(11, ref.___unsafeLoad().value);
        assertSame(otherTx, getThreadLocalTransaction());
        assertIsActive(otherTx);
    }

    @Test
    public void whenSupportsAndTransactionAvailable() {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Supports)
                .buildAtomicBlock();

        final BetaTransaction otherTx = stm.startDefaultTransaction();
        setThreadLocalTransaction(otherTx);

        AtomicIntClosure closure = new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                assertSame(otherTx, tx);
                return 10;
            }
        };

        int result = block.execute(closure);

        assertEquals(10, result);
        assertIsActive(otherTx);
        assertSame(otherTx, getThreadLocalTransaction());
    }

    @Test
    public void whenSupportsAndNoTransactionAvailable() {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setPropagationLevel(PropagationLevel.Supports)
                .buildAtomicBlock();

        AtomicIntClosure closure = new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                assertNull(tx);
                return 10;
            }
        };

        int result = block.execute(closure);

        assertEquals(10, result);
        assertNull(getThreadLocalTransaction());
    }
}
