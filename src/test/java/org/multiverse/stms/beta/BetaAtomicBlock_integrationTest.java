package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicLongClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class BetaAtomicBlock_integrationTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    
    @Test
    public void whenRead() {
        final BetaLongRef ref = createLongRef(stm, 10);

        AtomicBlock block = stm.createTransactionFactoryBuilder().buildAtomicBlock();
        long result = block.execute(new AtomicLongClosure() {
            @Override
            public long execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                return btx.openForRead(ref, false).value;
            }
        });

        assertEquals(10, result);
    }

    @Test
    public void whenUpdate() {
        final BetaLongRef ref = createLongRef(stm, 0);

        AtomicBlock block = stm.createTransactionFactoryBuilder().buildAtomicBlock();
        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                btx.openForWrite(ref, false).value++;
            }
        });

        assertEquals(1, ref.___unsafeLoad().value);
    }

    @Test
    public void whenTooManyRetries() {
        final BetaLongRef ref = BetaStmUtils.createLongRef(stm);

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForWrite(ref, true);

        try {
            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .setMaxRetries(100)
                    .buildAtomicBlock();

            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    btx.openForRead(ref, false);
                }
            });

            fail();
        } catch (TooManyRetriesException expected) {
        }
    }

    @Test
    public void whenMultipleUpdatesDoneInSingleTransaction() {
        final BetaLongRef ref = BetaStmUtils.createLongRef(stm);

        AtomicBlock block = stm.createTransactionFactoryBuilder().buildAtomicBlock();
        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                BetaTransaction btx = (BetaTransaction) tx;
                for (int k = 0; k < 10; k++) {
                    LongRefTranlocal tranlocal = btx.openForWrite(ref, false);
                    tranlocal.value++;
                }
            }
        });

        assertEquals(10, ref.___unsafeLoad().value);
    }
}
