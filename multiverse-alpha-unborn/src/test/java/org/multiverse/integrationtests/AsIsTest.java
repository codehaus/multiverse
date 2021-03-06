package org.multiverse.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.transactional.refs.BasicRef;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * Tests if an object that is not transactional can flow through stm space
 *
 * @author Peter Veentjer
 */
public class AsIsTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        List<String> l = new LinkedList<String>();
        l.add("1");
        l.add("2");

        BasicRef<List<String>> ref = new BasicRef<List<String>>();
        ref.set(l);

        List<String> found = ref.get();
        assertSame(l, found);
        assertEquals("[1, 2]", l.toString());
    }
}
