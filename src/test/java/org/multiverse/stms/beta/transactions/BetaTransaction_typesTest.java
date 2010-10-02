package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.exampletransactionalobjects.LinkedList;
import org.multiverse.stms.beta.transactionalobjects.*;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertEqualsDouble;
import static org.multiverse.stms.beta.BetaStmUtils.*;

public abstract class BetaTransaction_typesTest {

    protected BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    public abstract BetaTransaction newTransaction();

    @Test
    public void whenIntRefUsed() {
        BetaIntRef ref = newIntRef(stm, 100);

        BetaTransaction tx = newTransaction();
        IntRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEquals(100, read.value);
        IntRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.commit();

        assertEquals(101, ref.___unsafeLoad().value);
    }

    @Test
    public void whenLongRefUsed() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEquals(100, read.value);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.commit();

        assertEquals(101, ref.___unsafeLoad().value);
    }

    @Test
    public void whenRefUsed() {
        BetaRef<String> ref = newRef(stm, "peter");

        BetaTransaction tx = newTransaction();
        RefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEquals("peter", read.value);
        RefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value = "john";
        tx.commit();

        assertEquals("john", ref.___unsafeLoad().value);
    }

    @Test
    public void whenBooleanRefUsed() {
        BetaBooleanRef ref = new BetaBooleanRef(stm, false);

        BetaTransaction tx = newTransaction();
        BooleanRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertFalse(read.value);
        BooleanRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value = true;
        tx.commit();

        assertTrue(ref.___unsafeLoad().value);
    }

    @Test
    public void whenDoubleUsed() {
        BetaDoubleRef ref = newDoubleRef(stm, 10);

        BetaTransaction tx = newTransaction();
        DoubleRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEqualsDouble(10, read.value);
        DoubleRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value = 20;
        tx.commit();

        assertEqualsDouble(20, ref.___unsafeLoad().value);
    }

    @Test
    @Ignore
    public void whenCustomTransactionalObjectUsed() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LinkedList list = new LinkedList(tx);
        tx.commit();



    }
}
