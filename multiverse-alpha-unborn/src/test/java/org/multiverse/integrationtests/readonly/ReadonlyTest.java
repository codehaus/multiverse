package org.multiverse.integrationtests.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.integrationtests.Ref;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * Test
 *
 * @author Peter Veentjer
 */
public class ReadonlyTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenReadonly_thenUpdateFails() {
        Ref ref = new Ref(0);
        try {
            updateInReadonlyMethod(ref, 10);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertEquals(0, ref.get());
    }

    @TransactionalMethod(readonly = true)
    public void updateInReadonlyMethod(Ref ref, int newValue) {
        ref.set(newValue);
    }

    @Test
    public void whenReadonly_thenCreationOfNewTransactionalObjectNotFails() {
        try {
            readonly_createNewTransactionObject(10);
            fail();
        } catch (ReadonlyException expected) {
        }
    }

    @TransactionalMethod(readonly = true)
    public Ref readonly_createNewTransactionObject(int value) {
        return new Ref(value);
    }

    @Test
    public void whenReadonly_thenCreationOfNonTransactionalObjectSucceeds() {
        Integer ref = readonly_createNormalObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.intValue());
    }

    @TransactionalMethod(readonly = true)
    public Integer readonly_createNormalObject(int value) {
        return new Integer(value);
    }

    @Test
    public void whenReadonly_thenReadAllowed() {
        Ref ref = new Ref(10);
        int result = readInReadonlyMethod(ref);
        assertEquals(10, result);
        assertEquals(10, ref.get());
    }

    @TransactionalMethod(readonly = true)
    public int readInReadonlyMethod(Ref ref) {
        return ref.get();
    }

    @Test
    public void whenUpdate_thenCreationOfNewTransactionalObjectsSucceeds() {
        Ref ref = update_createNewTransactionObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.get());
    }

    @TransactionalMethod(readonly = false)
    public Ref update_createNewTransactionObject(int value) {
        return new Ref(value);
    }

    @Test
    public void whenUpdate_thenCreationOfNonTransactionalObjectSucceeds() {
        Integer ref = update_createNormalObject(100);
        assertNotNull(ref);
        assertEquals(100, ref.intValue());
    }

    @TransactionalMethod(readonly = false)
    public Integer update_createNormalObject(int value) {
        return new Integer(value);
    }

    @Test
    public void whenUpdate_thenReadSucceeds() {
        Ref ref = new Ref(10);
        int result = readInUpdateMethod(ref);
        assertEquals(10, result);
        assertEquals(10, ref.get());
    }

    @TransactionalMethod(readonly = false)
    public int readInUpdateMethod(Ref ref) {
        return ref.get();
    }

    @Test
    public void whenUpdate_thenUpdateSucceeds() {
        Ref ref = new Ref(0);
        updateInUpdateMethod(ref, 10);
        assertEquals(10, ref.get());
    }

    @TransactionalMethod(readonly = false)
    public void updateInUpdateMethod(Ref ref, int newValue) {
        ref.set(newValue);
    }

}
