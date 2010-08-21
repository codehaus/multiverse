package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.InvisibleCheckedException;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class BetaAtomicBlock_exceptionsTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void executeChecked_whenCheckedExceptionThrown() {
        AtomicBlock block = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        final LongRef ref = createLongRef(stm, 10);

        final Exception ex = new Exception();

        try {
            block.executeChecked(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    btx.openForWrite(ref, false, pool).value++;
                    throw ex;
                }
            });
            fail();
        } catch (Exception expected) {
            assertSame(ex, expected);
        }

        assertEquals(10, ref.___unsafeLoad().value);
    }

    @Test
    public void executeChecked_whenRuntimeExceptionThrown() throws Exception {
        AtomicBlock block = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        final LongRef ref = createLongRef(stm, 10);

        final RuntimeException ex = new RuntimeException();

        try {
            block.executeChecked(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    btx.openForWrite(ref, false, pool).value++;
                    throw ex;
                }
            });
            fail();
        } catch (RuntimeException expected) {
            assertSame(ex, expected);
        }

        assertEquals(10, ref.___unsafeLoad().value);
    }


    @Test
    public void executeChecked_whenErrorThrown() throws Exception {
        AtomicBlock block = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        final LongRef ref = createLongRef(stm, 10);

        final Error ex = new Error();

        try {
            block.executeChecked(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    btx.openForWrite(ref, false, pool).value++;
                    throw ex;
                }
            });
            fail();
        } catch (Error expected) {
            assertSame(ex, expected);
        }

        assertEquals(10, ref.___unsafeLoad().value);
    }

     @Test
    public void execute_whenCheckedExceptionThrown() {
        AtomicBlock block = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        final LongRef ref = createLongRef(stm, 10);

        final Exception ex = new Exception();

        try {
            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    btx.openForWrite(ref, false, pool).value++;
                    throw ex;
                }
            });
            fail();
        } catch (InvisibleCheckedException expected) {
            assertSame(ex, expected.getCause());
        }

        assertEquals(10, ref.___unsafeLoad().value);
    }

    @Test
    public void execute_whenRuntimeExceptionThrown() {
        AtomicBlock block = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        final LongRef ref = createLongRef(stm, 10);

        final RuntimeException ex = new RuntimeException();

        try {
            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    btx.openForWrite(ref, false, pool).value++;
                    throw ex;
                }
            });
            fail();
        } catch (RuntimeException expected) {
            assertSame(ex, expected);
        }

        assertEquals(10, ref.___unsafeLoad().value);
    }


    @Test
    public void execute_whenErrorThrown() {
        AtomicBlock block = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        final LongRef ref = createLongRef(stm, 10);

        final Error ex = new Error();

        try {
            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    btx.openForWrite(ref, false, pool).value++;
                    throw ex;
                }
            });
            fail();
        } catch (Error expected) {
            assertSame(ex, expected);
        }

        assertEquals(10, ref.___unsafeLoad().value);
    }


}
