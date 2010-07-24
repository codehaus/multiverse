package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;
import org.multiverse.stms.beta.transactions.MonoBetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class TransactionTemplateTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void test() {
        final LongRef ref = StmUtils.createLongRef(stm);

        TransactionTemplate t = new TransactionTemplate(stm) {
            @Override
            public void execute(BetaTransaction tx) {
                for (int k = 0; k < 10; k++) {
                    LongRefTranlocal tranlocal =  tx.openForWrite(ref, false, pool);
                    tranlocal.value++;
                }
            }
        };

        t.execute(pool);

        assertEquals(10, ((LongRefTranlocal) ref.active).value);
    }

    @Test
    public void whenTooManyRetries() {
        final LongRef ref = StmUtils.createLongRef(stm);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForWrite(ref, true, pool);

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setMaxRetries(100);

        try {
            new TransactionTemplate(stm, new MonoBetaTransaction(config)) {
                @Override
                public void execute(BetaTransaction tx) {
                    tx.openForRead(ref, false, pool);
                }
            }.execute(pool);
            fail();
        } catch (TooManyRetriesException expected) {
        }
    }
}
