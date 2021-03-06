package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class MetadataRepository_InterfaceTest {
    private MetadataRepository repository;

    @Before
    public void setUp() {
        repository = new MetadataRepository();
    }

    @Test
    public void whenNonTransactionalInterface() {
        ClassMetadata metadata = repository.loadClassMetadata(NonTransactionalInterface.class);

        assertNotNull(metadata);
        assertTrue(metadata.isInterface());
        assertFalse(metadata.isIgnoredClass());
        assertFalse(metadata.isTransactionalObject());
        assertFalse(metadata.isTransactionalObjectWithObjectGranularFields());
        assertFalse(metadata.hasManagedFieldsWithFieldGranularity());
        assertFalse(metadata.hasManagedFields());
        assertFalse(metadata.hasTransactionalMethods());
    }

    interface NonTransactionalInterface {
    }

    @Test
    public void whenEmptyTransactionalInterface() {
        ClassMetadata metadata = repository.loadClassMetadata(EmptyTransactionalInterface.class);

        assertNotNull(metadata);
        assertTrue(metadata.isInterface());
        assertFalse(metadata.isIgnoredClass());
        assertTrue(metadata.isTransactionalObject());
        assertFalse(metadata.isTransactionalObjectWithObjectGranularFields());
        assertFalse(metadata.hasManagedFieldsWithFieldGranularity());
        assertFalse(metadata.hasManagedFields());
        assertFalse(metadata.hasTransactionalMethods());
    }

    @TransactionalObject
    interface EmptyTransactionalInterface {
    }

    @Test
    public void whenNonEmptyTransactionalInterface() {
        ClassMetadata metadata = repository.loadClassMetadata(NonEmptyTransactionalInterface.class);

        assertNotNull(metadata);
        assertTrue(metadata.isInterface());
        assertFalse(metadata.isIgnoredClass());
        assertTrue(metadata.isTransactionalObject());
        assertFalse(metadata.isTransactionalObjectWithObjectGranularFields());
        assertFalse(metadata.hasManagedFieldsWithFieldGranularity());
        assertFalse(metadata.hasManagedFields());
        assertTrue(metadata.hasTransactionalMethods());

        MethodMetadata methodMetadata = metadata.getMethodMetadata("someMethod", "()V");
        assertNotNull(methodMetadata);
        assertTrue(methodMetadata.isAbstract());
        assertTrue(methodMetadata.isTransactional());
    }

    @TransactionalObject
    interface NonEmptyTransactionalInterface {
        void someMethod();
    }

    @Test
    public void whenInterfaceExtendsTransactionalInterface() {
        ClassMetadata metadata = repository.loadClassMetadata(InterfaceExtendsTransactionalInterface.class);

        assertNotNull(metadata);
        assertTrue(metadata.isInterface());
        assertFalse(metadata.isIgnoredClass());
        assertTrue(metadata.isTransactionalObject());
        assertFalse(metadata.isTransactionalObjectWithObjectGranularFields());
        assertFalse(metadata.hasManagedFieldsWithFieldGranularity());
        assertFalse(metadata.hasManagedFields());
        assertFalse(metadata.hasTransactionalMethods());
    }

    interface InterfaceExtendsTransactionalInterface extends EmptyTransactionalInterface {

    }
}
