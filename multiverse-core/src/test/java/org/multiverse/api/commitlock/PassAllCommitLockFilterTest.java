package org.multiverse.api.commitlock;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class PassAllCommitLockFilterTest {

    @Test
    public void test() {
        PassAllCommitLockFilter filter = PassAllCommitLockFilter.INSTANCE;
        CommitLock lock = mock(CommitLock.class);
        assertTrue(filter.needsLocking(lock));
    }
}
