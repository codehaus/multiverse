package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicLongClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;

public class BetaAtomicBlock_integrationTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenRead() {
        final LongRef ref = createLongRef(stm, 10);

        AtomicBlock block = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        long result = block.execute(new AtomicLongClosure() {
            @Override
            public long execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                return btx.openForRead(ref, false, pool).value;
            }
        });

        assertEquals(10, result);
    }

    @Test
    public void whenUpdate() {
        final LongRef ref = createLongRef(stm, 0);

        AtomicBlock block = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                btx.openForWrite(ref, false, pool).value++;
            }
        });

        assertEquals(1, ref.unsafeLoad().value);
    }

    @Test
    public void whenTooManyRetries() {
        final LongRef ref = BetaStmUtils.createLongRef(stm);

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForWrite(ref, true, pool);

        try {
            AtomicBlock block = stm.getTransactionFactoryBuilder()
                    .setMaxRetries(100)
                    .buildAtomicBlock();

            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    btx.openForRead(ref, false, pool);
                }
            });

            fail();
        } catch (TooManyRetriesException expected) {
        }
    }

    @Test
    public void whenMultipleUpdatesDoneInSingleTransaction() {
        final LongRef ref = BetaStmUtils.createLongRef(stm);

        AtomicBlock block = stm.getTransactionFactoryBuilder().buildAtomicBlock();
        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                BetaObjectPool pool = getThreadLocalBetaObjectPool();
                for (int k = 0; k < 10; k++) {
                    LongRefTranlocal tranlocal = btx.openForWrite(ref, false, pool);
                    tranlocal.value++;
                }
            }
        });

        assertEquals(10, ref.active.value);
    }
}
