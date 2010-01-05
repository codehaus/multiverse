package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.annotations.TransactionalObject;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class TransactionalMethod_ReadonlyTransactionTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void test() {
        intRef ref = new intRef(10);

        long version = stm.getVersion();

        int value = ref.get();
        assertEquals(10, value);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void updateIsDetected() {
        intRef ref = new intRef(10);

        long version = stm.getVersion();

        try {
            ref.readonlySet(11);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.get());
    }

    @TransactionalObject
    static class intRef {

        private int value;

        intRef(int value) {
            this.value = value;
        }

        @TransactionalMethod(readonly = true)
        public int get() {
            return value;
        }

        @TransactionalMethod(readonly = true)
        public void readonlySet(int value) {
            this.value = value;
        }

        public void set(int value) {
            this.value = value;
        }
    }
}
