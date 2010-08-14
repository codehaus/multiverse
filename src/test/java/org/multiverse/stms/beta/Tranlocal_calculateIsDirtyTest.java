package org.multiverse.stms.beta;

import org.junit.Test;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.refs.Tranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Peter Veentjer
 */
public class Tranlocal_calculateIsDirtyTest {

    @Test
    public void whenConstructed() {
        BetaTransaction tx = mock(BetaTransaction.class);
        LongRef ref = new LongRef(tx);
        Tranlocal tranlocal = new LongRefTranlocal(ref);

        assertTrue(tranlocal.calculateIsDirty());
        assertTrue(tranlocal.isDirty);
    }

    @Test
    public void whenCommitted() {
        LongRef ref = new LongRef(0);
        Tranlocal tranlocal = new LongRefTranlocal(ref);
        tranlocal.prepareForCommit();

        assertFalse(tranlocal.calculateIsDirty());
        assertFalse(tranlocal.isDirty);
    }

    @Test
    public void whenNotDirty() {
        LongRef ref = new LongRef(0);
        Tranlocal tranlocal = new LongRefTranlocal(ref);
        tranlocal.prepareForCommit();
        Tranlocal opened = tranlocal.openForWrite(new BetaObjectPool());

        assertFalse(opened.calculateIsDirty());
        assertFalse(opened.isDirty);
    }

    @Test
    public void whenDirty() {
        LongRef ref = new LongRef(0);
        LongRefTranlocal tranlocal = new LongRefTranlocal(ref);
        tranlocal.prepareForCommit();
        LongRefTranlocal opened = tranlocal.openForWrite(new BetaObjectPool());
        opened.value++;

        assertTrue(opened.calculateIsDirty());
        assertTrue(opened.isDirty);
    }
}
