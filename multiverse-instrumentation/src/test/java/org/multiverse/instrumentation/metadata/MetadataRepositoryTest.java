package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class MetadataRepositoryTest {
    private MetadataRepository repository;

    @Before
    public void setUp() {
        repository = new MetadataRepository();
    }

    @Test
    public void whenObject() {
        ClassMetadata metadata = repository.getClassMetadata(Object.class);
        assertNotNull(metadata);
        assertTrue(metadata.isIgnoredClass());
    }

    @Test
    public void whenObjectTransactional() {
        ClassMetadata metadata = repository.getClassMetadata(Person.class);

        assertNotNull(metadata);
        assertFalse(metadata.isIgnoredClass());
        assertTrue(metadata.isTransactionalObject());
        assertTrue(metadata.isRealTransactionalObject());
    }

    @TransactionalObject
    class Person {
        int age;
    }
}
