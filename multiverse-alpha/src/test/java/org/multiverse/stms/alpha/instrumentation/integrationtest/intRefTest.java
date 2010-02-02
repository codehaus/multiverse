package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.Test;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.existsField;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.existsTranlocalField;

/**
 * @author Peter Veentjer
 */
public class intRefTest {

    @Test
    public void testStructuralContent() {
        assertFalse(existsField(TransactionalInteger.class, "value"));
        assertTrue(existsTranlocalField(TransactionalInteger.class, "value"));
    }
}
