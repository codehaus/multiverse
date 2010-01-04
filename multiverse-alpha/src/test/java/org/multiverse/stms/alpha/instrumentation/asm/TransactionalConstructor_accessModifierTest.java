package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.transactional.annotations.TransactionalConstructor;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalConstructor_accessModifierTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void noArgTxConstructor() {
        long version = stm.getVersion();

        NoArgTxConstructor o = new NoArgTxConstructor();
        assertEquals(version, stm.getVersion());
        assertEquals(20, o.value);
    }

    static class NoArgTxConstructor {

        int value;

        @TransactionalConstructor
        NoArgTxConstructor() {
            this.value = 20;
        }
    }

    @Test
    public void singleArgTxConstructor() {
        long version = stm.getVersion();

        SingleArgTxConstructor o = new SingleArgTxConstructor(20);

        assertEquals(version, stm.getVersion());
        assertEquals(20, o.arg);
    }

    static class SingleArgTxConstructor {

        int arg;

        @TransactionalConstructor
        SingleArgTxConstructor(int value) {
            this.arg = value;
        }
    }

    @Test
    public void multiArgTxConstructor() {
        long version = stm.getVersion();

        MultiArgTxConstructor o = new MultiArgTxConstructor(6, 7, 8, 9);

        assertEquals(version, stm.getVersion());
        assertEquals(6, o.arg1);
        assertEquals(7, o.arg2);
        assertEquals(8, o.arg3);
        assertEquals(9, o.arg4);
    }

    static class MultiArgTxConstructor {

        int arg1, arg2, arg3, arg4;

        @TransactionalConstructor
        MultiArgTxConstructor(int arg1, int arg2, int arg3, int arg4) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
        }
    }

    @Test
    public void constructorWithCheckedException() {
        long version = stm.getVersion();
        try {
            new ConstructorWithCheckedException(true);
        } catch (Exception e) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void constructorWithCheckedExceptionThatIsNotThrown() throws Exception {
        long version = stm.getVersion();
        ConstructorWithCheckedException o = new ConstructorWithCheckedException(false);

        assertEquals(version, stm.getVersion());
    }

    static class ConstructorWithCheckedException {

        @TransactionalConstructor
        ConstructorWithCheckedException(boolean throwIt) throws Exception {
            if (throwIt) {
                throw new Exception();
            }
        }
    }

    @Test
    public void testPublicConstructor() {
        long version = stm.getVersion();

        PublicConstructor o = new PublicConstructor();

        assertEquals(10, o.value);
        assertEquals(version, stm.getVersion());
    }

    static class PublicConstructor {

        private int value;

        @TransactionalConstructor
        public PublicConstructor() {
            value = 10;
        }
    }

    @Test
    public void testFinalField() {
        long version = stm.getVersion();

        FinalField o = new FinalField(1);

        assertEquals(version, stm.getVersion());
        assertEquals(1, o.value);
    }

    static class FinalField {

        private int value;

        @TransactionalConstructor
        FinalField(int value) {
            this.value = value;
        }
    }

    @Test
    public void testProtectedConstructor() {
        long version = stm.getVersion();

        ProtectedConstructor o = new ProtectedConstructor();

        assertEquals(version, stm.getVersion());
        assertEquals(10, o.value);
    }

    static class ProtectedConstructor {

        private int value;

        @TransactionalConstructor
        protected ProtectedConstructor() {
            value = 10;
        }
    }

    @Test
    public void testPackageFriendlyConstructor() {
        long version = stm.getVersion();

        PackageFriendlyConstructor o = new PackageFriendlyConstructor();

        assertEquals(version, stm.getVersion());
        assertEquals(10, o.value);

    }

    static class PackageFriendlyConstructor {

        private int value;

        @TransactionalConstructor
        PackageFriendlyConstructor() {
            value = 10;
        }
    }

    @Test
    public void testPrivateFriendlyConstructor() {
        long version = stm.getVersion();

        PrivateConstructor o = new PrivateConstructor();

        assertEquals(version, stm.getVersion());
        assertEquals(10, o.value);
    }

    static class PrivateConstructor {

        private int value;

        @TransactionalConstructor
        private PrivateConstructor() {
            value = 10;
        }
    }

    
}
