package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class FullFamilynameStrategyTest {

    private FullFamilyNameStrategy strategy;

    @Before
    public void setUp() {
        strategy = new FullFamilyNameStrategy();
    }

    @Test
    public void testNoPackage() {
        String result = strategy.create("Foo", "foo", "(LBar;)LFooBar;");
        assertEquals("Foo.foo(Bar)", result);
    }

    @Test
    public void testAllObjects() {
        String result = strategy.create("java/lang/String", "foo", "(Ljava/lang/String;)Ljava/lang/String;");
        assertEquals("java.lang.String.foo(java.lang.String)", result);
    }

    @Test
    public void testNoArgMethod() {
        String result = strategy.create("java/lang/String", "foo", "()V");
        assertEquals("java.lang.String.foo()", result);
    }

    @Test
    public void testPrimitiveArg() {
        String result = strategy.create("java/lang/String", "foo", "(I)V");
        assertEquals("java.lang.String.foo(int)", result);
    }

    @Test
    public void testComplex() {
        String result = strategy.create("java/lang/String", "foo", "(Ljava/lang/String;ILjava/util/LinkedList;)Ljava/lang/String;");
        assertEquals("java.lang.String.foo(java.lang.String,int,java.util.LinkedList)", result);
    }
}
