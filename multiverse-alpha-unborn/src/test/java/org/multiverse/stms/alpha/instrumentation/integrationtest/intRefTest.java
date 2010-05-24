package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.javaagent.JavaAgentProblemMonitor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.instrumentation.InstrumentationTestUtils.resetInstrumentationProblemMonitor;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.existsField;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.existsTranlocalField;

/**
 * @author Peter Veentjer
 */
public class intRefTest {

    @Before
    public void setUp() {
        resetInstrumentationProblemMonitor();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
    }

    @Test
    public void testStructuralContent() {
        org.multiverse.transactional.refs.IntRef.class.toString();

        assertFalse(existsField(IntRef.class, "value"));
        assertTrue(existsTranlocalField(IntRef.class, "value"));
    }

    @TransactionalObject
    static class IntRef {
        private int value;

        public IntRef() {
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
