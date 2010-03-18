package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class MetadataRepository_MethodTest {
    private MetadataRepository repository;

    @Before
    public void setUp() {
        repository = new MetadataRepository();
    }

    @Test
    public void whenObjectWithTransactionalMethod() {
        ClassMetadata classMetadata = repository.getClassMetadata(ObjectWithTransactionalMethod.class);

        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("foo", "()V");
        assertNotNull(methodMetadata);
        assertTrue(methodMetadata.isTransactional());
        assertFalse(methodMetadata.isAbstract());
        assertFalse(methodMetadata.isConstructor());
    }

    class ObjectWithTransactionalMethod {

        @TransactionalMethod
        public void foo() {
        }
    }

    @Test
    public void whenTransactionalObjectWithMethod() {
        ClassMetadata classMetadata = repository.getClassMetadata(TransactionalObjectWithMethod.class);

        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("foo", "()V");
        assertNotNull(methodMetadata);
        assertTrue(methodMetadata.isTransactional());
        assertFalse(methodMetadata.isAbstract());
        assertFalse(methodMetadata.isConstructor());
    }

    @TransactionalObject
    class TransactionalObjectWithMethod {
        public void foo() {
        }
    }

    @Test
    public void whenTransactionalInterfaceWithMethod() {
        ClassMetadata classMetadata = repository.getClassMetadata(TransactionalInterfaceWithMethod.class);

        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("foo", "()V");
        assertNotNull(methodMetadata);
        assertTrue(methodMetadata.isTransactional());
        assertTrue(methodMetadata.isAbstract());
        assertFalse(methodMetadata.isConstructor());
    }

    @TransactionalObject
    interface TransactionalInterfaceWithMethod {
        public void foo();
    }
}
