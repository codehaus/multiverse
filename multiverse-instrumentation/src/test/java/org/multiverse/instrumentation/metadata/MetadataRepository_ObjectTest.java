package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class MetadataRepository_ObjectTest {
    private MetadataRepository repository;

    @Before
    public void setUp() {
        repository = new MetadataRepository();
    }

    @Test
    public void whenNonTransactionalObject() {
        ClassMetadata metadata = repository.loadClassMetadata(Object.class);

        assertNotNull(metadata);
        assertTrue(metadata.isIgnoredClass());
        assertFalse(metadata.isTransactionalObject());
    }

    @Test
    public void whenObjectTransactional() {
        ClassMetadata metadata = repository.loadClassMetadata(Person.class);

        assertNotNull(metadata);
        assertFalse(metadata.isIgnoredClass());
        assertTrue(metadata.isTransactionalObject());
        assertTrue(metadata.isTransactionalObjectWithObjectGranularFields());

        assertTrue(metadata.hasTransactionalMethods());
    }

    @TransactionalObject
    class Person {
        int age;

        public int getAge() {
            return age;
        }

        public void setAge(int newAge) {
            this.age = newAge;
        }
    }

    @Test
    public void whenObjectImplementingTransactionalInterface() {
        ClassMetadata metadata = repository.loadClassMetadata(ObjectImplementingTransactionalInterface.class);

        assertNotNull(metadata);
        assertFalse(metadata.isInterface());
        assertFalse(metadata.isIgnoredClass());
        assertTrue(metadata.isTransactionalObject());
        assertFalse(metadata.isTransactionalObjectWithObjectGranularFields());
    }


    @TransactionalObject
    interface TransactionalInterface {
    }

    class ObjectImplementingTransactionalInterface implements TransactionalInterface {
    }

    @Test
    public void whenObjectExtendsTransactionalObject() {
        ClassMetadata metadata = repository.loadClassMetadata(ObjectExtendingTransactionalObject.class);

        assertNotNull(metadata);
        assertFalse(metadata.isInterface());
        assertFalse(metadata.isIgnoredClass());
        assertTrue(metadata.isTransactionalObject());
        assertFalse(metadata.isTransactionalObjectWithObjectGranularFields());
    }

    @TransactionalObject
    class TxObject {
    }

    class ObjectExtendingTransactionalObject extends TxObject {
    }
}
