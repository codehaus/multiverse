package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class SynchronizedBlockTest {

    @Test
    @Ignore
    public void whenUsedAsExpression() {

    }

    @Test
    public void whenUsedInsideBlock() {
        IntRef ref = new IntRef();
        methodWithSynchronizedBlock(false,ref);
        assertEquals(1, ref.get());
    }

    public void methodWithSynchronizedBlock(boolean exception, IntRef ref) {
        synchronized (this) {
            ref.inc();
            if (exception) {
                throw new ExpectedRuntimeException();
            }
        }
    }

    @Test
    @Ignore
    public void whenExceptionThrown() {

    }
}
