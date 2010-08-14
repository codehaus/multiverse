package org.multiverse.api.blocking;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertClosed;

public class CheapLatch_tryAwaitTest {

    @Test
    public void whenCalledThenUnsupportedOperationException() throws InterruptedException {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        try{
        latch.tryAwait(1,1, TimeUnit.NANOSECONDS);
            fail();
        }catch(UnsupportedOperationException expected){
        }

        assertEquals(era, latch.getEra());
        assertClosed(latch);
    }
}
