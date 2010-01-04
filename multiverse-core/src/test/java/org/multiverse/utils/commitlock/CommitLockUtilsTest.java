package org.multiverse.utils.commitlock;

import org.junit.Test;
import org.multiverse.api.Transaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.utils.commitlock.CommitLockUtils.nothingToLock;
import static org.multiverse.utils.commitlock.CommitLockUtils.releaseLocks;

public class CommitLockUtilsTest {

    @Test
    public void testNothingToLock() {
        assertTrue(nothingToLock(null));
        assertTrue(nothingToLock(new CommitLock[]{}));
        assertTrue(nothingToLock(new CommitLock[1]));
        assertFalse(nothingToLock(new CommitLock[]{new DummyCommitLock()}));
    }

    @Test(expected = NullPointerException.class)
    public void releaseLocksWithNullTransactionFails() {
        releaseLocks(new CommitLock[]{}, null);
    }

    @Test
    public void releaseLocksWithNullLocksSucceeds() {
        releaseLocks(null, mock(Transaction.class));
    }


    @Test
    public void releaseLocksWithEmptyLocksSucceeds() {
        releaseLocks(new CommitLock[]{}, mock(Transaction.class));
    }

    @Test
    public void test() {
        testIncomplete();
    }

    class DummyCommitLock implements CommitLock {

        @Override
        public void ___releaseLock(Transaction expectedLockOwner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean ___tryLock(Transaction lockOwner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Transaction ___getLockOwner() {
            throw new UnsupportedOperationException();
        }
    }
}
