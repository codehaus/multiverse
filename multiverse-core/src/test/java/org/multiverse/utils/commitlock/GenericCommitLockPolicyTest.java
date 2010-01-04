package org.multiverse.utils.commitlock;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.testIncomplete;

import org.multiverse.api.Transaction;

@Ignore
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

        policy.tryAcquire(commitLock, PassAllCommitLockFilter.INSTANCE,null);
    }

    @Test
    public void tryLock_succeedsWithNullLock(){
        /*
        Transaction t = mock(Transaction.class);
        boolean result = policy.tryLocks(null, t);
        assertEquals(CommitLockResult.success, result);*/
        testIncomplete();
    }

    @Test
    public void tryLock(){
        tryLock(true);
        tryLock(false);
    }

    public void tryLock(boolean expectedResult){
        CommitLock commitLock = mock(CommitLock.class);
        Transaction t = mock(Transaction.class);

        when(commitLock.___tryLock(t)).thenReturn(expectedResult);

        boolean result = policy.tryAcquire(commitLock,PassAllCommitLockFilter.INSTANCE, t);
        assertEquals(expectedResult, result);
    }


    // =================== tryLockAll =======================

    @Test(expected = NullPointerException.class)
    public void tryLockAll_nullTransactionFails() {
        policy.tryAcquireAll(new CommitLock[]{}, PassAllCommitLockFilter.INSTANCE,null);
    }

    @Test
    public void tryLockAll_nullLocksSucceeds() {
        /*
        CommitLockResult result = policy.tryLocks(null, new DummyTransaction());
        assertEquals(CommitLockResult.success, result);
        */
        testIncomplete();
    }

    @Test
    public void tryLockAll_emptyLocksSucceeds() {
        /*
        CommitLockResult result = policy.tryLocks(new CommitLock[]{}, new DummyTransaction());
        assertEquals(CommitLockResult.success, result);
        */
        testIncomplete();
    }

    @Test
    public void testToString() {
        GenericCommitLockPolicy policy = new GenericCommitLockPolicy(10, 20);
        assertEquals("GenericCommitLockPolicy(retryCount=20, spinAttemptsPerLockCount=10)",policy.toString());
    }
}
