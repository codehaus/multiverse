package org.multiverse.datastructures.refs;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class IntRefTest {

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void test() {
        IntRef i = new IntRef();
        assertEquals(0, i.get());
    }

    @Test
    public void testSet() {
        IntRef i = new IntRef(0);
        i.setValue(10);
        assertEquals(10, i.get());
    }
}
