package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.instrumentation.metadata.MetadataRepository;
import org.multiverse.javaagent.JavaAgentProblemMonitor;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import static org.junit.Assert.*;
import static org.multiverse.instrumentation.InstrumentationTestUtils.resetInstrumentationProblemMonitor;

public class TransactionalObject_InterfaceTest {

    private MetadataRepository repository;

    @Before
    public void setUp() {
        resetInstrumentationProblemMonitor();
        repository = null;//MetadataRepository.INSTANCE;
    }

    @After
    public void tearDown() {
        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
        resetInstrumentationProblemMonitor();
    }

    @Test
    public void transactionalInterface() throws NoSuchMethodException {
        Class clazz = TransactionalInterface.class;
        clazz.getMethod("someMethod");
    }

    @TransactionalObject
    interface TransactionalInterface {
        void someMethod();
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
