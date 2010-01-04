package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.transactional.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class StaticInnerClassTest {


    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void publicConstructor() {
        InnerClassWithPublicConstructor foo = new InnerClassWithPublicConstructor(1);
        assertEquals(1, foo.get());

        foo.set(10);
        assertEquals(10, foo.get());
    }

    @TransactionalObject
    static class InnerClassWithPublicConstructor {
        private int value;

        public InnerClassWithPublicConstructor(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }

        public void set(int value) {
            this.value = value;
        }
    }

    @Test
    public void privateConstructor() {
        StaticInnerClassWithPrivateConstructor privateConstructor = new StaticInnerClassWithPrivateConstructor(1);
        assertEquals(1, privateConstructor.get());
    }

    @TransactionalObject
    static class StaticInnerClassWithPrivateConstructor {
        private int value;

        private StaticInnerClassWithPrivateConstructor(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }

        public void set(int value) {
            this.value = value;
        }
    }

    @Test
    public void protectedConstructor() {
        StaticInnerClassWithProtectedConstructor x = new StaticInnerClassWithProtectedConstructor(1);
        assertEquals(1, x.get());
    }

    @TransactionalObject
    static class StaticInnerClassWithProtectedConstructor {
        private int value;

        private StaticInnerClassWithProtectedConstructor(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }

        public void set(int value) {
            this.value = value;
        }
    }

    @Test
    public void packageFriendlyConstructor() {
        StaticInnerClassWithPackageFriendlyConstructor x = new StaticInnerClassWithPackageFriendlyConstructor(1);
        assertEquals(1, x.get());
    }

    @TransactionalObject
    static class StaticInnerClassWithPackageFriendlyConstructor {
        private int value;

        private StaticInnerClassWithPackageFriendlyConstructor(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }

        public void set(int value) {
            this.value = value;
        }
    }
}
