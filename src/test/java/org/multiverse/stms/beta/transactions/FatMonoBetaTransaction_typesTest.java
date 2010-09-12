package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.*;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertEqualsDouble;
import static org.multiverse.stms.beta.BetaStmUtils.*;

public class FatMonoBetaTransaction_typesTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenIntRefUsed() {
        BetaIntRef ref = newIntRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        IntRefTranlocal read = tx.openForRead(ref, false);
        assertEquals(100, read.value);
        IntRefTranlocal write = tx.openForWrite(ref, false);
        write.value++;
        tx.commit();

        assertEquals(101, ref.___unsafeLoad().value);
    }

    @Test
    public void whenLongRefUsed() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);
        assertEquals(100, read.value);
        LongRefTranlocal write = tx.openForWrite(ref, false);
        write.value++;
        tx.commit();

        assertEquals(101, ref.___unsafeLoad().value);
    }

    @Test
    public void whenRefUsed() {
        BetaRef<String> ref = newRef(stm, "peter");

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        RefTranlocal read = tx.openForRead(ref, false);
        assertEquals("peter", read.value);
        RefTranlocal write = tx.openForWrite(ref, false);
        write.value = "john";
        tx.commit();

        assertEquals("john", ref.___unsafeLoad().value);
    }

    @Test
    public void whenBooleanRefUsed() {
        BetaBooleanRef ref = new BetaBooleanRef(stm, false);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BooleanRefTranlocal read = tx.openForRead(ref, false);
        assertFalse(read.value);
        BooleanRefTranlocal write = tx.openForWrite(ref, false);
        write.value = true;
        tx.commit();

        assertTrue(ref.___unsafeLoad().value);
    }

    @Test
    public void whenDoubleUsed() {
        BetaDoubleRef ref = newDoubleRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        DoubleRefTranlocal read = tx.openForRead(ref, false);
        assertEqualsDouble(10, read.value);
        DoubleRefTranlocal write = tx.openForWrite(ref, false);
        write.value = 20;
        tx.commit();

        assertEqualsDouble(20, ref.___unsafeLoad().value);
    }

    @Test
    @Ignore
    public void whenCustomTransactionalObjectUsed() {
    }
}
