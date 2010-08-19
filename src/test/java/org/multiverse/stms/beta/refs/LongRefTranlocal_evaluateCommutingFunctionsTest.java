package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.functions.IncLongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRefTranlocal_evaluateCommutingFunctionsTest {

    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenSingleCommutingFunction() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongRefTranlocal tranlocal = ref.___openForCommute(pool);
        tranlocal.addCommutingFunction(IncLongFunction.INSTANCE, pool);
        tranlocal.read = committed;
        tranlocal.evaluateCommutingFunctions(pool);

        assertFalse(tranlocal.isCommitted);
        assertFalse(tranlocal.isCommuting);
        assertTrue(tranlocal.isDirty);
        assertSame(ref, tranlocal.owner);
        assertSame(committed, tranlocal.read);
        assertEquals(101, tranlocal.value);
    }

    @Test
    public void whenMultipleCommutingFunctions() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongRefTranlocal tranlocal = ref.___openForCommute(pool);
        tranlocal.addCommutingFunction(IncLongFunction.INSTANCE, pool);
        tranlocal.addCommutingFunction(IncLongFunction.INSTANCE, pool);
        tranlocal.addCommutingFunction(IncLongFunction.INSTANCE, pool);
        tranlocal.read = committed;
        tranlocal.evaluateCommutingFunctions(pool);

        assertFalse(tranlocal.isCommitted);
        assertFalse(tranlocal.isCommuting);
        assertTrue(tranlocal.isDirty);
        assertSame(ref, tranlocal.owner);
        assertSame(committed, tranlocal.read);
        assertEquals(103, tranlocal.value);
    }
}
