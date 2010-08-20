package org.multiverse.stms.beta.integrationtest.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicClosure;
import org.multiverse.api.closures.AtomicIntClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.IntRef;
import org.multiverse.stms.beta.transactionalobjects.Ref;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class ReadonlyTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    public void whenReadonly_thenUpdateFails() {
        IntRef ref = createIntRef(stm);
        try {
            updateInReadonlyMethod(ref, 10);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertEquals(0, ref.___unsafeLoad().value);
    }

    public void updateInReadonlyMethod(final IntRef ref, final int newValue) {
         AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setReadonly(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                ref.set(btx, pool, newValue);
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
        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setReadonly(true)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                Ref ref = new Ref(tx);
                btx.openForConstruction(ref, pool).value = value;
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
        AtomicBlock block = stm.getTransactionFactoryBuilder()
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
        IntRef ref = createIntRef(stm, 10);
        int result = readInReadonlyMethod(ref);
        assertEquals(10, result);
        assertEquals(10, ref.___unsafeLoad().value);
    }

    public int readInReadonlyMethod(final IntRef ref) {
        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setReadonly(true)
                .buildAtomicBlock();

        return block.execute(new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                return ref.get(btx, pool);
            }
        });
    }

    @Test
    public void whenUpdate_thenCreationOfNewTransactionalObjectsSucceeds() {
        IntRef ref = update_createNewTransactionObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.___unsafeLoad().value);
    }

    public IntRef update_createNewTransactionObject(final int value) {
        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .buildAtomicBlock();

        return block.execute(new AtomicClosure<IntRef>() {
            @Override
            public IntRef execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                IntRef ref = new IntRef(btx);
                btx.openForConstruction(ref, pool).value = value;
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
        AtomicBlock block = stm.getTransactionFactoryBuilder()
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
        IntRef ref = createIntRef(stm, 10);
        int result = readInUpdateMethod(ref);
        assertEquals(10, result);
        assertEquals(10, ref.___unsafeLoad().value);
    }


    public int readInUpdateMethod(final IntRef ref) {
        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .buildAtomicBlock();

        return block.execute(new AtomicIntClosure() {
            @Override
            public int execute(Transaction tx) throws Exception {
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                BetaTransaction btx = (BetaTransaction) tx;
                return ref.get(btx, pool);
            }
        });
    }

    @Test
    public void whenUpdate_thenUpdateSucceeds() {
        IntRef ref = createIntRef(stm);
        updateInUpdateMethod(ref, 10);
        assertEquals(10, ref.___unsafeLoad().value);
    }

    public void updateInUpdateMethod(final IntRef ref, final int newValue) {
        AtomicBlock block = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .buildAtomicBlock();

        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                assertFalse(tx.getConfiguration().isReadonly());
                BetaTransaction btx = (BetaTransaction) tx;
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                ref.set(btx, pool, newValue);
            }
        });
    }

    @Test
    public void whenDefault_thenUpdateSuccess() {
        IntRef ref = createIntRef(stm);
        defaultTransactionalMethod(ref);

        assertEquals(1, ref.___unsafeLoad().value);
    }


    public void defaultTransactionalMethod(final IntRef ref) {
        stm.getTransactionFactoryBuilder().buildAtomicBlock().execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                assertFalse(tx.getConfiguration().isReadonly());
                BetaTransaction btx = (BetaTransaction) tx;
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                ref.set(btx, pool, ref.get(btx, pool) + 1);
            }
        });
    }
}
