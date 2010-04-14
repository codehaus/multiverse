package org.multiverse.transactional.arrays;

import org.junit.Before;
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
    public void whenArrayRemainsTheSame() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(3);
        array.set(0, "a");
        array.set(2, "c");

        long version = stm.getVersion();
        TransactionalReferenceArray<String> newarray = array.copyToBiggerArray(3);

        assertEquals(version, stm.getVersion());
        assertEquals(3, newarray.length());
        assertEquals("[a, null, c]", newarray.toString());
    }

    @Test
    public void whenArrayGrows() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(3);
        array.set(0, "a");
        array.set(2, "c");

        long version = stm.getVersion();
        TransactionalReferenceArray<String> newarray = array.copyToBiggerArray(5);

        assertEquals(version, stm.getVersion());
        assertEquals(5, newarray.length());
        assertEquals("[a, null, c, null, null]", newarray.toString());
    }
}
