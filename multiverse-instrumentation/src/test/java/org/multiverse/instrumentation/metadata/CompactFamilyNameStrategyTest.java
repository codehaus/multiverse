package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class CompactFamilyNameStrategyTest {

    private CompactFamilyNameStrategy strategy;

    @Before
    public void setUp() {
        strategy = new CompactFamilyNameStrategy();
    }

    @Test
    public void testGetPackages() {
        String[] packages = strategy.getPackages("Foo");
        assertEquals(0, packages.length);

        packages = strategy.getPackages("org/Foo");
        assertEquals(1, packages.length);
        assertEquals("org", packages[0]);

        packages = strategy.getPackages("org/bla/Foo");
        assertEquals(2, packages.length);
        assertEquals("org", packages[0]);
        assertEquals("bla", packages[1]);
    }

    @Test
    public void testAllJavaLangObjects() {
        String result = strategy.create("java/lang/String", "foo", "(Ljava/lang/String;)Ljava/lang/String;");
        assertEquals("String.foo(String)", result);
    }

    @Test
    public void testNoPackage() {
        String result = strategy.create("Foo", "foo", "(LBar;)LFooBar;");
        assertEquals("Foo.foo(Bar)", result);
    }

    @Test
    public void testCompactOwner() {
        String result = strategy.create("org/foobar/Foo", "foo", "()V");
        assertEquals("o.f.Foo.foo()", result);
    }

    @Test
    public void testCompactArguments() {
        String result = strategy.create("Bar", "foo", "(Lorg/foo/Foo;)V");
        assertEquals("Bar.foo(o.f.Foo)", result);
    }

    @Test
    public void testNoArgMethod() {
        String result = strategy.create("java/lang/String", "foo", "()V");
        assertEquals("String.foo()", result);
    }

    @Test
    public void testPrimitiveArg() {
        String result = strategy.create("java/lang/String", "foo", "(I)V");
        assertEquals("String.foo(int)", result);
    }

    @Test
    public void testComplex() {
        String result = strategy.create("java/lang/String", "foo", "(Ljava/lang/String;ILjava/util/LinkedList;)Ljava/lang/String;");
        assertEquals("String.foo(String,int,j.u.LinkedList)", result);
    }
}
