package org.multiverse.integrationtests.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.integrationtests.Ref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class ReadonlyScopeTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void readonlySurroundingUpdate() {
        Ref ref = new Ref();

        try {
            readonlySurroundingUpdate(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertEquals(0, ref.get());
    }

    @TransactionalMethod(readonly = true)
    public void readonlySurroundingUpdate(Ref ref) {
        innerUpdate(ref);
    }

    public void innerUpdate(Ref ref) {
        ref.inc();
    }

    @Test
    public void updateSurroundingReadonly() {
        Ref ref = new Ref();
        outerUpdateInnerReadonly(ref);
        assertEquals(1, ref.get());
    }

    @TransactionalMethod(readonly = false)
    public void outerUpdateInnerReadonly(Ref ref) {
        innerReadonly(ref);
    }

    @TransactionalMethod(readonly = false)
    private void innerReadonly(Ref ref) {
        ref.inc();
    }
}
