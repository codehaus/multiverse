package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.functions.Functions;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRefTranlocal;

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
        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setStatus(STATUS_COMMUTING);
        tranlocal.addCommutingFunction(Functions.newDecLongFunction(), pool);

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenReadonly() {
        BetaLongRef ref = newLongRef(stm);
        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.value = 100;
        tranlocal.oldValue = 100;
        tranlocal.version = 10;
        tranlocal.setStatus(STATUS_READONLY);

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenUpdateWithoutDirtyFlag() {
        BetaLongRef ref = newLongRef(stm);
        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.value = 100;
        tranlocal.oldValue = 100;
        tranlocal.version = 10;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenUpdateWithDirtyFlag() {
        BetaLongRef ref = newLongRef(stm);
        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        //todo: setUpdate
        tranlocal.setDirty(true);
        tranlocal.value = 100;
        tranlocal.oldValue = 100;
        tranlocal.version = 10;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenHasCommitLock() {
        BetaLongRef ref = newLongRef(stm);
        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setLockMode(LOCKMODE_EXCLUSIVE);
        tranlocal.value = 100;
        tranlocal.oldValue = 100;
        tranlocal.version = 10;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenHasUpdateLock() {
        BetaLongRef ref = newLongRef(stm);
        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setLockMode(LOCKMODE_WRITE);
        tranlocal.value = 100;
        tranlocal.oldValue = 100;
        tranlocal.version = 10;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }


    @Test
    public void whenConstructed() {
        BetaLongRef ref = newLongRef(stm);
        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setLockMode(LOCKMODE_EXCLUSIVE);
        tranlocal.setStatus(STATUS_CONSTRUCTING);
        tranlocal.value = 100;
        tranlocal.oldValue = 0;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    @Test
    public void whenHasDepartObligation() {
        BetaLongRef ref = newLongRef(stm);
        BetaLongRefTranlocal tranlocal = ref.___newTranlocal();
        tranlocal.setDepartObligation(true);
        tranlocal.value = 100;
        tranlocal.oldValue = 0;

        tranlocal.prepareForPooling(pool);

        assertCleared(tranlocal);
    }

    private void assertCleared(BetaLongRefTranlocal tranlocal) {
        assertEquals(0, tranlocal.version);
        assertEquals(0, tranlocal.value);
        assertEquals(0, tranlocal.oldValue);
        assertNull(tranlocal.owner);
        assertFalse(tranlocal.hasDepartObligation());
        assertFalse(tranlocal.isReadonly());
        assertTranlocalHasNoLock(tranlocal);
        assertFalse(tranlocal.isCommuting());
        assertFalse(tranlocal.isConstructing());
        assertFalse(tranlocal.isDirty());
        assertNull(tranlocal.headCallable);
    }
}
