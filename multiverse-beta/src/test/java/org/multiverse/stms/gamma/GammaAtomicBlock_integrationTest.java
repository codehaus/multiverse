package org.multiverse.stms.gamma;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicLongClosure;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.MonoGammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class GammaAtomicBlock_integrationTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }


    @Test
    public void whenRead() {
        final GammaLongRef ref = new GammaLongRef(stm, 10);

        AtomicBlock block = stm.newTransactionFactoryBuilder().buildAtomicBlock();
        long result = block.execute(new AtomicLongClosure() {
            @Override
            public long execute(Transaction tx) throws Exception {
                assertSame(tx, getThreadLocalTransaction());
                return ref.get(tx);
            }
        });

        assertNull(getThreadLocalTransaction());
        assertEquals(10, result);
    }

    @Test
    public void whenUpdate() {
        final GammaLongRef ref = new GammaLongRef(stm, 0);

        AtomicBlock block = stm.newTransactionFactoryBuilder().buildAtomicBlock();
        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                ref.incrementAndGet(tx, 1);
            }
        });

        assertEquals(1, ref.atomicGet());
    }

    @Test
    public void whenTooManyRetries() {
        final GammaLongRef ref = new GammaLongRef(stm);

        MonoGammaTransaction otherTx = new MonoGammaTransaction(stm);
        ref.openForWrite(otherTx, LOCKMODE_EXCLUSIVE);

        try {
            AtomicBlock block = stm.newTransactionFactoryBuilder()
                    .setMaxRetries(100)
                    .buildAtomicBlock();

            block.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    ref.get(tx);
                }
            });

            fail();
        } catch (TooManyRetriesException expected) {
        }
    }

    @Test
    public void whenMultipleUpdatesDoneInSingleTransaction() {
        final GammaLongRef ref = new GammaLongRef(stm);

        AtomicBlock block = stm.newTransactionFactoryBuilder()
                .setDirtyCheckEnabled(false)
                .buildAtomicBlock();
        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                for (int k = 0; k < 10; k++) {
                    long l = ref.get();
                    ref.set(l + 1);
                }


                int a = 10;

            }
        });

        assertEquals(10, ref.atomicGet());
    }
}
