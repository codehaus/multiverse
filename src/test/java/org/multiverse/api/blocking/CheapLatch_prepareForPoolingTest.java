package org.multiverse.api.blocking;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertClosed;

public class CheapLatch_prepareForPoolingTest {

    @Test
    public void whenClosed() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.prepareForPooling();

        assertClosed(latch);
        assertEquals(era + 1, latch.getEra());
    }

    @Test
    public void whenOpen() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.open(era);

        latch.prepareForPooling();
        assertClosed(latch);
        assertEquals(era + 1, latch.getEra());
    }
}
