package org.multiverse.stms.alpha.instrumentation.asm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.utils.profiling.ProfileRepository;

/**
 * @author Peter Veentjer
 */
public class AtomicMethod_ReadonlyTransactionTest {

    private AlphaStm stm;
    static private ProfileRepository profiler;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        profiler = stm.getProfiler();
        setGlobalStmInstance(stm);
    }

    @Test
    public void test() {
        IntRef ref = new IntRef(10);

        long version = stm.getTime();
        long readonlyTransactionStarted = profiler.sumKey1("readonlytransaction.started.count");
        long readonlyTransactionAborted = profiler.sumKey1("readonlytransaction.aborted.count");
        long readonlyTransactionCommitted = profiler.sumKey1("readonlytransaction.committed.count");

        int value = ref.get();
        assertEquals(10, value);
        assertEquals(version, stm.getTime());
        assertEquals(readonlyTransactionStarted + 1, profiler.sumKey1("readonlytransaction.started.count"));
        assertEquals(readonlyTransactionAborted, profiler.sumKey1("readonlytransaction.aborted.count"));
        assertEquals(readonlyTransactionCommitted + 1, profiler.sumKey1("readonlytransaction.committed.count"));

    }

    @Test
    public void updateIsDetected() {
        IntRef ref = new IntRef(10);

        long version = stm.getTime();
        long readonlyTransactionStarted = profiler.sumKey1("readonlytransaction.started.count");
        long readonlyTransactionAborted = profiler.sumKey1("readonlytransaction.aborted.count");
        long readonlyTransactionCommitted = profiler.sumKey1("readonlytransaction.committed.count");

        try {
            ref.readonlySet(11);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertEquals(version, stm.getTime());
        assertEquals(readonlyTransactionStarted + 1, profiler.sumKey1("readonlytransaction.started.count"));
        assertEquals(readonlyTransactionAborted + 1, profiler.sumKey1("readonlytransaction.aborted.count"));
        assertEquals(readonlyTransactionCommitted, profiler.sumKey1("readonlytransaction.committed.count"));
        assertEquals(10, ref.get());
    }

    @AtomicObject
    static class IntRef {

        private int value;

        IntRef(int value) {
            this.value = value;
        }

        @AtomicMethod(readonly = true)
        public int get() {
            return value;
        }

        @AtomicMethod(readonly = true)
        public void readonlySet(int value) {
            System.out.println(profiler.sumKey1("readonlytransaction.started.count"));
            this.value = value;
        }

        public void set(int value) {
            this.value = value;
        }
    }
}
