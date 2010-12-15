package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.exampletransactionalobjects.LinkedList;
import org.multiverse.stms.beta.transactionalobjects.*;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertEqualsDouble;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;

public abstract class BetaTransaction_typesTest {

    protected BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    public abstract BetaTransaction newTransaction();

    @Test
    public void whenIntRefUsed() {
        int initialValue = 100;
        BetaIntRef ref = newIntRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaIntRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEquals(initialValue, read.value);
        BetaIntRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }


    @Test
    public void whenLongRefUsed() {
        long initialValue = 100;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEquals(initialValue, read.value);
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void whenRefUsed() {
        String initialValue = "peter";
        BetaRef<String> ref = newRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEquals(initialValue, read.value);
        BetaRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        String newValue = "john";
        write.value = newValue;
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, newValue);

    }

    @Test
    public void whenBooleanRefUsed() {
        boolean initialValue = false;
        BetaBooleanRef ref = new BetaBooleanRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaBooleanRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEquals(initialValue, read.value);
        BetaBooleanRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value = true;
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, !initialValue);
    }

    @Test
    public void whenDoubleUsed() {
        int initialValue = 10;
        BetaDoubleRef ref = newDoubleRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaDoubleRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        assertEqualsDouble(initialValue, read.value);
        BetaDoubleRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        double newValue = 20;
        write.value = newValue;
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, newValue);
    }

    @Test
    @Ignore
    public void whenCustomTransactionalObjectUsed() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LinkedList list = new LinkedList(tx);
        tx.commit();


    }
}
