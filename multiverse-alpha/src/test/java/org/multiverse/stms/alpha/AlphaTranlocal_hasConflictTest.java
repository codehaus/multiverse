package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class AlphaTranlocal_hasConflictTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void whenFreshAndNoConflictingTransaction_thenNoConflict() {
        ManualRef ref = ManualRef.createUncommitted();

        ManualRefTranlocal tranlocal = new ManualRefTranlocal(ref);

        Transaction tx = mock(Transaction.class);
        boolean hasConflict = tranlocal.hasReadConflict(tx);
        assertFalse(hasConflict);
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
    public void whenReadonlyAndNoConflictTransaction_thenNoConflict() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal readonly = (ManualRefTranlocal) ref.___load();

        Transaction tx = mock(Transaction.class);
        boolean hasConflict = readonly.hasReadConflict(tx);
        assertFalse(hasConflict);
    }

    @Test
    public void whenReadonlyAndConflictingTransaction_thenConflict() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal readonly = (ManualRefTranlocal) ref.___load();

        //conflicting transaction
        ref.inc(stm);

        Transaction tx = mock(Transaction.class);
        boolean hasConflict = readonly.hasReadConflict(tx);
        assertTrue(hasConflict);
    }
}
