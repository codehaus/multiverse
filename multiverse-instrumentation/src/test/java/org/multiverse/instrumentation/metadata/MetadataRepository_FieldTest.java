package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.FieldGranularity;
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
    public void nonTransactionalObject() {
        ClassMetadata classMetadata = repository.loadClassMetadata(NonTransactional.class);
        assertFalse(classMetadata.isTransactionalObject());
        assertFalse(classMetadata.hasManagedFields());
        assertFalse(classMetadata.hasManagedFieldsWithFieldGranularity());
        assertFalse(classMetadata.hasManagedFieldsWithObjectGranularity());
    }

    class NonTransactional {
        int field;
    }

    @Test
    public void whenTransactionalObjectWithField() {
        ClassMetadata classMetadata = repository.loadClassMetadata(TransactionalObjectWithField.class);
        assertTrue(classMetadata.isTransactionalObject());
        assertTrue(classMetadata.isTransactionalObjectWithObjectGranularFields());
        assertTrue(classMetadata.hasManagedFields());
        assertFalse(classMetadata.hasManagedFieldsWithFieldGranularity());
        assertTrue(classMetadata.hasManagedFieldsWithObjectGranularity());

        FieldMetadata fieldMetadata = classMetadata.getFieldMetadata("field");
        assertNotNull(fieldMetadata);
        assertEquals("field", fieldMetadata.getName());
        assertTrue(fieldMetadata.isManagedField());
    }

    @TransactionalObject
    class TransactionalObjectWithField {
        int field;
    }

    @Test
    public void whenTransactionalObjectWithExcludedField() {
        ClassMetadata classMetadata = repository.loadClassMetadata(TransactionalObjectWithExcludedField.class);
        assertTrue(classMetadata.isTransactionalObject());
        assertFalse(classMetadata.isTransactionalObjectWithObjectGranularFields());
        assertFalse(classMetadata.hasManagedFieldsWithFieldGranularity());
        assertFalse(classMetadata.hasManagedFieldsWithObjectGranularity());

        FieldMetadata fieldMetadata = classMetadata.getFieldMetadata("field");
        assertNotNull(fieldMetadata);
        assertFalse(fieldMetadata.isManagedField());
    }

    @TransactionalObject
    class TransactionalObjectWithExcludedField {
        @org.multiverse.annotations.NonTransactional
        int field;
    }

    @Test
    public void whenTransactionalObjectWithFinalField() {
        ClassMetadata classMetadata = repository.loadClassMetadata(TransactionalObjectWithFinalField.class);
        assertTrue(classMetadata.isTransactionalObject());
        assertFalse(classMetadata.hasManagedFieldsWithObjectGranularity());
        assertFalse(classMetadata.hasManagedFieldsWithFieldGranularity());
        assertFalse(classMetadata.hasManagedFields());
        assertFalse(classMetadata.isTransactionalObjectWithObjectGranularFields());

        FieldMetadata fieldMetadata = classMetadata.getFieldMetadata("field");
        assertNotNull(fieldMetadata);
        assertFalse(fieldMetadata.isManagedField());
    }

    @TransactionalObject
    class TransactionalObjectWithFinalField {
        final int field = 10;
    }

    @Test
    public void whenTransactionalObjectWithVolatileField() {
        ClassMetadata classMetadata = repository.loadClassMetadata(TransactionalObjectWithVolatileField.class);
        assertTrue(classMetadata.isTransactionalObject());
        assertFalse(classMetadata.isTransactionalObjectWithObjectGranularFields());
        assertFalse(classMetadata.hasManagedFieldsWithObjectGranularity());
        assertFalse(classMetadata.hasManagedFieldsWithFieldGranularity());

        FieldMetadata fieldMetadata = classMetadata.getFieldMetadata("field");
        assertNotNull(fieldMetadata);
        assertFalse(fieldMetadata.isManagedField());
        assertFalse(fieldMetadata.hasFieldGranularity());

    }

    @TransactionalObject
    class TransactionalObjectWithVolatileField {
        volatile int field;
    }


    @Test
    public void whenTransactionalObjectWithFieldGranularityField() {
        ClassMetadata classMetadata = repository.loadClassMetadata(TransactionalObjectWithFieldGranularityField.class);

        assertTrue(classMetadata.isTransactionalObject());
        assertFalse(classMetadata.isTransactionalObjectWithObjectGranularFields());
        assertFalse(classMetadata.hasManagedFieldsWithObjectGranularity());
        assertTrue(classMetadata.hasManagedFieldsWithFieldGranularity());

        FieldMetadata fieldMetadata = classMetadata.getFieldMetadata("field");
        assertNotNull(fieldMetadata);
        assertTrue(fieldMetadata.isManagedField());
        assertTrue(fieldMetadata.hasFieldGranularity());
    }


    @TransactionalObject
    class TransactionalObjectWithFieldGranularityField {

        @FieldGranularity
        private int field;
    }

    @Test
    public void whenTransactionalObjectMixedGranularityFields() {
        ClassMetadata classMetadata = repository.loadClassMetadata(TransactionalObjectMixedGranularityFields.class);

        assertTrue(classMetadata.isTransactionalObject());
        assertTrue(classMetadata.isTransactionalObjectWithObjectGranularFields());
        assertTrue(classMetadata.hasManagedFieldsWithObjectGranularity());
        assertTrue(classMetadata.hasManagedFieldsWithFieldGranularity());

        FieldMetadata fieldGranular = classMetadata.getFieldMetadata("fieldGranular");
        assertNotNull(fieldGranular);
        assertTrue(fieldGranular.isManagedField());
        assertTrue(fieldGranular.hasFieldGranularity());

        FieldMetadata objectGranular = classMetadata.getFieldMetadata("objectGranular");
        assertNotNull(objectGranular);
        assertTrue(objectGranular.isManagedField());
        assertFalse(objectGranular.hasFieldGranularity());
    }


    @TransactionalObject
    class TransactionalObjectMixedGranularityFields {

        @FieldGranularity
        private int fieldGranular;

        private int objectGranular;
    }
}
