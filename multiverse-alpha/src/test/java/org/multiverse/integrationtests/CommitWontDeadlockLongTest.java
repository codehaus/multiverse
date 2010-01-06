package org.multiverse.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestUtils;


/**
 * A test that checks that committing transactions, won't cause deadlocks.
 * <p/>
 * Tests for direct deadlocks and deadlock chains.
 */
public class CommitWontDeadlockLongTest {

    @Before
    public void setUp() {

    }

    @Test
    public void test() {
        TestUtils.testIncomplete();
    }
}
