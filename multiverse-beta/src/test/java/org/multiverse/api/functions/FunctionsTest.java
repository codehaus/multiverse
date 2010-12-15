package org.multiverse.api.functions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.functions.Functions.newIdentityIntFunction;
import static org.multiverse.api.functions.Functions.newIdentityLongFunction;

public class FunctionsTest {

    @Test
    public void testIntIdentityFunction() {
        IntFunction function = newIdentityIntFunction();

        assertEquals(0, function.call(0));
        assertEquals(10, function.call(10));
        assertEquals(-10, function.call(-10));
    }

    @Test
    public void testLongIdentityFunction() {
        LongFunction function = newIdentityLongFunction();

        assertEquals(0, function.call(0));
        assertEquals(10, function.call(10));
        assertEquals(-10, function.call(-10));
    }
}
