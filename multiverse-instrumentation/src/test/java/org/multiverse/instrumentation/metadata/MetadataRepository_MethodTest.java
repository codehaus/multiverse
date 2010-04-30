package org.multiverse.instrumentation.metadata;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.NonTransactional;
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
        ClassMetadata classMetadata = repository.loadClassMetadata(ObjectWithTransactionalMethod.class);

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
        ClassMetadata classMetadata = repository.loadClassMetadata(TransactionalObjectWithMethod.class);

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
        ClassMetadata classMetadata = repository.loadClassMetadata(TransactionalInterfaceWithMethod.class);

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


    @Test
    public void whenTransactionalObjectWithExcludedMethod() {
        ClassMetadata classMetadata = repository.loadClassMetadata(TransactionalObjectWithExcludedMethod.class);

        MethodMetadata methodMetadata = classMetadata.getMethodMetadata("foo", "()V");
        assertNotNull(methodMetadata);
        assertFalse(methodMetadata.isTransactional());
        assertFalse(methodMetadata.isAbstract());
        assertFalse(methodMetadata.isConstructor());
    }

    @TransactionalObject
    class TransactionalObjectWithExcludedMethod {
        @NonTransactional
        public void foo() {
        }
    }
}
