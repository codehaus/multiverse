package org.multiverse.templates;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.TraceLevel;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.programmatic.ProgrammaticRef;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionTemplate_loggingTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenCourseLogging() {
        whenLogging(TraceLevel.course);
    }

    @Test
    public void whenFineLogging() {
        whenLogging(TraceLevel.fine);
    }


    @Test
    public void whenNoLogging() {
        whenLogging(TraceLevel.none);
    }


    public void whenLogging(TraceLevel level) {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setTraceLevel(level)
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .setFamilyName("loggingtransaction")
                .build();

        final ProgrammaticRef ref = stm.getProgrammaticRefFactoryBuilder()
                .build()
                .atomicCreateRef();

        new TransactionTemplate(txFactory) {
            @Override
            public Object execute(Transaction tx) throws Exception {
                ref.set("foo");
                return null;
            }
        }.execute();

        ref.set("banana");
    }
}
