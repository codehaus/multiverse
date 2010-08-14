package org.multiverse.api.blocking;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertClosed;

public class StandardLatch_prepareForPoolingTest {

    @Test
    public void whenClosed() {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();
        latch.prepareForPooling();

        assertClosed(latch);
        assertEquals(era + 1, latch.getEra());
    }

    @Test
    public void whenOpen() {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();
        latch.open(era);

        latch.prepareForPooling();
        assertClosed(latch);
        assertEquals(era + 1, latch.getEra());
    }
}
