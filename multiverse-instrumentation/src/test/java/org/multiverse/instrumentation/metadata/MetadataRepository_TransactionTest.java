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
        assertFalse(transactionMetadata.readOnly);
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.allowWriteSkewProblem);
        assertTrue(transactionMetadata.automaticReadTracking);
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
        assertFalse(transactionMetadata.interruptible);
        assertTrue(transactionMetadata.allowWriteSkewProblem);
        assertFalse(transactionMetadata.automaticReadTracking);
        assertEquals(1000, transactionMetadata.maxRetryCount);
        assertEquals("org.multiverse.instrumentation.metadata.MetadataRepository_TransactionTest$ReadonlyMethod.method()", transactionMetadata.familyName);
    }

    class ReadonlyMethod {
        @TransactionalMethod(readonly = true)
        void method() {
        }
    }
}
