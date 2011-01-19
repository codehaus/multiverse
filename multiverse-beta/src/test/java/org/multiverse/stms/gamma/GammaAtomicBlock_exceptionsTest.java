package org.multiverse.stms.gamma;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.InvisibleCheckedException;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class GammaAtomicBlock_exceptionsTest implements GammaConstants {
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void executeChecked_whenCheckedExceptionThrown() {
        AtomicBlock block = stm.newTransactionFactoryBuilder().newAtomicBlock();
        final GammaLongRef ref = new GammaLongRef(stm, 10);

        final Exception ex = new Exception();

        try {
            block.executeChecked(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    ref.openForWrite(btx, LOCKMODE_NONE).long_value++;
                    throw ex;
                }
            });
            fail();
        } catch (Exception expected) {
            assertSame(ex, expected);
        }

        assertEquals(10, ref.atomicGet());
    }

    @Test
    public void executeChecked_whenRuntimeExceptionThrown() throws Exception {
        AtomicBlock block = stm.newTransactionFactoryBuilder().newAtomicBlock();
        final GammaLongRef ref = new GammaLongRef(stm, 10);

        final RuntimeException ex = new RuntimeException();

        try {
            block.executeChecked(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    ref.openForWrite(btx, LOCKMODE_NONE).long_value++;
                    throw ex;
                }
            });
            fail();
        } catch (RuntimeException expected) {
            assertSame(ex, expected);
        }

        assertEquals(10, ref.atomicGet());
    }


    @Test
    public void executeChecked_whenErrorThrown() throws Exception {
        AtomicBlock block = stm.newTransactionFactoryBuilder().newAtomicBlock();
        final GammaLongRef ref = new GammaLongRef(stm, 10);

        final Error ex = new Error();

        try {
            block.executeChecked(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    ref.openForWrite(btx, LOCKMODE_NONE).long_value++;
                    throw ex;
                }
            });
            fail();
        } catch (Error expected) {
            assertSame(ex, expected);
        }

        assertEquals(10, ref.atomicGet());
    }

    @Test
    public void execute_whenCheckedExceptionThrown() {
        AtomicBlock block = stm.newTransactionFactoryBuilder().newAtomicBlock();
        final GammaLongRef ref = new GammaLongRef(stm, 10);

        final Exception ex = new Exception();

        try {
            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    ref.openForWrite(btx, LOCKMODE_NONE).long_value++;
                    throw ex;
                }
            });
            fail();
        } catch (InvisibleCheckedException expected) {
            assertSame(ex, expected.getCause());
        }

        assertEquals(10, ref.atomicGet());
    }

    @Test
    public void execute_whenRuntimeExceptionThrown() {
        AtomicBlock block = stm.newTransactionFactoryBuilder().newAtomicBlock();
        final GammaLongRef ref = new GammaLongRef(stm, 10);

        final RuntimeException ex = new RuntimeException();

        try {
            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    ref.openForWrite(btx, LOCKMODE_NONE).long_value++;
                    throw ex;
                }
            });
            fail();
        } catch (RuntimeException expected) {
            assertSame(ex, expected);
        }

        assertEquals(10, ref.atomicGet());
    }


    @Test
    public void execute_whenErrorThrown() {
        AtomicBlock block = stm.newTransactionFactoryBuilder().newAtomicBlock();
        final GammaLongRef ref = new GammaLongRef(stm, 10);

        final Error ex = new Error();

        try {
            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    GammaTransaction btx = (GammaTransaction) tx;
                    ref.openForWrite(btx, LOCKMODE_NONE).long_value++;
                    throw ex;
                }
            });
            fail();
        } catch (Error expected) {
            assertSame(ex, expected);
        }

        assertEquals(10, ref.atomicGet());
    }


}
