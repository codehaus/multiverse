package org.multiverse.stms.alpha.instrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.transactional.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertNotNull;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class NonStaticInnerClassThatIsNotTransactionalObjectTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void testAnonymousInnerClass() {
        AnonymousInnerClass o = new AnonymousInnerClass();
        assertNotNull(o.getRunnable());
    }

    @TransactionalObject
    public static class AnonymousInnerClass {

        private Runnable runnable;

        public AnonymousInnerClass() {
            runnable = new Runnable() {
                @Override
                public void run() {
                }
            };
        }

        public Runnable getRunnable() {
            return runnable;
        }
    }

    @Test
    public void testNamedInnerClass() {
        NamedInnerClass o = new NamedInnerClass();
        assertNotNull(o.getNamedRunnable());
    }

    @TransactionalObject
    public static class NamedInnerClass {

        private NamedRunnable someRunnable;

        public NamedInnerClass() {
            someRunnable = new NamedRunnable();
        }

        class NamedRunnable implements Runnable {
            @Override
            public void run() {
                //todo
            }
        }

        public NamedRunnable getNamedRunnable() {
            return someRunnable;
        }
    }
}