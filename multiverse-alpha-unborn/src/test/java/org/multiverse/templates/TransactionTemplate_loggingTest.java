package org.multiverse.templates;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LogLevel;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.programmatic.ProgrammaticReference;

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
        whenLogging(LogLevel.course);
    }

    @Test
    public void whenFineLogging() {
        whenLogging(LogLevel.fine);
    }


    @Test
    public void whenNoLogging() {
        whenLogging(LogLevel.none);
    }


    public void whenLogging(LogLevel level) {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setLogLevel(level)
                .setSpeculativeConfigurationEnabled(false)
                .setFamilyName("loggingtransaction")
                .build();

        final ProgrammaticReference ref = stm.getProgrammaticReferenceFactoryBuilder()
                .build()
                .atomicCreateReference();

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
