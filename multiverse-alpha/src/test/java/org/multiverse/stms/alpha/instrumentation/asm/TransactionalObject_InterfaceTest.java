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
        ClassMetadata classMetadata = repository.getClassMetadata(internalName);
        assertTrue(classMetadata.isTransactionalObject());
        assertFalse(classMetadata.isRealTransactionalObject());

        clazz.getMethod("someMethod");
    }

    @TransactionalObject
    interface TransactionalInterface {
        void someMethod();
    }

    @Test
    public void extendingInterface() {
        Class clazz = ExtendingInterface.class;

        String internalName = Type.getInternalName(clazz);
        ClassMetadata classMetadata = repository.getClassMetadata(internalName);

        assertTrue(classMetadata.isTransactionalObject());
        assertFalse(classMetadata.isRealTransactionalObject());
    }

    interface ExtendingInterface extends TransactionalInterface {
    }

    @Test
    public void objectImplementingTransactionalInterface() {
        ObjectImplementingTransactionalInterface o = new ObjectImplementingTransactionalInterface();
        assertTrue(o instanceof AlphaTransactionalObject);

        o.someMethod();

        assertEquals(1, o.getValue());
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
