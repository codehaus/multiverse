package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.exampletransactionalobjects.LinkedList;
import org.multiverse.stms.beta.transactionalobjects.*;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertEqualsDouble;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
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
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        IntRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEquals(100, read.value);
        IntRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.commit();

        assertVersion(version+1, ref.getVersion());
        assertEquals(101, ref.___weakRead());
    }


    void assertVersion(long l, long version) {
    }

    @Test
    public void whenLongRefUsed() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEquals(100, read.value);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.commit();

        assertVersion(version+1, ref.getVersion());
        assertEquals(101, ref.___weakRead());
    }

    @Test
    public void whenRefUsed() {
        BetaRef<String> ref = newRef(stm, "peter");
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        RefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEquals("peter", read.value);
        RefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value = "john";
        tx.commit();

        assertVersion(version+1, ref.getVersion());
        assertEquals("john", ref.___weakRead());

    }

    @Test
    public void whenBooleanRefUsed() {
        BetaBooleanRef ref = new BetaBooleanRef(stm, false);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BooleanRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertFalse(read.value);
        BooleanRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value = true;
        tx.commit();

        assertVersion(version+1, ref.getVersion());
        assertTrue(ref.___weakRead());
    }

    @Test
    public void whenDoubleUsed() {
        BetaDoubleRef ref = newDoubleRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        DoubleRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEqualsDouble(10, read.value);
        DoubleRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value = 20;
        tx.commit();

        assertVersion(version+1, ref.getVersion());
        assertEqualsDouble(20, ref.___weakRead());
    }

    @Test
    @Ignore
    public void whenCustomTransactionalObjectUsed() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LinkedList list = new LinkedList(tx);
        tx.commit();


    }
}
