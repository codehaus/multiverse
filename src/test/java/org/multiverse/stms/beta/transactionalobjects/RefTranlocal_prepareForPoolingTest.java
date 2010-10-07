package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;

import static org.junit.Assert.assertNull;
import static org.multiverse.stms.beta.BetaStmTestUtils.newRef;

public class RefTranlocal_prepareForPoolingTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    /**
     * This test is to make sure that there is no hidden memory leak to the value, when tranlocals are pooled.
     */
    @Test
    public void whenReferenceHasValue_thenItIsNulled() {
        BetaRef<String> ref = newRef(stm, "peter");
        RefTranlocal<String> tranlocal = ref.___newTranlocal();
        ref.___load(1, null, LOCKMODE_NONE, tranlocal);

        tranlocal.prepareForPooling(pool);

        assertNull(tranlocal.value);
    }
}
