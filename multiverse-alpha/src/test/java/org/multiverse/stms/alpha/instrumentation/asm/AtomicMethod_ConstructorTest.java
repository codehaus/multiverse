package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.stms.alpha.AlphaStm;

public class AtomicMethod_ConstructorTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void noArgAtomicConstructor() {
        long version = stm.getTime();

        NoArgAtomicConstructor o = new NoArgAtomicConstructor();
        assertEquals(version, stm.getTime());
        assertEquals(20, o.value);
    }

    static class NoArgAtomicConstructor {

        int value;

        @AtomicMethod NoArgAtomicConstructor() {
            this.value = 20;
        }
    }

    @Test
    public void singleArgAtomicConstructor() {
        long version = stm.getTime();

        SingleArgAtomicConstructor o = new SingleArgAtomicConstructor(20);

        assertEquals(version, stm.getTime());
        assertEquals(20, o.arg);
    }

    static class SingleArgAtomicConstructor {

        int arg;

        @AtomicMethod SingleArgAtomicConstructor(int value) {
            this.arg = value;
        }
    }

    @Test
    public void multiArgAtomicConstructor() {
        long version = stm.getTime();

        MultiArgAtomicConstructor o = new MultiArgAtomicConstructor(6, 7, 8, 9);

        assertEquals(version, stm.getTime());
        assertEquals(6, o.arg1);
        assertEquals(7, o.arg2);
        assertEquals(8, o.arg3);
        assertEquals(9, o.arg4);
    }

    static class MultiArgAtomicConstructor {

        int arg1, arg2, arg3, arg4;

        @AtomicMethod MultiArgAtomicConstructor(int arg1, int arg2, int arg3, int arg4) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
        }
    }

    @Test
    public void constructorWithCheckedException() {
        long version = stm.getTime();
        try {
            new ConstructorWithCheckedException(true);
        } catch (Exception e) {
        }

        assertEquals(version, stm.getTime());
    }

    @Test
    public void constructorWithCheckedExceptionThatIsNotThrown() throws Exception {
        long version = stm.getTime();
        ConstructorWithCheckedException o = new ConstructorWithCheckedException(false);

        assertEquals(version, stm.getTime());
    }

    static class ConstructorWithCheckedException {

        @AtomicMethod ConstructorWithCheckedException(boolean throwIt) throws Exception {
            if (throwIt) {
                throw new Exception();
            }
        }
    }

    @Test
    public void testPublicConstructor() {
        long version = stm.getTime();

        PublicConstructor o = new PublicConstructor();

        assertEquals(10, o.value);
        assertEquals(version, stm.getTime());
    }

    static class PublicConstructor {

        private int value;

        @AtomicMethod
        public PublicConstructor() {
            value = 10;
        }
    }

    @Test
    public void testFinalField() {
        long version = stm.getTime();

        FinalField o = new FinalField(1);

        assertEquals(version, stm.getTime());
        assertEquals(1, o.value);
    }

    static class FinalField {

        private int value;

        @AtomicMethod FinalField(int value) {
            this.value = value;
        }
    }

    @Test
    public void testProtectedConstructor() {
        long version = stm.getTime();

        ProtectedConstructor o = new ProtectedConstructor();

        assertEquals(version, stm.getTime());
        assertEquals(10, o.value);
    }

    static class ProtectedConstructor {

        private int value;

        @AtomicMethod
        protected ProtectedConstructor() {
            value = 10;
        }
    }

    @Test
    public void testPackageFriendlyConstructor() {
        long version = stm.getTime();

        PackageFriendlyConstructor o = new PackageFriendlyConstructor();

        assertEquals(version, stm.getTime());
        assertEquals(10, o.value);

    }

    static class PackageFriendlyConstructor {

        private int value;

        @AtomicMethod PackageFriendlyConstructor() {
            value = 10;
        }
    }

    @Test
    public void testPrivateFriendlyConstructor() {
        long version = stm.getTime();

        PrivateConstructor o = new PrivateConstructor();

        assertEquals(version, stm.getTime());
        assertEquals(10, o.value);
    }

    static class PrivateConstructor {

        private int value;

        @AtomicMethod
        private PrivateConstructor() {
            value = 10;
        }
    }
}
