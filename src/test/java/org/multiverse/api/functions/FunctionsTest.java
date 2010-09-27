package org.multiverse.api.functions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.functions.Functions.newIntIdentityFunction;
import static org.multiverse.api.functions.Functions.newLongIdentityFunction;

public class FunctionsTest {

    @Test
    public void testIntIdentityFunction(){
        IntFunction function = newIntIdentityFunction();

        assertEquals(0, function.call(0));
        assertEquals(10, function.call(10));
        assertEquals(-10, function.call(-10));
    }

    @Test
    public void testLongIdentityFunction(){
        LongFunction function = newLongIdentityFunction();

        assertEquals(0, function.call(0));
        assertEquals(10, function.call(10));
        assertEquals(-10, function.call(-10));
    }
}
