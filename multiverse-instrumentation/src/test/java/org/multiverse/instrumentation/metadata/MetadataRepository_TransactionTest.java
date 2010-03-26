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
        ClassMetadata classMetadata = repository.getClassMetadata(DefaultSettings.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("method", "()V");
        TransactionMetadata transactionMetadata = methodMetadata.getTransactionalMetadata();

        assertNotNull(transactionMetadata);
        assertNull(transactionMetadata.automaticReadTracking);
        assertNull(transactionMetadata.readOnly);
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.allowWriteSkewProblem);
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
        ClassMetadata classMetadata = repository.getClassMetadata(ReadonlyMethod.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("method", "()V");
        TransactionMetadata transactionMetadata = methodMetadata.getTransactionalMetadata();

        assertNotNull(transactionMetadata);
        assertTrue(transactionMetadata.readOnly);
        assertNull(transactionMetadata.automaticReadTracking);
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.allowWriteSkewProblem);
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
        ClassMetadata classMetadata = repository.getClassMetadata(DefaultUpdateMethod.class);
        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("method", "()V");
        TransactionMetadata transactionMetadata = methodMetadata.getTransactionalMetadata();

        assertNotNull(transactionMetadata);
        assertFalse(transactionMetadata.readOnly);
        assertNull(transactionMetadata.automaticReadTracking);
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.allowWriteSkewProblem);
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
        ClassMetadata classMetadata = repository.getClassMetadata(AutomaticReadTracking.class);
        MethodMetadata enabledMethodMetadata = classMetadata.getMethodMetadata("enabled", "()V");
        TransactionMetadata enabledTransactionMetadata = enabledMethodMetadata.getTransactionalMetadata();

        assertNotNull(enabledTransactionMetadata);
        assertTrue(enabledTransactionMetadata.automaticReadTracking);

        MethodMetadata disabledMethodMetadata = classMetadata.getMethodMetadata("disabled", "()V");

        TransactionMetadata disabledTransactionMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(disabledTransactionMetadata);
        assertFalse(disabledTransactionMetadata.automaticReadTracking);
    }

    class AutomaticReadTracking {
        @TransactionalMethod(automaticReadTracking = true)
        void enabled() {
        }

        @TransactionalMethod(automaticReadTracking = false)
        void disabled() {
        }
    }

    @Test
    public void whenInterrupted() {
        ClassMetadata classMetadata = repository.getClassMetadata(Interrupted.class);
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

        @TransactionalMethod(automaticReadTracking = false)
        void disabled() {
        }
    }


    @Test
    public void whenAllowWriteSkewProblem() {
        ClassMetadata classMetadata = repository.getClassMetadata(AllowWriteSkewProblem.class);
        MethodMetadata enabledMethodMetadata = classMetadata.getMethodMetadata("enabled", "()V");
        TransactionMetadata enabledTransactionMetadata = enabledMethodMetadata.getTransactionalMetadata();

        assertNotNull(enabledTransactionMetadata);
        assertTrue(enabledTransactionMetadata.allowWriteSkewProblem);

        MethodMetadata disabledMethodMetadata = classMetadata.getMethodMetadata("disabled", "()V");

        TransactionMetadata disabledTransactionMetadata = disabledMethodMetadata.getTransactionalMetadata();
        assertNotNull(disabledTransactionMetadata);
        assertFalse(disabledTransactionMetadata.allowWriteSkewProblem);
    }

    class AllowWriteSkewProblem {
        @TransactionalMethod(automaticReadTracking = true, allowWriteSkewProblem = true)
        void enabled() {
        }

        @TransactionalMethod(automaticReadTracking = true, allowWriteSkewProblem = false)
        void disabled() {
        }
    }

    @Test
    public void whenMaxRetryCount() {
        ClassMetadata classMetadata = repository.getClassMetadata(MaxRetryCountProblem.class);
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
