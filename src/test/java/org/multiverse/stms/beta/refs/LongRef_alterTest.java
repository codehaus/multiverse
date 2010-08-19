package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRef_alterTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        LongFunction function = new LongFunction() {
            @Override
            public long call(long current) {
                return current + 1;
            }
        };

        LongRef ref = createLongRef(stm, 100);
        BetaTransaction tx = stm.startDefaultTransaction();
        long result = ref.alter(tx, pool, function);
        tx.commit();

        assertEquals(101, ref.___unsafeLoad().value);
        assertEquals(101, result);
    }
}
