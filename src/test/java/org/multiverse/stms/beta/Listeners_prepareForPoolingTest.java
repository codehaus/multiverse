package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.functions.Functions;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertTranlocalHasNoLock;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class Listeners_prepareForPoolingTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenCommuting() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.isCommuting = true;
        tranlocal.addCommutingFunction(Functions.newDecLongFunction(), pool);

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenCommitted() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.value = 100;
        tranlocal.oldValue = 100;
        tranlocal.version = 10;
        tranlocal.isCommitted = true;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenUpdateWithoutDirtyFlag() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.value = 100;
        tranlocal.oldValue = 100;
        tranlocal.version = 10;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenUpdateWithDirtyFlag() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.isDirty = true;
        tranlocal.value = 100;
        tranlocal.oldValue = 100;
        tranlocal.version = 10;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenHasCommitLock() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.lockMode = LOCKMODE_COMMIT;
        tranlocal.value = 100;
        tranlocal.oldValue = 100;
        tranlocal.version = 10;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenHasUpdateLock() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.lockMode = LOCKMODE_UPDATE;
        tranlocal.value = 100;
        tranlocal.oldValue = 100;
        tranlocal.version = 10;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }


    @Test
    public void whenConstructed() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.lockMode = LOCKMODE_COMMIT;
        tranlocal.isConstructing = true;
        tranlocal.value = 100;
        tranlocal.oldValue = 0;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenHasDepartObligation() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.hasDepartObligation = true;
        tranlocal.value = 100;
        tranlocal.oldValue = 0;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    private void assertCleared(LongRefTranlocal tranlocal) {
        assertEquals(0, tranlocal.version);
        assertEquals(0, tranlocal.value);
        assertEquals(0, tranlocal.oldValue);
        assertNull(tranlocal.owner);
        assertFalse(tranlocal.hasDepartObligation);
        assertFalse(tranlocal.isCommitted);
        assertTranlocalHasNoLock(tranlocal);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isConstructing);
        assertFalse(tranlocal.isDirty);
        assertNull(tranlocal.headCallable);
    }
}
