package org.multiverse.transactional.collections;

import org.junit.Test;
import org.multiverse.instrumentation.metadata.ClassMetadata;
import org.multiverse.instrumentation.metadata.FieldMetadata;
import org.multiverse.instrumentation.metadata.MetadataRepository;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.hasField;
import static org.multiverse.TestUtils.hasMethod;

/**
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_ClassStructureTest {

    @Test
    public void test() {
        Class clazz = TransactionalLinkedList.class;

        assertTrue(hasField(clazz, "maxCapacity"));
        assertTrue(hasField(clazz, "size"));
        assertTrue(hasField(clazz, "head"));
        assertTrue(hasField(clazz, "tail"));

        assertTrue(hasMethod(clazz, "remainingCapacity"));
        assertTrue(hasMethod(clazz, "remainingCapacity", AlphaTransaction.class));
    }

    @Test
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
