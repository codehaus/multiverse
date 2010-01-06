package org.multiverse.stms.alpha.integrationtests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.api.exceptions.WriteConflictException;
import org.multiverse.stms.alpha.AlphaStm;

public class RetryCountTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @Test
    public void testNoRetry() {
        NoRetriesMethod method = new NoRetriesMethod();

        try {
            method.execute();
            fail();
        } catch (TooManyRetriesException expected) {
        }

        assertEquals(1, method.count);
    }

    static class NoRetriesMethod {
        int count;

        @AtomicMethod(retryCount = 0)
        public void execute() {
            count++;
            throw new WriteConflictException();
        }
    }

    @Test
    public void testOneRetry() {
        OneRetriesMethod method = new OneRetriesMethod();

        try {
            method.execute();
            fail();
        } catch (TooManyRetriesException expected) {
        }

        assertEquals(2, method.count);
    }

    static class OneRetriesMethod {
        int count;

        @AtomicMethod(retryCount = 1)
        public void execute() {
            count++;
            throw new WriteConflictException();
        }
    }

    @Test
    public void testMultipleRetries() {
        TenRetriesMethod method = new TenRetriesMethod();

        try {
            method.execute();
            fail();
        } catch (TooManyRetriesException expected) {
        }

        assertEquals(11, method.count);
    }

    static class TenRetriesMethod {
        int count;

        @AtomicMethod(retryCount = 10)
        public void execute() {
            count++;
            throw new WriteConflictException();
        }
    }
}
