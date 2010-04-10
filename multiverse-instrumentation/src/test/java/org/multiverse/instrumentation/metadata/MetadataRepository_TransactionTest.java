package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;

import static org.junit.Assert.*;

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
        assertNull(transactionMetadata.automaticReadTrackingEnabled);
        assertNull(transactionMetadata.readOnly);
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.writeSkewProblemAllowed);
        assertEquals(1000, transactionMetadata.maxRetryCount);
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
        assertNull(transactionMetadata.automaticReadTrackingEnabled);
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.writeSkewProblemAllowed);
        assertEquals(1000, transactionMetadata.maxRetryCount);
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
        assertNull(transactionMetadata.automaticReadTrackingEnabled);
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.writeSkewProblemAllowed);
        assertEquals(1000, transactionMetadata.maxRetryCount);
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
        assertTrue(enabledTransactionMetadata.automaticReadTrackingEnabled);

        MethodMetadata disabledMethodMetadata = classMetadata.getMethodMetadata("disabled", "()V");

        TransactionMetadata disabledTransactionMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(disabledTransactionMetadata);
        assertFalse(disabledTransactionMetadata.automaticReadTrackingEnabled);
    }

    class AutomaticReadTracking {
        @TransactionalMethod(automaticReadTrackingEnabled = true)
        void enabled() {
        }

        @TransactionalMethod(automaticReadTrackingEnabled = false)
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

        @TransactionalMethod(automaticReadTrackingEnabled = false)
        void disabled() {
        }
    }


    @Test
    public void whenAllowWriteSkewProblem() {
        ClassMetadata classMetadata = repository.loadClassMetadata(AllowWriteSkewProblem.class);

        MethodMetadata enabledMethodMetadata = classMetadata.getMethodMetadata("enabled", "()V");
        TransactionMetadata enabledTransactionMetadata = enabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(enabledTransactionMetadata);
        assertTrue(enabledTransactionMetadata.writeSkewProblemAllowed);

        MethodMetadata disabledMethodMetadata = classMetadata.getMethodMetadata("disabled", "()V");
        TransactionMetadata disabledTransactionMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(disabledTransactionMetadata);
        assertFalse(disabledTransactionMetadata.writeSkewProblemAllowed);

        MethodMetadata nowAllowedAndDefaultNonAutomaticReadTracking = classMetadata.getMethodMetadata("nowAllowedAndDefaultNonAutomaticReadTracking", "()V");
        TransactionMetadata nowAllowedAndDefaultNonAutomaticReadTrackingMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(nowAllowedAndDefaultNonAutomaticReadTracking);
        assertFalse(nowAllowedAndDefaultNonAutomaticReadTrackingMetadata.writeSkewProblemAllowed);
        assertTrue(nowAllowedAndDefaultNonAutomaticReadTrackingMetadata.automaticReadTrackingEnabled);

        MethodMetadata nowAllowedAndAutomaticReadTracking = classMetadata.getMethodMetadata("nowAllowedAndAutomaticReadTracking", "()V");
        TransactionMetadata nowAllowedAndAutomaticReadTrackingMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(nowAllowedAndAutomaticReadTracking);
        assertFalse(nowAllowedAndAutomaticReadTrackingMetadata.writeSkewProblemAllowed);
        assertTrue(nowAllowedAndAutomaticReadTrackingMetadata.automaticReadTrackingEnabled);

    }

    class AllowWriteSkewProblem {

        @TransactionalMethod(writeSkewProblemAllowed = false)
        void nowAllowedAndDefaultNonAutomaticReadTracking() {
        }

        @TransactionalMethod(writeSkewProblemAllowed = false, automaticReadTrackingEnabled = false)
        void nowAllowedAndAutomaticReadTracking() {
        }


        @TransactionalMethod(automaticReadTrackingEnabled = true, writeSkewProblemAllowed = true)
        void enabled() {
        }

        @TransactionalMethod(automaticReadTrackingEnabled = true, writeSkewProblemAllowed = false)
        void disabled() {
        }
    }

    @Test
    public void whenMaxRetryCount() {
        ClassMetadata classMetadata = repository.loadClassMetadata(MaxRetryCountProblem.class);
        MethodMetadata explicitValueMethodMetadata = classMetadata.getMethodMetadata("explicitValue", "()V");
        TransactionMetadata explicitValueTransactionMetadata = explicitValueMethodMetadata.getTransactionalMetadata();

        assertNotNull(explicitValueTransactionMetadata);
        assertEquals(100, explicitValueTransactionMetadata.maxRetryCount);

        MethodMetadata defaultValueMethodMetadata = classMetadata.getMethodMetadata("defaultValue", "()V");

        TransactionMetadata defaultValueTransactionMetadata = defaultValueMethodMetadata.getTransactionalMetadata();
        assertNotNull(defaultValueTransactionMetadata);
        assertEquals(1000, defaultValueTransactionMetadata.maxRetryCount);
    }

    class MaxRetryCountProblem {
        @TransactionalMethod(maxRetryCount = 100)
        void explicitValue() {
        }

        @TransactionalMethod
        void defaultValue() {
        }
    }
}
