package org.multiverse.stms.beta;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author Peter Veentjer
 */
public class Tranlocal_calculateIsDirtyTest implements BetaStmConstants{

    @Test
    public void whenConstructed() {
        BetaTransaction tx = mock(BetaTransaction.class);
        BetaLongRef ref = new BetaLongRef(tx);
        Tranlocal tranlocal = new LongRefTranlocal(ref);

        assertTrue(tranlocal.calculateIsDirty());
        assertEquals(DIRTY_TRUE, tranlocal.isDirty);
    }

    @Test
    @Ignore
    public void whenCommuting(){

    }

    @Test
    public void whenCommitted() {
        BetaLongRef ref = new BetaLongRef(0);
        Tranlocal tranlocal = new LongRefTranlocal(ref);
        tranlocal.prepareForCommit();

        assertFalse(tranlocal.calculateIsDirty());
        assertEquals(DIRTY_FALSE, tranlocal.isDirty);
    }

    @Test
    public void whenNotDirty() {
        BetaLongRef ref = new BetaLongRef(0);
        Tranlocal tranlocal = new LongRefTranlocal(ref);
        tranlocal.prepareForCommit();
        Tranlocal opened = tranlocal.openForWrite(new BetaObjectPool());

        assertFalse(opened.calculateIsDirty());
        assertEquals(DIRTY_FALSE, tranlocal.isDirty);
    }

    @Test
    public void whenDirty() {
        BetaLongRef ref = new BetaLongRef(0);
        LongRefTranlocal tranlocal = new LongRefTranlocal(ref);
        tranlocal.prepareForCommit();

        LongRefTranlocal opened = tranlocal.openForWrite(new BetaObjectPool());
        opened.value++;

        assertTrue(opened.calculateIsDirty());
        assertEquals(DIRTY_TRUE, opened.isDirty);
    }
}
