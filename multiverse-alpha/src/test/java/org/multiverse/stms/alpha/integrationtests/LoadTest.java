package org.multiverse.stms.alpha.integrationtests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.datastructures.refs.Ref;
import org.multiverse.stms.alpha.AlphaAtomicObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransaction;

public class LoadTest {

    private AlphaStm stm;
    private static Ref<Integer> intRef;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        intRef = new Ref<Integer>(0);
        setThreadLocalTransaction(null);
    }

    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    @Test
    public void testUpdateLoad() {
        executeUpdate();
    }

    public static AlphaTranlocal getTranlocal() {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        return t.load((AlphaAtomicObject) ((Object) intRef));
    }

    @AtomicMethod
    public static void executeUpdate() {
        AlphaTranlocal tranlocal = getTranlocal();
        assertEquals(0, tranlocal.___writeVersion);
        assertEquals(0, (int) intRef.get());
    }

    @Test
    public void testReadonlyLoad() {
        executeReadonly();
    }

    @AtomicMethod(readonly = true)
    public static void executeReadonly() {
        assertTrue(getTranlocal().___writeVersion > 0);
        assertEquals(0, (int) intRef.get());
    }
}
