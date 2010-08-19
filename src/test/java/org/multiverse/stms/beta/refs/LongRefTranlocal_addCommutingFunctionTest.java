package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.functions.IncLongFunction;
import org.multiverse.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.assertHasCommutingFunctions;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRefTranlocal_addCommutingFunctionTest {

    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenFirstAddition() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal tranlocal = ref.___openForCommute(pool);
        tranlocal.addCommutingFunction(IncLongFunction.INSTANCE, pool);

        assertFalse(tranlocal.isCommitted);
        assertTrue(tranlocal.isCommuting);
        assertEquals(0, tranlocal.value);
        assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, IncLongFunction.INSTANCE);
    }

    @Test
    public void whenMultipleAdditions(){
        LongRef ref = createLongRef(stm);
        LongRefTranlocal tranlocal = ref.___openForCommute(pool);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);
        LongFunction function3 = mock(LongFunction.class);

        tranlocal.addCommutingFunction(function1, pool);
        tranlocal.addCommutingFunction(function2, pool);
        tranlocal.addCommutingFunction(function3, pool);

        assertFalse(tranlocal.isCommitted);
        assertTrue(tranlocal.isCommuting);
        assertEquals(0, tranlocal.value);
        assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function3, function2, function1);
    }
}
