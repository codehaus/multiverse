package org.multiverse.utils.commitlock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.multiverse.DummyTransaction;
import static org.multiverse.TestUtils.testIncomplete;
import org.multiverse.api.Transaction;
import static org.multiverse.utils.commitlock.CommitLockUtils.nothingToLock;
import static org.multiverse.utils.commitlock.CommitLockUtils.releaseLocks;

public class CommitLockUtilsTest {

    @Test
    public void testNothingToLock(){
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
        releaseLocks(null, new DummyTransaction());
    }

    @Test
    public void releaseLocksWithEmptyLocksSucceeds() {
        releaseLocks(new CommitLock[]{}, new DummyTransaction());
    }

    @Test
    public void test() {
        testIncomplete();
    }

    class DummyCommitLock implements CommitLock{        
        @Override
        public void releaseLock(Transaction expectedLockOwner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CommitLockResult tryLockAndDetectConflicts(Transaction lockOwner) {
            throw new UnsupportedOperationException();
        }
    }
}
