package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;

import static org.junit.Assert.*;
import static org.multiverse.api.functions.Functions.newIdentityLongFunction;
import static org.multiverse.api.functions.Functions.newIncLongFunction;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class LongRefTranlocal_evaluateCommutingFunctionsTest implements BetaStmConstants {

    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenCommutingFunctionDoesntChangeValue() {
        int initialValue = 100;

        BetaLongRef ref = newLongRef(stm, initialValue);
        long version = ref.getVersion();

        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setStatus(STATUS_COMMUTING);
        tranlocal.addCommutingFunction(newIdentityLongFunction(), pool);
        tranlocal.version = version;
        tranlocal.value = initialValue;
        tranlocal.oldValue = initialValue;
        tranlocal.evaluateCommutingFunctions(pool);

        assertFalse(tranlocal.isReadonly());
        assertFalse(tranlocal.isCommuting());
        assertFalse(tranlocal.isDirty());
        assertEquals(initialValue, tranlocal.value);
        assertVersionAndValue(ref, version, initialValue);
    }

    @Test
    public void whenSingleCommutingFunction() {
        int initialValue = 100;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long version = ref.getVersion();

        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setStatus(STATUS_COMMUTING);

        tranlocal.addCommutingFunction(newIncLongFunction(), pool);
        tranlocal.version = version;
        tranlocal.value = initialValue;
        tranlocal.oldValue = initialValue;
        tranlocal.evaluateCommutingFunctions(pool);

        assertFalse(tranlocal.isReadonly());
        assertFalse(tranlocal.isCommuting());
        assertTrue(tranlocal.isDirty());
        assertSame(ref, tranlocal.owner);
        assertEquals(initialValue + 1, tranlocal.value);
    }

    @Test
    public void whenMultipleCommutingFunctions() {
        int initialValue = 100;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long version = ref.getVersion();

        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setStatus(STATUS_COMMUTING);
        tranlocal.addCommutingFunction(newIncLongFunction(), pool);
        tranlocal.addCommutingFunction(newIncLongFunction(), pool);
        tranlocal.addCommutingFunction(newIncLongFunction(), pool);
        tranlocal.version = version;
        tranlocal.value = initialValue;
        tranlocal.oldValue = initialValue;
        tranlocal.evaluateCommutingFunctions(pool);

        assertFalse(tranlocal.isReadonly());
        assertFalse(tranlocal.isCommuting());
        assertTrue(tranlocal.isDirty());
        assertEquals(initialValue + 3, tranlocal.value);
    }
}
