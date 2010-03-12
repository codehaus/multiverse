package org.multiverse.transactional.arrays;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class TransactionalReferenceArray_copyToBiggerArrayTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenNewCapacityTooSmall() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);

        long version = stm.getVersion();
        try {
            array.copyToBiggerArray(9);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    @Ignore
    public void when() {

    }
}
