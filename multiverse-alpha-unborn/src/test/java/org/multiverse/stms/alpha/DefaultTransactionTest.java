package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class DefaultTransactionTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void testBlocking(){

    }

    @Test
    public void testConstruction() {
        TransactionFactory<AlphaTransaction> txFactory = stm.getTransactionFactoryBuilder().build();

        final AtomicReference<ManualRef> holder = new AtomicReference<ManualRef>();

        new TransactionTemplate(txFactory){
            @Override
            public Object execute(Transaction tx) throws Exception {
                final ManualRef ref = new ManualRef(stm, 20);
                holder.set(ref);
                return null;
            }
        }.execute();

        assertEquals(20, holder.get().get(stm));
    }

    @Test
    public void testUpdate() {
        final ManualRef ref = new ManualRef(stm);

        TransactionFactory<AlphaTransaction> txFactory = stm.getTransactionFactoryBuilder().build();

        new TransactionTemplate(txFactory){
            @Override
            public Object execute(Transaction tx) throws Exception {
                ref.set((AlphaTransaction)tx, 20);
                return null;
            }
        }.execute();

        assertEquals(20, ref.get(stm));
    }
}
