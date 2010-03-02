package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaTransactionalObject;
import org.multiverse.stms.alpha.instrumentation.metadata.ClassMetadata;
import org.multiverse.stms.alpha.instrumentation.metadata.MetadataRepository;
import org.multiverse.utils.instrumentation.InstrumentationProblemMonitor;
import org.objectweb.asm.Type;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.resetInstrumentationProblemMonitor;

public class TransactionalObject_InterfaceTest {

    private MetadataRepository repository;

    @Before
    public void setUp() {
        resetInstrumentationProblemMonitor();
        repository = MetadataRepository.INSTANCE;
    }

    @After
    public void tearDown() {
        assertFalse(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
        resetInstrumentationProblemMonitor();
    }

    @Test
    public void transactionalInterface() throws NoSuchMethodException {
        Class clazz = TransactionalInterface.class;

        String internalName = Type.getInternalName(clazz);
        ClassMetadata classMetadata = repository.getClassMetadata(ClassLoader.getSystemClassLoader(), internalName);

        assertFalse(classMetadata.isIgnoredClass());
        assertTrue(classMetadata.isTransactionalObject());
        assertFalse(classMetadata.isRealTransactionalObject());
        assertTrue(classMetadata.getInterfaces().isEmpty());
        assertTrue(classMetadata.isInterface());

        clazz.getMethod("someMethod");
    }

    @TransactionalObject
    interface TransactionalInterface {
        void someMethod();
    }

    @Test
    public void extendingInterface() {
        ClassMetadata classMetadata = repository.getClassMetadata(ExtendingInterface.class);
        ClassMetadata interfaceMetadata = repository.getClassMetadata(TransactionalInterface.class);

        assertTrue(classMetadata.isInterface());
        assertFalse(classMetadata.isIgnoredClass());
        assertTrue(classMetadata.isTransactionalObject());
        assertFalse(classMetadata.isRealTransactionalObject());
        assertEquals(1, classMetadata.getInterfaces().size());
        assertEquals(classMetadata.getInterfaces().get(0), interfaceMetadata);
    }

    interface ExtendingInterface extends TransactionalInterface {
    }

    @Test
    public void objectImplementingTransactionalInterface() {
        ObjectImplementingTransactionalInterface o = new ObjectImplementingTransactionalInterface();
        assertTrue(o instanceof AlphaTransactionalObject);

        o.someMethod();

        assertEquals(1, o.getValue());

        ClassMetadata interfaceMetadata = repository.getClassMetadata(TransactionalInterface.class);
        ClassMetadata classMetadata = repository.getClassMetadata(ObjectImplementingTransactionalInterface.class);

        assertFalse(classMetadata.isInterface());
        assertFalse(classMetadata.isIgnoredClass());
        assertTrue(classMetadata.isTransactionalObject());
        assertTrue(classMetadata.isRealTransactionalObject());
        assertEquals(1, classMetadata.getInterfaces().size());
        assertEquals(classMetadata.getInterfaces().get(0), interfaceMetadata);
    }


    public class ObjectImplementingTransactionalInterface implements TransactionalInterface {
        private int value;

        @Override
        public void someMethod() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void objectImplementingExtendingInterface() {
        ObjectImplementingExtendingInterface o = new ObjectImplementingExtendingInterface();
        assertTrue(o instanceof AlphaTransactionalObject);

        o.someMethod();

        assertEquals(1, o.getValue());

        ClassMetadata interfaceMetadata = repository.getClassMetadata(ExtendingInterface.class);
        ClassMetadata classMetadata = repository.getClassMetadata(ObjectImplementingExtendingInterface.class);

        assertFalse(classMetadata.isInterface());
        assertFalse(classMetadata.isIgnoredClass());
        assertTrue(classMetadata.isTransactionalObject());
        assertTrue(classMetadata.isRealTransactionalObject());
        assertEquals(1, classMetadata.getInterfaces().size());
        assertEquals(classMetadata.getInterfaces().get(0), interfaceMetadata);
    }

    public class ObjectImplementingExtendingInterface implements ExtendingInterface {
        private int value;

        @Override
        public void someMethod() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }
}
