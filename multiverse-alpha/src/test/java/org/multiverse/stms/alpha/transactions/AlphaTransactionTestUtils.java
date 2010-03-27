package org.multiverse.stms.alpha.transactions;

import org.multiverse.api.Latch;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.utils.Listeners;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class AlphaTransactionTestUtils {

    public static void assertHasNoListeners(ManualRef ref) {
        assertNull(ref.___getListeners());
    }

    public static void assertHasListeners(ManualRef ref, Latch... expectedLatches) {
        List<Latch> found = new LinkedList<Latch>();
        Listeners listener = ref.___getListeners();
        while (listener != null) {
            found.add(listener.getListener());
            listener = listener.getNext();
        }

        assertEquals(Arrays.asList(expectedLatches), found);
    }

    public static void assertIsUpdatableClone(ManualRef ref, AlphaTranlocal committed, AlphaTranlocal found) {
        assertNotNull(found);
        assertSame(committed, found.getOrigin());
        assertSame(ref, found.getTransactionalObject());
        assertFalse(found.isCommitted());
    }

    public static void assertFreshTranlocal(ManualRef ref, AlphaTranlocal found) {
        assertNotNull(found);
        assertSame(ref, found.getTransactionalObject());
        assertNull(found.getOrigin());
        assertFalse(found.isCommitted());
    }

}
