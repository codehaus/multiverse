package org.multiverse.stms.beta.integrationtest.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicIntClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newIntRef;

public class ReadonlyTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (BetaStm) getGlobalStmInstance();
    }

    @Test
    public void whenReadonly_thenUpdateFails() {
        BetaIntRef ref = newIntRef(stm);
        try {
            updateInReadonlyMethod(ref, 10);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertEquals(0, ref.atomicGet());
    }

    public void updateInReadonlyMethod(final BetaIntRef ref, final int newValue) {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setReadonly(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                ref.getAndSet(btx, newValue);
            }
        });
    }

    @Test
    public void whenReadonly_thenCreationOfNewTransactionalObjectNotFails() {
        try {
            readonly_createNewTransactionObject(10);
            fail();
        } catch (ReadonlyException expected) {
        }
    }

    public void readonly_createNewTransactionObject(final int value) {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setReadonly(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                BetaRef ref = new BetaRef(btx);
                btx.openForConstruction(ref).value = value;
            }
        });
    }

    @Test
    public void whenReadonly_thenCreationOfNonTransactionalObjectSucceeds() {
        Integer ref = readonly_createNormalObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.intValue());
    }

    public Integer readonly_createNormalObject(final int value) {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setReadonly(true)
                .buildAtomicBlock();

        return block.execute(new AtomicClosure<Integer>() {
            @Override
            public Integer execute(Transaction tx) throws Exception {
                return new Integer(value);
            }
        });
    }

    @Test
    public void whenReadonly_thenReadAllowed() {
        BetaIntRef ref = newIntRef(stm, 10);
        int result = readInReadonlyMethod(ref);
        assertEquals(10, result);
        assertEquals(10, ref.atomicGet());
    }

    public int readInReadonlyMethod(final BetaIntRef ref) {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setReadonly(true)
                .buildAtomicBlock();

        return block.execute(new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                return ref.get(btx);
            }
        });
    }

    @Test
    public void whenUpdate_thenCreationOfNewTransactionalObjectsSucceeds() {
        BetaIntRef ref = update_createNewTransactionObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.atomicGet());
    }

    public BetaIntRef update_createNewTransactionObject(final int value) {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setReadonly(false)
                .buildAtomicBlock();

        return block.execute(new AtomicClosure<BetaIntRef>() {
            @Override
            public BetaIntRef execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                BetaIntRef ref = new BetaIntRef(btx);
                btx.openForConstruction(ref).value = value;
                return ref;
            }
        });
    }

    @Test
    public void whenUpdate_thenCreationOfNonTransactionalObjectSucceeds() {
        Integer ref = update_createNormalObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.intValue());
    }

    public Integer update_createNormalObject(final int value) {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setReadonly(false)
                .buildAtomicBlock();

        return block.execute(new AtomicClosure<Integer>() {
            @Override
            public Integer execute(Transaction tx) throws Exception {
                return new Integer(value);
            }
        });
    }

    @Test
    public void whenUpdate_thenReadSucceeds() {
        BetaIntRef ref = newIntRef(stm, 10);
        int result = readInUpdateMethod(ref);
        assertEquals(10, result);
        assertEquals(10, ref.atomicGet());
    }


    public int readInUpdateMethod(final BetaIntRef ref) {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setReadonly(false)
                .buildAtomicBlock();

        return block.execute(new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                return ref.get(btx);
            }
        });
    }

    @Test
    public void whenUpdate_thenUpdateSucceeds() {
        BetaIntRef ref = newIntRef(stm);
        updateInUpdateMethod(ref, 10);
        assertEquals(10, ref.atomicGet());
    }

    public void updateInUpdateMethod(final BetaIntRef ref, final int newValue) {
        AtomicBlock block = stm.createTransactionFactoryBuilder()
                .setReadonly(false)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                assertFalse(tx.getConfiguration().isReadonly());
                BetaTransaction btx = (BetaTransaction) tx;
                ref.getAndSet(btx, newValue);
            }
        });
    }

    @Test
    public void whenDefault_thenUpdateSuccess() {
        BetaIntRef ref = newIntRef(stm);
        defaultTransactionalMethod(ref);

        assertEquals(1, ref.atomicGet());
    }


    public void defaultTransactionalMethod(final BetaIntRef ref) {
        stm.createTransactionFactoryBuilder().buildAtomicBlock().execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                assertFalse(tx.getConfiguration().isReadonly());
                BetaTransaction btx = (BetaTransaction) tx;
                ref.getAndSet(btx, ref.get(btx) + 1);
            }
        });
    }
}
