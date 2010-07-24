package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;
import static org.multiverse.stms.beta.orec.OrecTestUtils.makeReadBiased;

/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_miscTest {

    @Test
    public void testToReadonly(){
        UnsafeOrec orec = makeReadBiased(new UnsafeOrec());

        assertTrue(orec.isReadBiased());
        assertUnlocked(orec);
        assertSurplus(0, orec);
    }
}
