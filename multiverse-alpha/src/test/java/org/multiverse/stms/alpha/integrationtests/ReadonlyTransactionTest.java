package org.multiverse.stms.alpha.integrationtests;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.datastructures.refs.Ref;
import org.multiverse.stms.alpha.AlphaAtomicObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.AlphaTransaction;
import org.multiverse.stms.alpha.instrumentation.asm.MetadataRepository;

public class ReadonlyTransactionTest {

    private static AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        clearThreadLocalTransaction();
    }

    @Test
    public void refIsTransformed() {
        Ref ref = new Ref();
        MetadataRepository s = MetadataRepository.INSTANCE;
        assertTrue(((Object) ref) instanceof AlphaAtomicObject);
    }

    @Test
    public void test() {
        Ref<Integer> ref = new Ref<Integer>(10);

        long version = stm.getTime();

        readonlyMethod(ref, 10);

        assertEquals(version, stm.getTime());
    }

    @AtomicMethod(readonly = true)
    public static void readonlyMethod(Ref<Integer> ref, int expectedValue) {
        AlphaTranlocal tranlocal = getTranlocal(ref);
        assertSame(ref, tranlocal.getAtomicObject());
        assertSame(stm.getTime(), tranlocal.___writeVersion);
        assertEquals(expectedValue, (int) ref.get());
    }

    @Test
    public void modificationInReadonlyTransactionIsDetected() {
        Ref<Integer> ref = new Ref<Integer>(0);
        assertTrue(((Object) ref) instanceof AlphaAtomicObject);

        long version = stm.getTime();

        try {
            readonlyMethodThatUpdates(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertEquals(version, stm.getTime());
        assertEquals(0, (int) ref.get());
    }

    @AtomicMethod(readonly = true)
    public static void readonlyMethodThatUpdates(Ref<Integer> ref) {
        AlphaTranlocal tranlocal = getTranlocal(ref);
        assertSame(ref, tranlocal.getAtomicObject());
        assertSame(stm.getTime(), tranlocal.___writeVersion);
        ref.set(1);
    }

    public static AlphaTranlocal getTranlocal(Object atomicObject) {
        AlphaTransaction t = (AlphaTransaction) getThreadLocalTransaction();
        return t.load((AlphaAtomicObject) atomicObject);
    }
}
