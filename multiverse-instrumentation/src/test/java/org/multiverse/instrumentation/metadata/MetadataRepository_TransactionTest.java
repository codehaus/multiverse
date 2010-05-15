package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.LogLevel;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class MetadataRepository_TransactionTest {
    private MetadataRepository repository;

    @Before
    public void setUp() {
        repository = new MetadataRepository();
    }

    @Test
    public void whenDefaultSettings() {
        ClassMetadata classMetadata = repository.loadClassMetadata(DefaultSettings.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("method", "()V");
        TransactionMetadata transactionMetadata = methodMetadata.getTransactionalMetadata();

        assertNotNull(transactionMetadata);
        assertNull(transactionMetadata.trackReads);
        assertNull(transactionMetadata.readOnly);
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.writeSkew);
        assertEquals(1000, transactionMetadata.maxRetries);
        assertEquals("org.multiverse.instrumentation.metadata.MetadataRepository_TransactionTest$DefaultSettings.method()", transactionMetadata.familyName);
    }

    class DefaultSettings {
        @TransactionalMethod
        void method() {
        }
    }

    @Test
    public void whenReadonlyMethod() {
        ClassMetadata classMetadata = repository.loadClassMetadata(ReadonlyMethod.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("method", "()V");
        TransactionMetadata transactionMetadata = methodMetadata.getTransactionalMetadata();

        assertNotNull(transactionMetadata);
        assertTrue(transactionMetadata.readOnly);
        assertNull(transactionMetadata.trackReads);
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.writeSkew);
        assertEquals(1000, transactionMetadata.maxRetries);
        assertEquals("org.multiverse.instrumentation.metadata.MetadataRepository_TransactionTest$ReadonlyMethod.method()", transactionMetadata.familyName);
    }

    class ReadonlyMethod {
        @TransactionalMethod(readonly = true)
        void method() {
        }
    }

    @Test
    public void whenUpdateMethod() {
        ClassMetadata classMetadata = repository.loadClassMetadata(DefaultUpdateMethod.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("method", "()V");
        TransactionMetadata transactionMetadata = methodMetadata.getTransactionalMetadata();

        assertNotNull(transactionMetadata);
        assertFalse(transactionMetadata.readOnly);
        assertNull(transactionMetadata.trackReads);
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.writeSkew);
        assertEquals(1000, transactionMetadata.maxRetries);
        assertEquals("org.multiverse.instrumentation.metadata.MetadataRepository_TransactionTest$DefaultUpdateMethod.method()", transactionMetadata.familyName);
    }


    class DefaultUpdateMethod {
        @TransactionalMethod(readonly = false)
        void method() {
        }
    }


    @Test
    public void whenAutomaticReadTracking() {
        ClassMetadata classMetadata = repository.loadClassMetadata(AutomaticReadTracking.class);
        MethodMetadata enabledMethodMetadata = classMetadata.getMethodMetadata("enabled", "()V");
        TransactionMetadata enabledTransactionMetadata = enabledMethodMetadata.getTransactionalMetadata();

        assertNotNull(enabledTransactionMetadata);
        assertTrue(enabledTransactionMetadata.trackReads);

        MethodMetadata disabledMethodMetadata = classMetadata.getMethodMetadata("disabled", "()V");

        TransactionMetadata disabledTransactionMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(disabledTransactionMetadata);
        assertFalse(disabledTransactionMetadata.trackReads);
    }

    class AutomaticReadTracking {
        @TransactionalMethod(trackReads = true)
        void enabled() {
        }

        @TransactionalMethod(trackReads = false)
        void disabled() {
        }
    }

    @Test
    public void whenInterrupted() {
        ClassMetadata classMetadata = repository.loadClassMetadata(Interrupted.class);
        MethodMetadata enabledMethodMetadata = classMetadata.getMethodMetadata("enabled", "()V");
        TransactionMetadata enabledTransactionMetadata = enabledMethodMetadata.getTransactionalMetadata();

        assertNotNull(enabledTransactionMetadata);
        assertTrue(enabledTransactionMetadata.interruptible);

        MethodMetadata disabledMethodMetadata = classMetadata.getMethodMetadata("disabled", "()V");

        TransactionMetadata disabledTransactionMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(disabledTransactionMetadata);
        assertFalse(disabledTransactionMetadata.interruptible);
    }

    class Interrupted {
        @TransactionalMethod(interruptible = true)
        void enabled() throws InterruptedException {
        }

        @TransactionalMethod(trackReads = false)
        void disabled() {
        }
    }


    @Test
    public void whenWriteSkewAllowed() {
        ClassMetadata classMetadata = repository.loadClassMetadata(WriteSkewAllowed.class);

        MethodMetadata enabledMethodMetadata = classMetadata.getMethodMetadata("enabled", "()V");
        TransactionMetadata enabledTransactionMetadata = enabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(enabledTransactionMetadata);
        assertTrue(enabledTransactionMetadata.writeSkew);

        MethodMetadata disabledMethodMetadata = classMetadata.getMethodMetadata("disabled", "()V");
        TransactionMetadata disabledTransactionMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(disabledTransactionMetadata);
        assertFalse(disabledTransactionMetadata.writeSkew);

        MethodMetadata nowAllowedAndDefaultNonAutomaticReadTracking = classMetadata.getMethodMetadata("nowAllowedAndDefaultNonAutomaticReadTracking", "()V");
        TransactionMetadata nowAllowedAndDefaultNonAutomaticReadTrackingMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(nowAllowedAndDefaultNonAutomaticReadTracking);
        assertFalse(nowAllowedAndDefaultNonAutomaticReadTrackingMetadata.writeSkew);
        assertTrue(nowAllowedAndDefaultNonAutomaticReadTrackingMetadata.trackReads);

        MethodMetadata nowAllowedAndAutomaticReadTracking = classMetadata.getMethodMetadata("nowAllowedAndAutomaticReadTracking", "()V");
        TransactionMetadata nowAllowedAndAutomaticReadTrackingMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(nowAllowedAndAutomaticReadTracking);
        assertFalse(nowAllowedAndAutomaticReadTrackingMetadata.writeSkew);
        assertTrue(nowAllowedAndAutomaticReadTrackingMetadata.trackReads);

    }

    class WriteSkewAllowed {

        @TransactionalMethod(writeSkew = false)
        void nowAllowedAndDefaultNonAutomaticReadTracking() {
        }

        @TransactionalMethod(writeSkew = false, trackReads = false)
        void nowAllowedAndAutomaticReadTracking() {
        }


        @TransactionalMethod(trackReads = true, writeSkew = true)
        void enabled() {
        }

        @TransactionalMethod(trackReads = true, writeSkew = false)
        void disabled() {
        }
    }

    @Test
    public void whenMaxRetryCount() {
        ClassMetadata classMetadata = repository.loadClassMetadata(MaxRetryCountProblem.class);
        MethodMetadata explicitValueMethodMetadata = classMetadata.getMethodMetadata("explicitValue", "()V");
        TransactionMetadata explicitValueTransactionMetadata = explicitValueMethodMetadata.getTransactionalMetadata();

        assertNotNull(explicitValueTransactionMetadata);
        assertEquals(100, explicitValueTransactionMetadata.maxRetries);

        MethodMetadata defaultValueMethodMetadata = classMetadata.getMethodMetadata("defaultValue", "()V");

        TransactionMetadata defaultValueTransactionMetadata = defaultValueMethodMetadata.getTransactionalMetadata();
        assertNotNull(defaultValueTransactionMetadata);
        assertEquals(1000, defaultValueTransactionMetadata.maxRetries);
    }

    class MaxRetryCountProblem {
        @TransactionalMethod(maxRetries = 100)
        void explicitValue() {
        }

        @TransactionalMethod
        void defaultValue() {
        }
    }

    @Test
    public void whenTimeout() {
        ClassMetadata classMetadata = repository.loadClassMetadata(Timeout.class);
        MethodMetadata explicitValueMethodMetadata = classMetadata.getMethodMetadata("explicitValue", "()V");
        TransactionMetadata explicitValueTransactionMetadata = explicitValueMethodMetadata.getTransactionalMetadata();

        assertNotNull(explicitValueTransactionMetadata);
        assertEquals(TimeUnit.HOURS.toNanos(10), explicitValueTransactionMetadata.timeoutNs);

        MethodMetadata defaultValueMethodMetadata = classMetadata.getMethodMetadata("defaultValue", "()V");

        TransactionMetadata defaultValueTransactionMetadata = defaultValueMethodMetadata.getTransactionalMetadata();
        assertNotNull(defaultValueTransactionMetadata);
        assertEquals(Long.MAX_VALUE, defaultValueTransactionMetadata.timeoutNs);

        MethodMetadata explicitValueWithoutTimeUnit = classMetadata.getMethodMetadata("explicitValueWithoutTimeUnit", "()V");

        TransactionMetadata explicitValueWithoutTimeUnitTransactionMetadata = explicitValueWithoutTimeUnit.getTransactionalMetadata();
        assertNotNull(explicitValueWithoutTimeUnitTransactionMetadata);
        assertEquals(TimeUnit.SECONDS.toNanos(10), explicitValueWithoutTimeUnitTransactionMetadata.timeoutNs);
    }

    class Timeout {
        @TransactionalMethod(timeout = 10, timeoutTimeUnit = TimeUnit.HOURS)
        void explicitValue() {
            Transaction tx = getThreadLocalTransaction();
            assertEquals(TimeUnit.HOURS.toNanos(10), tx.getConfiguration().getTimeoutNs());
        }

        @TransactionalMethod(timeout = 10)
        void explicitValueWithoutTimeUnit() {
            Transaction tx = getThreadLocalTransaction();
            assertEquals(TimeUnit.SECONDS.toNanos(10), tx.getConfiguration().getTimeoutNs());
        }


        @TransactionalMethod
        void defaultValue() {
            Transaction tx = getThreadLocalTransaction();
            assertEquals(Long.MAX_VALUE, tx.getConfiguration().getTimeoutNs());
        }
    }


    @Test
    public void whenLogLevel() {
        ClassMetadata classMetadata = repository.loadClassMetadata(LogLevelObject.class);
        MethodMetadata explicitValueMethodMetadata = classMetadata.getMethodMetadata("explicitValue", "()V");
        TransactionMetadata explicitValueTransactionMetadata = explicitValueMethodMetadata.getTransactionalMetadata();

        assertNotNull(explicitValueTransactionMetadata);
        assertEquals(LogLevel.course, explicitValueTransactionMetadata.logLevel);

        MethodMetadata defaultValueMethodMetadata = classMetadata.getMethodMetadata("defaultValue", "()V");

        TransactionMetadata defaultValueTransactionMetadata = defaultValueMethodMetadata.getTransactionalMetadata();
        assertNotNull(defaultValueTransactionMetadata);
        assertEquals(LogLevel.none, defaultValueTransactionMetadata.logLevel);
    }

    class LogLevelObject {
        @TransactionalMethod(logLevel = LogLevel.course)
        void explicitValue() {
            Transaction tx = getThreadLocalTransaction();
            assertEquals(TimeUnit.HOURS.toNanos(10), tx.getConfiguration().getTimeoutNs());
        }

        @TransactionalMethod
        void defaultValue() {
            Transaction tx = getThreadLocalTransaction();
            assertEquals(Long.MAX_VALUE, tx.getConfiguration().getTimeoutNs());
        }
    }
}
