package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.instrumentation.metadata.ClassMetadata;
import org.multiverse.stms.alpha.instrumentation.metadata.MetadataRepository;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.assertNoInstrumentationProblems;
import static org.multiverse.TestUtils.hasMethod;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class TransactionalMethod_InterfaceTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        TestUtils.resetInstrumentationProblemMonitor();
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @After
    public void tearDown() {
        assertNoInstrumentationProblems();
    }

    @Test
    public void interfaceWithTransactionalMethod() {
        Class clazz = InterfaceWithTransactionalMethod.class;

        MetadataRepository repo = MetadataRepository.INSTANCE;
        ClassMetadata classMetadata = repo.getClassMetadata(clazz);

        assertFalse(classMetadata.isIgnoredClass());
        assertTrue(classMetadata.isInterface());

        assertTrue(hasMethod(clazz, "transactional"));
        assertTrue(hasMethod(clazz, "nontransactional"));

        assertTrue(hasMethod(clazz, "transactional", AlphaTransaction.class));
        assertFalse(hasMethod(clazz, "nontransactional", AlphaTransaction.class));
    }

    interface InterfaceWithTransactionalMethod {
        @TransactionalMethod
        public void transactional();

        public void nontransactional();
    }

    @Test
    public void transactionalInterface() {
        Class clazz = TransactionalInterface.class;

        MetadataRepository repo = MetadataRepository.INSTANCE;
        ClassMetadata classMetadata = repo.getClassMetadata(clazz);

        assertFalse(classMetadata.isIgnoredClass());
        assertTrue(classMetadata.isInterface());

        assertTrue(hasMethod(clazz, "explitTransactional"));
        assertTrue(hasMethod(clazz, "implicitTransactional"));

        assertTrue(hasMethod(clazz, "explitTransactional", AlphaTransaction.class));
        assertTrue(hasMethod(clazz, "implicitTransactional", AlphaTransaction.class));
    }

    @TransactionalObject
    interface TransactionalInterface {
        @TransactionalMethod
        public void explitTransactional();

        public void implicitTransactional();
    }

    @Test
    public void InterfaceExtendingTransactionalInterface() {
        Class clazz = TransactionalInterface.class;

        MetadataRepository repo = MetadataRepository.INSTANCE;
        ClassMetadata classMetadata = repo.getClassMetadata(clazz);

        assertFalse(classMetadata.isIgnoredClass());
        assertTrue(classMetadata.isInterface());

        assertTrue(hasMethod(clazz, "explitTransactional"));
        assertTrue(hasMethod(clazz, "implicitTransactional"));

        assertTrue(hasMethod(clazz, "explitTransactional", AlphaTransaction.class));
        assertTrue(hasMethod(clazz, "implicitTransactional", AlphaTransaction.class));

    }

    @TransactionalObject
    interface InterfaceExtendingTransactionalInterface extends TransactionalInterface {
    }

    @Test
    @Ignore
    public void interfaceOverridingTransactionalMethod() {
    }


}
