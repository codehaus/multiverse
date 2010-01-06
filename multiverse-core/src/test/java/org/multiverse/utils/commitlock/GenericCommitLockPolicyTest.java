package org.multiverse.utils.commitlock;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.multiverse.DummyTransaction;
import org.multiverse.api.Transaction;

public class GenericCommitLockPolicyTest {
    private GenericCommitLockPolicy policy;

    @Before
    public void setUp() {
        policy = new GenericCommitLockPolicy(10, 10);
    }

    @Test
    public void construction(){
        GenericCommitLockPolicy policy = new GenericCommitLockPolicy(10, 20);
        assertEquals(10, policy.getSpinAttemptsPerLockCount());
        assertEquals(20, policy.getRetryCount());        
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructWithTooSmallSpinAttemptsPerLock() {
        new GenericCommitLockPolicy(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructWithTooSmallRetryCount() {
        new GenericCommitLockPolicy(0, -1);
    }


    // ====================tryLock ==========================

    @Test(expected = NullPointerException.class)
    public void tryLock_failsIfLockOwnerIsNull(){
        CommitLock commitLock = mock(CommitLock.class);

        policy.tryLockAndDetectConflict(commitLock, null);
    }

    @Test
    public void tryLock_succeedsWithNullLock(){
        Transaction t = mock(Transaction.class);
        CommitLockResult result = policy.tryLockAndDetectConflict(null, t);
        assertEquals(CommitLockResult.success, result);
    }

    @Test
    public void tryLock(){
        tryLock(CommitLockResult.success);
        tryLock(CommitLockResult.failure);
        tryLock(CommitLockResult.conflict);
    }

    public void tryLock(CommitLockResult expectedResult){
        CommitLock commitLock = mock(CommitLock.class);
        Transaction t = mock(Transaction.class);

        when(commitLock.tryLockAndDetectConflicts(t)).thenReturn(expectedResult);

        CommitLockResult result = policy.tryLockAndDetectConflict(commitLock, t);
        assertEquals(expectedResult, result);
    }


    // =================== tryLockAll =======================

    @Test(expected = NullPointerException.class)
    public void tryLockAll_nullTransactionFails() {
        policy.tryLockAllAndDetectConflicts(new CommitLock[]{}, null);
    }

    @Test
    public void tryLockAll_nullLocksSucceeds() {
        CommitLockResult result = policy.tryLockAllAndDetectConflicts(null, new DummyTransaction());
        assertEquals(CommitLockResult.success, result);
    }

    @Test
    public void tryLockAll_emptyLocksSucceeds() {
        CommitLockResult result = policy.tryLockAllAndDetectConflicts(new CommitLock[]{}, new DummyTransaction());
        assertEquals(CommitLockResult.success, result);
    }

    @Test
    public void testToString() {
        GenericCommitLockPolicy policy = new GenericCommitLockPolicy(10, 20);
        assertEquals("GenericCommitLockPolicy(retryCount=20, spinAttemptsPerLockCount=10)",policy.toString());
    }
}
