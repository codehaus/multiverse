package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaRef_constructionTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void noArgConstruction() {
        long version = stm.getVersion();

        AlphaRef<String> ref = new AlphaRef<String>();

        assertEquals(version, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void nullConstruction() {
        long version = stm.getVersion();

        AlphaRef<String> ref = new AlphaRef<String>();

        assertEquals(version, stm.getVersion());
        assertEquals(null, ref.get());
    }

    @Test
    public void nonNullConstruction() {
        long version = stm.getVersion();
        String s = "foo";
        AlphaRef<String> ref = new AlphaRef<String>(s);

        assertEquals(version, stm.getVersion());
        assertEquals(s, ref.get());
    }

}
