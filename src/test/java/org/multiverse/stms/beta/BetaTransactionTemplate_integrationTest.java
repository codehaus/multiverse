package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class BetaTransactionTemplate_integrationTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        final LongRef ref = createLongRef(stm, 0);

        new BetaTransactionTemplate(stm) {
            @Override
            public Object execute(BetaTransaction tx) throws Exception {
                tx.openForWrite(ref, false, pool).value++;
                return null;
            }
        }.execute(pool);

        assertEquals(1, ref.unsafeLoad().value);
    }

    @Test
    public void whenTooManyRetries() {
        final LongRef ref = BetaStmUtils.createLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, true, pool);

        try {                        
            new BetaTransactionTemplate(stm.getTransactionFactoryBuilder()
                    .setMaxRetries(100)
                    .build()) {
                @Override
                public Object execute(BetaTransaction tx) {
                    tx.openForRead(ref, false, pool);
                    return null;
                }
            }.execute(pool);
            fail();
        } catch (TooManyRetriesException expected) {
        }
    }

    @Test
    public void whenMultipleUpdatesDoneInSingleTransaction() {
        final LongRef ref = BetaStmUtils.createLongRef(stm);

        BetaTransactionTemplate t = new BetaTransactionTemplate(stm) {
            @Override
            public Object execute(BetaTransaction tx) {
                for (int k = 0; k < 10; k++) {
                    LongRefTranlocal tranlocal = tx.openForWrite(ref, false, pool);
                    tranlocal.value++;
                }
                return null;
            }
        };

        t.execute(pool);

        assertEquals(10, ref.active.value);
    }
}
