package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.functions.Functions;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

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
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongRefTranlocal tranlocal = ref.___openForCommute(pool);
        tranlocal.addCommutingFunction(new IdentityLongFunction(), pool);
        tranlocal.read = committed;
        tranlocal.evaluateCommutingFunctions(pool);

        assertFalse(tranlocal.isCommitted);
        assertFalse(tranlocal.isCommuting);
        assertEquals(DIRTY_FALSE, tranlocal.isDirty);
        assertSame(ref, tranlocal.owner);
        assertSame(committed, tranlocal.read);
        assertEquals(100, tranlocal.value);
    }


    @Test
    public void whenSingleCommutingFunction() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongRefTranlocal tranlocal = ref.___openForCommute(pool);
        tranlocal.addCommutingFunction(Functions.newLongIncFunction(1), pool);
        tranlocal.read = committed;
        tranlocal.evaluateCommutingFunctions(pool);

        assertFalse(tranlocal.isCommitted);
        assertFalse(tranlocal.isCommuting);
        assertEquals(DIRTY_TRUE, tranlocal.isDirty);
        assertSame(ref, tranlocal.owner);
        assertSame(committed, tranlocal.read);
        assertEquals(101, tranlocal.value);
    }

    @Test
    public void whenMultipleCommutingFunctions() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongRefTranlocal tranlocal = ref.___openForCommute(pool);
        tranlocal.addCommutingFunction(Functions.newLongIncFunction(1), pool);
        tranlocal.addCommutingFunction(Functions.newLongIncFunction(1), pool);
        tranlocal.addCommutingFunction(Functions.newLongIncFunction(1), pool);
        tranlocal.read = committed;
        tranlocal.evaluateCommutingFunctions(pool);

        assertFalse(tranlocal.isCommitted);
        assertFalse(tranlocal.isCommuting);
        assertEquals(DIRTY_TRUE, tranlocal.isDirty);
        assertSame(ref, tranlocal.owner);
        assertSame(committed, tranlocal.read);
        assertEquals(103, tranlocal.value);
    }
}
