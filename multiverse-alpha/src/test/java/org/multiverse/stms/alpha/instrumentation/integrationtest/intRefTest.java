package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestUtils;
import org.multiverse.stms.alpha.instrumentation.metadata.MetadataRepository;
import org.multiverse.transactional.primitives.TransactionalInteger;
import org.multiverse.utils.instrumentation.InstrumentationProblemMonitor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.existsField;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.existsTranlocalField;

/**
 * @author Peter Veentjer
 */
public class intRefTest {

    @Before
    public void setUp() {
        TestUtils.resetInstrumentationProblemMonitor();
    }

    @After
    public void tearDown() {
        assertFalse(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
    }

    @Test
    public void testStructuralContent() {
        TransactionalInteger.class.toString();

        MetadataRepository repo = MetadataRepository.INSTANCE;
        assertFalse(existsField(TransactionalInteger.class, "value"));
        assertTrue(existsTranlocalField(TransactionalInteger.class, "value"));
    }
}
