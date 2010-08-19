package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_constructionTest {

    @Test
    public void test() {
        UnsafeOrec orec = new UnsafeOrec();

        assertEquals(0, orec.___getSurplus());
        assertFalse(orec.___isLocked());
        assertFalse(orec.___isReadBiased());
    }
}
