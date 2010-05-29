package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.templates.TransactionTemplate;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticRef_atomicGetTest {
    private AlphaProgrammaticRefFactory refFactory;
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = AlphaStm.createFast();
        refFactory = (AlphaProgrammaticRefFactory) stm.getProgrammaticRefFactoryBuilder().build();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNoTransactionRunningAndNullValue() {
        AlphaProgrammaticRef ref = refFactory.atomicCreateRef();

        long version = stm.getVersion();

        assertNull(ref.atomicGet());

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenNoTransactionRunningAndNonNullValue() {
        String value = "foo";
        AlphaProgrammaticRef ref = refFactory.atomicCreateRef(value);

        long version = stm.getVersion();

        assertEquals(value, ref.atomicGet());
        assertEquals(version, stm.getVersion());
    }

    @Test
    @Ignore
    public void whenTransactionUpAndRunningThenIgnored() {
        final String oldValue = "oldvalue";
        final AlphaProgrammaticRef ref = refFactory.atomicCreateRef(oldValue);

        long version = stm.getVersion();

        try {
            new TransactionTemplate(stm.getTransactionFactoryBuilder()
                    .setSpeculativeConfigurationEnabled(false)
                    .setReadonly(false)
                    .build()) {
                @Override
                public Object execute(Transaction tx) throws Exception {
                    System.out.println("1");
                    ref.set("newvalue");
                    System.out.println("2");
                    assertSame(oldValue, ref.atomicGet());
                    System.out.println("3");
                    tx.abort();
                    return null;
                }
            }.execute();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(version, stm.getVersion());
    }
}
