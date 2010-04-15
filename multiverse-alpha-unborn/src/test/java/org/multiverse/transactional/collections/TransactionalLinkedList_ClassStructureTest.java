package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.instrumentation.metadata.FieldMetadata;
import org.multiverse.instrumentation.metadata.MetadataRepository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.hasField;
import static org.multiverse.TestUtils.hasMethod;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_ClassStructureTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        Class clazz = TransactionalLinkedList.class;

        assertTrue(hasField(clazz, "maxCapacity"));
        assertTrue(hasField(clazz, "size"));
        assertTrue(hasField(clazz, "head"));
        assertTrue(hasField(clazz, "tail"));

        assertTrue(hasMethod(clazz, "remainingCapacity"));
        //assertTrue(hasMethod(clazz, "remainingCapacity", AlphaTransaction.class));
    }

    @Test
    @Ignore
    public void testMetadata() {
        MetadataRepository repo = new MetadataRepository();
        ClassMetadata metadata = repo.loadClassMetadata(TransactionalLinkedList.class);

        assertTrue(metadata.isTransactionalObject());
        assertFalse(metadata.isRealTransactionalObject());

        FieldMetadata sizeMetadata = metadata.getFieldMetadata("size");
        assertFalse(sizeMetadata.isManagedField());
        assertFalse(sizeMetadata.hasFieldGranularity());

        FieldMetadata headMetadata = metadata.getFieldMetadata("head");
        assertFalse(headMetadata.isManagedField());
        assertTrue(headMetadata.hasFieldGranularity());

        FieldMetadata tailMetadata = metadata.getFieldMetadata("tail");
        assertFalse(tailMetadata.isManagedField());
        assertTrue(tailMetadata.hasFieldGranularity());
    }

}
