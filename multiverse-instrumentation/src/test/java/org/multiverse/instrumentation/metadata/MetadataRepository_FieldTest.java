package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.Exclude;
import org.multiverse.annotations.TransactionalObject;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class MetadataRepository_FieldTest {
    private MetadataRepository repository;

    @Before
    public void setUp() {
        repository = new MetadataRepository();
    }

    @Test
    public void whenTransactionalObjectWithField() {
        ClassMetadata classMetadata = repository.getClassMetadata(TransactionalObjectWithField.class);
        assertTrue(classMetadata.isRealTransactionalObject());

        FieldMetadata fieldMetadata = classMetadata.getFieldMetadata("field");
        assertNotNull(fieldMetadata);
        assertTrue(fieldMetadata.isManagedField());
    }

    @TransactionalObject
    class TransactionalObjectWithField {
        int field;
    }

    @Test
    public void whenTransactionalObjectWithExcludedField() {
        ClassMetadata classMetadata = repository.getClassMetadata(TransactionalObjectWithExcludedField.class);
        assertFalse(classMetadata.isRealTransactionalObject());

        FieldMetadata fieldMetadata = classMetadata.getFieldMetadata("field");
        assertNotNull(fieldMetadata);
        assertFalse(fieldMetadata.isManagedField());
    }

    @TransactionalObject
    class TransactionalObjectWithExcludedField {
        @Exclude
        int field;
    }
}
