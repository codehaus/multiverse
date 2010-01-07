package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class AlphaTranlocal_hasConflictTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    @Ignore
    public void whenFreshAndNoConflictingTransaction_thenNoConflict() {
        ManualRef ref = ManualRef.createUncommitted();

        ManualRefTranlocal tranlocal = new ManualRefTranlocal(ref);
        //assertFalse(tranlocal.hasReadConflict());
    }

    @Test
    @Ignore
    public void whenFreshAndConflictingTransaction_thenConflict() {
        ManualRef ref = ManualRef.createUncommitted();
        ManualRefTranlocal first = new ManualRefTranlocal(ref);
        ref.___store(first, stm.getVersion() + 1);

        ManualRefTranlocal tranlocal = new ManualRefTranlocal(ref);
        //assertTrue(tranlocal.hasReadConflict());
    }

    @Test
    public void whenUpdatedAndNoConflictingTransaction_thenNoConflict() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        ManualRefTranlocal tranlocal = new ManualRefTranlocal(committed);
        assertFalse(tranlocal.hasWriteConflict());
    }

    @Test
    public void whenUpdatedAndConflictingTransaction_thenConflict() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        //conflicting transaction
        ref.inc(stm);

        ManualRefTranlocal tranlocal = new ManualRefTranlocal(committed);
        assertTrue(tranlocal.hasWriteConflict());
    }

    @Test
    @Ignore
    public void whenReadonlyAndNoConflictTransaction_thenNoConflict() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal readonly = (ManualRefTranlocal) ref.___load();

        //assertFalse(readonly.hasReadConflict());
    }

    @Test
    @Ignore
    public void whenReadonlyAndConflictingTransaction_thenConflict() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal readonly = (ManualRefTranlocal) ref.___load();

        //conflicting transaction
        ref.inc(stm);

        //assertTrue(readonly.hasReadConflict());

    }
}
