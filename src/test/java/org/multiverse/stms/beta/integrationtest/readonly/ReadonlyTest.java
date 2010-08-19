package org.multiverse.stms.beta.integrationtest.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionFactory;
import org.multiverse.stms.beta.BetaTransactionTemplate;
import org.multiverse.stms.beta.refs.IntRef;
import org.multiverse.stms.beta.refs.Ref;
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
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(true)
                .build();

        new BetaTransactionTemplate(txFactory) {
            @Override
            public Object execute(BetaTransaction tx) throws Exception {
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                ref.set(tx, pool, newValue);
                return null;
            }
        }.execute();
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
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(true)
                .build();

        new BetaTransactionTemplate(txFactory) {
            @Override
            public IntRef execute(BetaTransaction tx) throws Exception {
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                Ref ref = new Ref(tx);
                tx.openForConstruction(ref, pool).value = value;
                return null;
            }
        }.execute();
    }

    @Test
    public void whenReadonly_thenCreationOfNonTransactionalObjectSucceeds() {
        Integer ref = readonly_createNormalObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.intValue());
    }

    public Integer readonly_createNormalObject(final int value) {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(true)
                .build();

        return new BetaTransactionTemplate<Integer>(txFactory) {
            @Override
            public Integer execute(BetaTransaction tx) throws Exception {
                return new Integer(value);
            }
        }.execute();
    }

    @Test
    public void whenReadonly_thenReadAllowed() {
        IntRef ref = createIntRef(stm, 10);
        int result = readInReadonlyMethod(ref);
        assertEquals(10, result);
        assertEquals(10, ref.___unsafeLoad().value);
    }

    public int readInReadonlyMethod(final IntRef ref) {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(true)
                .build();

        return new BetaTransactionTemplate<Integer>(txFactory) {
            @Override
            public Integer execute(BetaTransaction tx) throws Exception {
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                return ref.get(tx, pool);
            }
        }.execute();
    }

    @Test
    public void whenUpdate_thenCreationOfNewTransactionalObjectsSucceeds() {
        IntRef ref = update_createNewTransactionObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.___unsafeLoad().value);
    }

    public IntRef update_createNewTransactionObject(final int value) {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build();

        return new BetaTransactionTemplate<IntRef>(txFactory) {
            @Override
            public IntRef execute(BetaTransaction tx) throws Exception {
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                IntRef ref = new IntRef(tx);
                tx.openForConstruction(ref, pool).value = value;
                return ref;
            }
        }.execute();
    }

    @Test
    public void whenUpdate_thenCreationOfNonTransactionalObjectSucceeds() {
        Integer ref = update_createNormalObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.intValue());
    }

    public Integer update_createNormalObject(final int value) {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build();

        return new BetaTransactionTemplate<Integer>(txFactory) {
            @Override
            public Integer execute(BetaTransaction tx) throws Exception {
                return new Integer(value);
            }
        }.execute();
    }

    @Test
    public void whenUpdate_thenReadSucceeds() {
        IntRef ref = createIntRef(stm, 10);
        int result = readInUpdateMethod(ref);
        assertEquals(10, result);
        assertEquals(10, ref.___unsafeLoad().value);
    }


    public int readInUpdateMethod(final IntRef ref) {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build();

        return new BetaTransactionTemplate<Integer>(txFactory) {
            @Override
            public Integer execute(BetaTransaction tx) throws Exception {
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                return ref.get(tx, pool);
            }
        }.execute();
    }

    @Test
    public void whenUpdate_thenUpdateSucceeds() {
        IntRef ref = createIntRef(stm);
        updateInUpdateMethod(ref, 10);
        assertEquals(10, ref.___unsafeLoad().value);
    }

    public void updateInUpdateMethod(final IntRef ref, final int newValue) {
        BetaTransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build();


        new BetaTransactionTemplate(stm) {
            @Override
            public Object execute(BetaTransaction tx) throws Exception {
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                ref.set(tx, pool, newValue);
                return null;
            }
        }.execute();

    }

    @Test
    public void whenDefault_thenUpdateSuccess() {
        IntRef ref = createIntRef(stm);
        defaultTransactionalMethod(ref);

        assertEquals(1, ref.___unsafeLoad().value);
    }


    public void defaultTransactionalMethod(final IntRef ref) {
        new BetaTransactionTemplate(stm) {
            @Override
            public Object execute(BetaTransaction tx) throws Exception {
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                ref.set(tx, pool, ref.get(tx, pool) + 1);
                return null;
            }
        }.execute();
    }
}
